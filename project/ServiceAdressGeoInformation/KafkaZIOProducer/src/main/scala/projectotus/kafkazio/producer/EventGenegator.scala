package projectotus.kafkazio.producer

import projectotus.kafkazio.model.{MonadParser, TransactionRaw}
import zio.ZIO
import zio.stream.ZStream

import java.io.IOException
import java.nio.file.Paths

//@accessible
trait Service {
  def parserTransactionRaw(parseStr: String): ZIO[Any, Throwable, Iterator[TransactionRaw]]
}

class EventGenerator {

  val workingDir: String = Paths.get(".").toAbsolutePath.toString.replace(".", "")

  private def readFile(file: String): ZIO[String, IOException, String] = {
    lazy val fileReader = ZIO.fromAutoCloseable(
      ZIO.attemptBlockingIO(scala.io.Source.fromFile(s"$workingDir$file")))
    lazy val read = ZIO.scoped {
      fileReader.map(_.getLines().toList.mkString("\n"))
    }
    read
   }

  def logFailures[R, E, A](zio: ZIO[R, E, A]): ZIO[R, E, A] =
    zio.foldCauseZIO(
      cause => {
        cause.prettyPrint
        zio
      },
      _ => zio
    )

  def readFileZio(file: String): ZIO[String, Nothing, String] =
    logFailures {
      readFile(file).foldZIO(
        error => ZIO.succeed(s"Ошибка - $error"),
        data => ZIO.succeed(data)
      )
    }

  def StringField: MonadParser[String, String] = MonadParser[String, String] {
    case str =>
      val idx = str.indexOf(";")
      if (idx > -1)
        (str.substring(0, idx), str.substring(idx+1))
      else
        (str, "")
  }

  def IntField: MonadParser[Int, String] = StringField.map(_.toInt)
  def DoubleField: MonadParser[Double, String] = StringField.map(_.toDouble)
  def LongField: MonadParser[Long, String] = StringField.map(_.toLong)
  def BooleanField: MonadParser[Boolean, String] = StringField.map(_.toBoolean)

  lazy val parser: MonadParser[TransactionRaw, String] = for {
    userId <- LongField
    country <- StringField
    amount <- DoubleField
  } yield TransactionRaw(userId, country, BigDecimal(amount))

  /*def parserTransactionRaw(parseStr: String) =
    ZIO.attempt(TransactionRaw(parseStr.split(";", -1)(0).toLong, parseStr.split(";", -1)(1).toString, BigDecimal(parseStr.split(";", -1)(2)))).logError("parse error").mapError(e => new Throwable(e.getMessage.toString))
  */
  def parserTransactionRaw(parseStr: Iterator[String]): ZIO[Any, Throwable, Iterator[TransactionRaw]] = {
    for {
      chunk <- ZStream.fromIterator(parseStr).mapZIO(parseTransaction).refineOrDie(e => e).runCollect
      iter <- ZIO.attempt(chunk.toIterator)
    } yield iter
  }

  def parseTransaction(parse: String) = for {
    userId <- parseTransactionRawLong(parse)
    country <- parseTransactionRawString(parse)
    amount <- parseTransactionRawBigDecimal(parse)
  } yield TransactionRaw(userId, country, amount)
  def parseTransactionRawLong(parseStr: String) = ZIO.attempt(parseStr.split(";", -1)(0).toLong).mapError(e => new Throwable(e.getMessage.toString))
  def parseTransactionRawString(parseStr: String) = ZIO.attempt(parseStr.split(";", -1)(1).toString).mapError(e => new Throwable(e.getMessage.toString))
  def parseTransactionRawBigDecimal(parseStr: String) = ZIO.attempt(BigDecimal(parseStr.split(";", -1)(2))).mapError(e => new Throwable(e.getMessage.toString))

  lazy val transactions = List(
    TransactionRaw("4783237878786353".toLong, "55.735926,37.616601", BigDecimal(12.99))
  )
}

object EventGenerator {
  def parserTransactionRaw(parseStr: Iterator[String]) =
    ZIO.serviceWithZIO[EventGenerator](_.parserTransactionRaw(parseStr = parseStr))

  val live: zio.ULayer[EventGenerator] = zio.ZLayer.succeed(new EventGenerator)

  val workingDir: String = Paths.get(".").toAbsolutePath.toString.replace(".", "")
}