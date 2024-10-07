package AddressGeoInformationAPI

import code.DataRepository.DataService
import code.model.ErrorMsg
import code.restService.{RestServer, RestServerLive}
import db.DataSource
import zio._
import zio.http.{Response, Root, _}
import zio.http.endpoint.openapi.OpenAPI._
import zio.macros._
import zio.http.Routes._
import zio.http.endpoint._
import zio.http.endpoint.openapi._
import zio.http.codec.{HttpCodec, PathCodec}
import zio.json.EncoderOps
import zio.metrics.connectors.prometheus.PrometheusPublisher

import java.io.StringWriter



object AddressGeoInformationAPI {
  val api = {
    Routes(
      Method.GET /"countryinformation"/string("name") -> handler { (name: String, req: Request) =>
        val response = for {
          inf <- RestServerLive.countryinformation(name)
          .logError(s"Error countryinformation/${name}")
          .mapError(error => ErrorMsg.internalError(error.getMessage))
          _ <- ZIO.fail(ErrorMsg.invalidBody("list is empty")).when(inf.isEmpty)
        } yield Response.json(inf.toJson.toString)
        response.catchAll(errorMsg => ZIO.succeed(Response.json(errorMsg.toJson)
          .status(Status.BadRequest)))
      },
     //Method.POST /"countryinformation"/string("name") ->
     Method.GET / "adressgeoinformation" -> handler {(req: Request) =>
        RestServerLive.adressgeoinformation.foldZIO (
          err => ZIO.logError(err.getMessage) *> zio.ZIO.succeed (Response.status (Status.BadRequest))
            @@ ZIOAspect.annotated("adressgeoinformation",req.toString),
          result => zio.ZIO.succeed (Response.json (result.toJson.toString)))
      },
    Method.GET / "hikaricp-metrics" -> handler {(req: Request) =>

      /*val writer = new StringWriter()
      io.prometheus.client.exporter.common.TextFormat
        .write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples())
      val buffer = writer.getBuffer.toString
      buffer.isEmpty match {
        case true => zio.ZIO.succeed(Response.status (Status.NotFound))
        case false => zio.ZIO.succeed(Response.json(buffer.toJson))
      }*/
      val response = for {
          writer <- ZIO.succeed(new StringWriter())
          _ <- ZIO.attempt(io.prometheus.client.exporter.common.TextFormat
            .write004(writer, io.prometheus.client.CollectorRegistry.defaultRegistry.metricFamilySamples()))
            .logError(s"Error hikaricp-metrics")
            .mapError(error => ErrorMsg.internalError(error.getMessage))
        buffer <- ZIO.succeed(writer.getBuffer.toString)
          _ <- ZIO.fail(ErrorMsg.invalidBody("buffer is empty")).when(buffer.isEmpty)
      } yield Response.json(buffer.toJson.toString)
      response.catchAll(errorMsg => ZIO.succeed(Response.json(errorMsg.toJson)
        .status(Status.BadRequest)))
    },
    Method.GET / "metrics" -> handler {(req: Request) =>
        zio.ZIO.serviceWithZIO[PrometheusPublisher](_.get.map(Response.text))
    },

      Method.GET / "health" -> Handler.ok)

  }

  /*val api = Http.collectZIO[Request]{
    case Method.GET -> !! / phone =>
      PhoneBookService.find(phone).foldM(
        err => ZIO.succeed(Response.status(Status.NotFound)),
        result => ZIO.succeed(Response.json(result.asJson.toString()))
      )
    case req @ Method.POST -> !!  =>
      (for{
        r <- req.body
        dto <- ZIO.fromEither(PhoneRecordDTO.decoder.decodeJson(r.asJson))
        result <- PhoneBookService.insert(dto)
      } yield result).foldM(
        err =>
          ZIO.effect(println(err.getMessage)) *>
            ZIO.succeed(Response.status(Status.BadRequest)),
        result => ZIO.succeed(Response.json(result))
      )
    case req @ Method.PUT -> !! / id / addressId => (for{
      r <- req.bodyAsString
      dto <- ZIO.fromEither(PhoneRecordDTO.decoder.decodeJson(r.asJson))
      _ <- PhoneBookService.update(id, addressId, dto)
    } yield ()).foldM(
      err => ZIO.succeed(Response.status(Status.BadRequest)),
      result => ZIO.succeed(Response.ok)
    )
    case Method.DELETE -> !! / id => PhoneBookService.delete(id).foldM(
      err => ZIO.succeed(Response.status(Status.BadRequest)),
      result => ZIO.succeed(Response.ok)
    )
  }*/
}
