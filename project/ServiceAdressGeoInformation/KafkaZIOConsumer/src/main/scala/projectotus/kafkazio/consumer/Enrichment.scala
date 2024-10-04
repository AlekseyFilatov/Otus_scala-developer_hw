package projectotus.kafkazio.consumer

import zio._
import zio.http._
import zio.json._
import projectotus.kafkazio.model._

trait Enrichment {
  def enrich(transactionRaw: TransactionRaw): IO[EnrichmentError, TransactionEnriched]
}

object Enrichment {
  def enrich(transactionRaw: TransactionRaw): ZIO[Enrichment, EnrichmentError, TransactionEnriched] =
    for {
      a <- ZIO.serviceWithZIO[Enrichment] (_.enrich(transactionRaw))
    } yield TransactionEnriched(a.userId, a.coordinate, a.amount)
}

final case class EnrichmentLive(countryCache: CountryCache, httpClient: Client) extends Enrichment { self =>

  override def enrich(
                       transactionRaw: TransactionRaw
                     ): IO[EnrichmentError, TransactionEnriched] = {
    val TransactionRaw(userId, coordinate, amount) = transactionRaw
    for {
      _       <- ZIO.logInfo("Enriching raw transaction.")
      country <- self.countryCache.get(coordinate).someOrElseZIO(self.fetchAndCacheCountryDetails(coordinate))
    } yield TransactionEnriched(userId, country, amount)
  }

  private def fetchAndCacheCountryDetails(
                                           coordinate: String
                                         ): IO[EnrichmentError, Country] =
    for {
      _       <- ZIO.logInfo(s"Cache miss. Fetching country details from external API.")
      country <- self.fetchCountryDetails(coordinate)
      _       <- self.countryCache.put(country)
    } yield country

  private def fetchCountryDetails(
                                   coordinate: String
                                 ): IO[EnrichmentError, Country] =
    for {
      host <- ZIO.config(AppConfig.config.map(_.enrichment.host)).orDie
      response <- (self.httpClient @@ ZClientAspect.requestLogging())
        .scheme(Scheme.HTTP)
        .host("localhost").port(8081)
        .path("/countryinformation/")
        .get(coordinate)
        .mapError(EnrichmentError.CountryApiUnreachable)
      responseBody <- response.body.asString.mapError(EnrichmentError.ResponseExtraction)
      _ <- ZIO
        .fail(EnrichmentError.UnexpectedResponse(response.status, responseBody))
        .when(response.status != Status.Ok)
      country <- ZIO
        .fromEither(responseBody.fromJson[NonEmptyChunk[Country]])
        .mapBoth(EnrichmentError.ResponseParsing, _.head)
    } yield country
}

object EnrichmentLive {
  lazy val layer: URLayer[CountryCache with Client, Enrichment] = ZLayer.fromFunction(EnrichmentLive(_, _))
}