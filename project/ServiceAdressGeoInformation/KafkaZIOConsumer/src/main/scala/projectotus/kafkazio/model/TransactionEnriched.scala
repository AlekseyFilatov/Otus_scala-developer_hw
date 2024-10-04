package projectotus.kafkazio.model

import zio.json._
import zio.kafka.serde._
import projectotus.kafkazio.protocol.deriveSerde

final case class TransactionEnriched(userId: Long, coordinate: Country, amount: BigDecimal)

object TransactionEnriched {
  implicit val codec: JsonCodec[TransactionEnriched] = DeriveJsonCodec.gen[TransactionEnriched]

  lazy val serde: Serde[Any, TransactionEnriched] = deriveSerde[TransactionEnriched]
}