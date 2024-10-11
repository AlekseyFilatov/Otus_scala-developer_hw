package code.restService

import code.DataRepository.DataService
import code._
import code.model.{Country, Person}
import db.DataSource
import io.getquill.{PostgresJdbcContext, SnakeCase}
import io.getquill.jdbczio.Quill
import zio.config.typesafe.FromConfigSourceTypesafe
import zio.http.endpoint.openapi.OpenAPI.SecurityScheme.Http
import zio.logging.backend.SLF4J
import zio._
import zio.macros.accessible
import zio.prelude.data.Optional.AllValuesAreNullable
import zio.http.Routes._
import zio.http.endpoint._
import zio.http.endpoint.openapi._
import zio.http.codec.{HttpCodec, PathCodec}

import java.sql.SQLException
import zio.http._

  @accessible
  trait RestServer {
    def adressgeoinformation: ZIO[DataSource, SQLException, List[Person]]
    def countryinformation(countryName: String): ZIO[DataSource, SQLException, List[Country]]
  }

  class RestServerLive(dataService: code.DataRepository.DataService) extends RestServer {

    override def adressgeoinformation: ZIO[DataSource, SQLException, List[Person]] =
      for {
        _ <- ZIO.logInfo("DataService user operation adressgeoinformation")
        res <- dataService.getPeople
      } yield res

    override def countryinformation(nameCountry: String): ZIO[DataSource, SQLException, List[Country]] =
      for {
        _ <- ZIO.logInfo("DataService user operation countryinformation")
        res <- dataService.getCountry(nameCountry)
      } yield res

  }


  object RestServerLive {
    def countryinformation(nameCountry: String) =
    ZIO.serviceWithZIO[RestServerLive](_.countryinformation(nameCountry = nameCountry))

    def adressgeoinformation =
      ZIO.serviceWithZIO[RestServerLive](_.adressgeoinformation)

   val live: ZLayer[DataService, Nothing, RestServerLive] = ZLayer {
      for {
            _ <- ZIO.logInfo("DataService operation")
            rest <- ZIO.service[DataService]
          } yield new RestServerLive(rest)
   }
  }
