package projectotus.kafkazio.model

import zio.json._
import zio.kafka.serde._
import projectotus.kafkazio.protocol.deriveSerde

final case class TransactionRaw(userId: Long, coordinate: String, amount: BigDecimal)

object TransactionRaw {
  implicit val codec: JsonCodec[TransactionRaw] = DeriveJsonCodec.gen[TransactionRaw]

  lazy val serde: Serde[Any, TransactionRaw] = deriveSerde[TransactionRaw]
}