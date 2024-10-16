package projectotus.kafkazio

import zio._
import zio.json._
import zio.kafka.serde._

package object protocol {
  def deriveSerde[A: JsonCodec]: Serde[Any, A] =
    Serde.string
      .inmapM(string => ZIO.fromEither(string.fromJson[A]).mapError(new IllegalArgumentException(_)))(a =>
        ZIO.succeed(a.toJson)
      )
}
