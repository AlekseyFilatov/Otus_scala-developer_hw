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

object ServiceAdrGeoInf extends ZIOAppDefault {

  object GreetingRoutes {
    def apply(): Routes[Any, Response] =
      Routes(
        // GET /greet?name=:name
        Method.GET / "greeter" -> handler { (req: Request) =>
          if (req.url.queryParams.nonEmpty)
            ZIO.succeed(
              Response.text(
                s"Hello ${req.url.queryParams("name").map(_.toString)}!"
              )
            )
          else
            ZIO.fail(Response.badRequest("The name query parameter is missing!"))
        },

        // GET /greet
        Method.GET / "greet" -> handler(Response.text(s"Hello World!")),

        // GET /greet/:name
        Method.GET / "greet" / string("name") -> handler {
          (name: String, _: Request) =>
            Response.text(s"Hello $name!")
        }
      )

  }

  def runLog(info: String, head: String): ZIO[Any, Any, Unit] =
    for {
      _ <- ZIO.logInfo(info) @@ zio.logging.loggerName(head)
      _ <- ZIO.logInfo("Confidential user operation") @@ SLF4J.logMarkerName("CONFIDENTIAL")
    } yield ()

  /*override val bootstrap =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> Runtime.removeDefaultLoggers >>> SLF4J.slf4j
*/

  override def run = {
    for {
      _ <- runLog("Starting operation", "zio.logging.kafka")
     /* _ <- ZIO.serviceWithZIO[RestServer](_.runServer)
        .provide(RestServerLive.layer)*/
      /*_ <- Server.serve(AddressGeoInformationAPI.AddressGeoInformationAPI()).provide(Server.defaultWithPort(8080)
        , DataService.live, RestServerLive.layer, Quill.Postgres.fromNamingStrategy(SnakeCase),
        Quill.DataSource.fromPrefix("gisDatabaseConfig"))*/
    } yield ()
    /*override def run ={
    for {
      _ <- runLog("Starting operation","zio.logging.kafka")
      //appConfig <- ZIO.config(AppConfig.config)
      _ <- DataService.getPeople
      .provide(
      DataService.live,
      Quill.Postgres.fromNamingStrategy(SnakeCase),
      Quill.DataSource.fromPrefix("gisDatabaseConfig"),
      )
    .debug("Results")
    .exitCode
    } yield ()
  }*/
    //override val run =
    // ZIO.serviceWithZIO[RestServer](_.runServer).provide(RestServerLive.layer)
    //override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] =  Server.serve(GreetingRoutes().toHttpApp).provide(Server.default)
  }

  object EndPointsServer extends EndPoints {
    private def handleGetAdressAndGeoInformationEndPoint(): ZIO[Any, Throwable, Any] = ???
  }

  trait EndPoints {

  }

  object EndPointDoc extends ZIOAppDefault with EndPoints {

    override def run: ZIO[Any with ZIOAppArgs with Scope, Any, Any] = ???
  }
}