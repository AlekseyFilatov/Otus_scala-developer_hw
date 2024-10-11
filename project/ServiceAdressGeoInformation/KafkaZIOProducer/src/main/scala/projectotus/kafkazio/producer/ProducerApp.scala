package projectotus.kafkazio.producer

import projectotus.kafkazio.model.{AppConfigProducer, TransactionRaw}
import zio._
import zio.config.typesafe._
import zio.kafka.producer._
import zio.kafka.serde._
import zio.logging.backend._
import zio.stream._

import java.nio.file.Paths

object ProducerApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Nothing, Unit] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val run =
    (for {
      topic <- ZIO.config(AppConfigProducer.config.map(_.topic))
      filecsv <- ZIO.config(AppConfigProducer.config.map(_.filecsv))
      workingDir <- ZIO.succeed(Paths.get(".").toAbsolutePath.toString.replace(".", ""))
      _ <- ZStream.fromIteratorScoped {
          ZIO.fromAutoCloseable(
              ZIO.attemptBlocking(
                scala.io.Source.fromFile(s"${workingDir}${filecsv}")
              )
            ).map(_.getLines)
            .flatMap(tr => ZIO.logInfo("Parse lines...") *>
              DataTransformation.EventGenerator.parserTransactionRaw(tr))
            .logError("parse error")
            .mapError(e => new Throwable(e.getMessage.toString))
        }
        .mapZIO { transaction =>
          (ZIO.logInfo("Producing transaction to Kafka...") *>
            Producer.produce(
              topic = topic,
              key = transaction.userId,
              value = transaction,
              keySerializer = Serde.long,
              valueSerializer = TransactionRaw.serde
            )) @@ ZIOAspect.annotated("userId", transaction.userId.toString)
        }
        .runDrain
        .catchAll {
          error => ZIO.logError(s"Got error while producing: $error") *> ZIO.succeed()
        }
    } yield ()).provide(
      producerSettingsProduce,
      Producer.live,
      DataTransformation.EventGenerator.live
    )

  private lazy val producerSettingsProduce =
    ZLayer {
      ZIO.config(AppConfigProducer.config.map(_.bootstrapServers)).map(ProducerSettings(_))
    }
}