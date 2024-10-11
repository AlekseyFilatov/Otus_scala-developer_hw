
//import code.DataRepository
import code.DataRepository.DataService
import code.restService.{RestServer, RestServerLive}
//import com.zaxxer.hikari.HikariDataSource
import io.getquill.context.ZioJdbc
import io.getquill.{CompositeNamingStrategy2, Escape, JdbcContextConfig, Literal, NamingStrategy, PostgresEscape, PostgresZioJdbcContext, SnakeCase, SqlMirrorContext}
import io.getquill.jdbczio.Quill
//import io.getquill.jdbczio.Quill._
//import io.getquill.util.LoadConfig
import zio.{ULayer, ZLayer, durationInt}
import zio.http._
import zio.http.endpoint._
import zio.http.endpoint.openapi._
import zio.http.codec.{HttpCodec, PathCodec}
import zio.metrics.connectors.{MetricsConfig, prometheus}
import zio.metrics.connectors.prometheus.{PrometheusPublisher, publisherLayer}

import scala.language.postfixOps
//import javax.sql.DataSource

object App {

  /*val dsLayer: ZLayer[Any, Throwable, javax.sql.DataSource] = Quill.DataSource.fromPrefix("gisDatabaseConfig")
  val quillLayer: ZLayer[javax.sql.DataSource, Nothing, Quill.Postgres[CompositeNamingStrategy2[SnakeCase, PostgresEscape]]] =
    Quill.Postgres.fromNamingStrategy(CompositeNamingStrategy2(SnakeCase,PostgresEscape))
  val contextLayer: ZLayer[Any, Throwable, Quill.Postgres[CompositeNamingStrategy2[SnakeCase, PostgresEscape]]] =
    dsLayer >>> quillLayer
  val dataServiceLayer = contextLayer >>> DataService.live*/

  val loggingMiddleware = new code.middleware.LoggingMiddleware()

  val responseTimeMiddleware = new code.middleware.ResponseTimeMiddleware()

  val middlewares = loggingMiddleware ++ responseTimeMiddleware

  private val metricsConfig = ZLayer.succeed(MetricsConfig(5.seconds))


  val appEnvironment
  = db.zioDS >+> DataService.live >+> RestServerLive.live  >+> Server.defaultWith(_.port(8081).enableRequestStreaming) ++
  metricsConfig >+> prometheus.publisherLayer >+> prometheus.prometheusLayer
  val httpApp = AddressGeoInformationAPI.AddressGeoInformationAPI.api

  val server = Server.serve(httpApp @@ middlewares).provideLayer(appEnvironment)
}

package object db {

  type DataSource = javax.sql.DataSource
  type SnakeCase = io.getquill.jdbczio.Quill.Postgres[io.getquill.SnakeCase]

  object Ctx extends PostgresZioJdbcContext(NamingStrategy(SnakeCase))

  val zioDS: ZLayer[Any, Throwable, DataSource] = Quill.DataSource.fromPrefix("gisDatabaseConfig")
}