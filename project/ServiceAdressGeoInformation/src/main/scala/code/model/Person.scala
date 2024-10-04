package code.model

import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}


case class Person(id: Int, name: String, age: Int)
object Person{
  implicit val codec: JsonCodec[Person] = DeriveJsonCodec.gen[Person]
  implicit val enc: JsonEncoder[Person] = DeriveJsonEncoder.gen[Person]
  implicit val dec: JsonDecoder[Person] = DeriveJsonDecoder.gen[Person]

}
