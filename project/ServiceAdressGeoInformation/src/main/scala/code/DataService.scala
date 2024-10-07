package code


import code.model.{Country,Person}
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}
import zio.macros.accessible
import zio.{ULayer, ZIO, ZLayer}

import java.sql.SQLException
import javax.sql.DataSource



object DataRepository {

  @accessible
  trait Sevice {
    def getPeople: ZIO[DataSource, SQLException, List[Person]]

    def getCountry(coordinate: String): ZIO[DataSource, SQLException, List[Country]]
  }


  class DataService extends Sevice {
    val ctx = db.Ctx

    import ctx._

    def getPeople: ZIO[DataSource, SQLException, List[Person]] =
      { for {
        _ <- ZIO.logInfo(s"getPeople user operation")
        res <- ctx.run(quote{query[Person]})
      } yield  res
      }

    def getCountry(coordinate: String): ZIO[DataSource, SQLException, List[Country]] =
    { for {
      _ <- ZIO.logInfo(s"country ${coordinate} user operation")
      res <- ctx.run(quote{query[Country].filter(c => c.coordinate == lift(coordinate))})
    } yield  res
    }
  }

  object DataService {

    def getPeople: ZIO[DataSource with DataService, SQLException, List[Person]] =
      ZIO.serviceWithZIO[DataService](_.getPeople)

    def getCountry(coordinate: String): ZIO[DataSource with DataService, SQLException, List[Country]] =
      ZIO.serviceWithZIO[DataService](_.getCountry(coordinate = coordinate))

    val live: ULayer[DataService] = ZLayer.succeed(new DataService)
  }
}