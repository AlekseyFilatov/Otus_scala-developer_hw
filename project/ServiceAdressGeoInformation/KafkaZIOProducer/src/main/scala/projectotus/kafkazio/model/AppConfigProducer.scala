package projectotus.kafkazio.model

import zio.config.magnolia.deriveConfig

final case class AppConfigProducer(bootstrapServers: List[String], topic: String, filecsv: String)

object AppConfigProducer {
  lazy val config = deriveConfig[AppConfigProducer]
}
