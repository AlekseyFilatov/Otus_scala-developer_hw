package projectotus.kafkazio.model

import zio.json._

final case class Country(coordinate: String, city: String, street: String, numb: String)

object Country {
  implicit val codec: JsonCodec[Country] = DeriveJsonCodec.gen[Country]
}