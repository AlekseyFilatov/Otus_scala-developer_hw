package projectotus.kafkazio.producer

import projectotus.kafkazio.model.{AppConfigProducer, TransactionRaw}
import zio._
import zio.config.typesafe._
import zio.kafka.producer._
import zio.kafka.serde._
import zio.logging.backend._
import zio.stream._

object ProducerApp extends ZIOAppDefault {
  override val bootstrap: ZLayer[ZIOAppArgs, Nothing, Unit] =
    Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  override val run =
    (for {
      topic <- ZIO.config(AppConfigProducer.config.map(_.topic))
      _ <- ZStream
        .fromIteratorScoped(
          {
            val it = ZIO.fromAutoCloseable(
              ZIO.attemptBlocking(
                scala.io.Source.fromFile(s"${EventGenerator.workingDir}transactionRaw.csv")
                )
            ).map(_.getLines)
            //it.map(x => x.map(x => EventGenerator.parser.parse(x)))
            /*for {
              tr <- it.mapAttempt(x => x.map(x => TransactionRaw(x.split(";", -1)(0).toLong, x.split(";", -1)(1).toString, BigDecimal(x.split(";", -1)(2))))).foldZIO(error => ZIO.die(new Throwable(s"Local :${error}")), result => ZIO.succeed(result))
          } yield tr*/
           for {
              iter <- it.mapAttempt(x => x.map(x => TransactionRaw(x.split(";", -1)(0).toLong, x.split(";", -1)(1).toString, BigDecimal(x.split(";", -1)(2)))))
                .logError("parse error")
                .mapError(e => new Throwable(e.getMessage.toString))
                .catchAll(e => ZIO.none)
            } yield iter.iterator

           /*for {
              transactionRaw <- it.flatMap(x => {
                EventGenerator.parserTransactionRaw(x)
              })
            } yield TransactionRaw(transactionRaw.userId, transactionRaw.country, transactionRaw.amount)*/
          }
        )
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
      EventGenerator.live
    )

  private lazy val producerSettingsProduce =
    ZLayer {
      ZIO.config(AppConfigProducer.config.map(_.bootstrapServers)).map(ProducerSettings(_))
    }
}