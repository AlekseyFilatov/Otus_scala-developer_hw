package catseffectHomework10
import cats.{ApplicativeError, Defer, Monad, MonadError, effect}
import cats.effect.kernel.{Ref, Spawn}
import cats.effect.{IO, IOApp}

import scala.concurrent.duration.DurationInt
import cats.effect._
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.{AuthedRequest, Method, Request, Uri}

import scala.util.Try
//import cats.effect.unsafe.IORuntime.global
import cats.effect.unsafe.implicits.global
import com.comcast.ip4s._
import org.http4s.{EntityDecoder, EntityEncoder, HttpRoutes}
import org.http4s.dsl.io._
import org.http4s.implicits._
import org.http4s.ember.server._
import org.http4s.server.Router
import org.typelevel.log4cats.LoggerFactory
import org.typelevel.log4cats.slf4j.Slf4jFactory
import cats.implicits._
import fs2.{Chunk, Pure}
import io.circe.Encoder
import io.circe.syntax._
import io.circe._
import io.circe.literal._
import org.http4s.circe.{jsonEncoder, jsonOf}
import io.circe.generic.auto._
import org.http4s.circe.CirceEntityCodec.circeEntityEncoder
import org.http4s.dsl.Http4sDsl
import fs2.Stream._
import fs2.concurrent.SignallingRef
import jdk.jpackage.internal.IOUtils

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt


object RestfullHomework {

    val createSwitch: IO[SignallingRef[IO, Boolean]] = SignallingRef[IO, Boolean](false)
    val createTotal: IO[SignallingRef[IO, BigInt]] = SignallingRef[IO, BigInt](0)
    type Sessions[F[_]] = Ref[F, Set[Int]]

    def program1(chunk: IO[Int], total: IO[Int], time: IO[Int]): fs2.Stream[IO, Byte] = {

        def program(switchTotal: SignallingRef[IO, BigInt], switch: SignallingRef[IO, Boolean], totalIO: Int, chunkIO: Int): fs2.Stream[IO, Byte] =
            for{
                      x <- fs2.io.file.readAll[IO](java.nio.file.Paths.get("1").toAbsolutePath, chunkIO)
                      size <- fs2.Stream.eval(switchTotal.updateAndGet( _ + BigInt(chunkIO)))
                      _ <- fs2.Stream.eval(switch.set(size >= totalIO))
            } yield x

        for {
            chunkIO <- fs2.Stream.eval(chunk)
            totalIO <- fs2.Stream.eval(total)
            timeIO <- fs2.Stream.eval(time)
            _ <- fs2.Stream(println(java.time.LocalTime.now))
            _ <- fs2.Stream(println(s"total: ${total} , chunk: ${chunk}, time: ${time} "))
            switch <- fs2.Stream.eval(createSwitch)
            switchTotal <- fs2.Stream.eval(createTotal)
            x <- program(switchTotal, switch, totalIO, chunkIO).metered(timeIO.seconds).interruptWhen(switch)
        } yield x

    }

    def slow(chunk: IO[Int], total: IO[Int], time: IO[Int]): IO[Byte] = program1(chunk, total, time).compile.lastOrError

    def checkValues[F[_]](f: => Int)(implicit ae: ApplicativeError[F, Throwable]) = ae.fromTry(Try{Try(f) match {
        case scala.util.Success(value) if value >= 0 => value.pure[F]
        case scala.util.Success(value) if value < 0 => ae.raiseError[Int](new RuntimeException("checkValue failed"))
        case scala.util.Failure(_) => ae.raiseError[Int](new RuntimeException("checkValue failed"))
    }})
    def checkVal(chunk: String, total: String, time: String) = for {
        a <- checkValues[IO](chunk.toInt)
        b <- checkValues[IO](total.toInt)
        c <- checkValues[IO](time.toInt)
    } yield (a,b,c)

    case class Counter(value: String)

    implicit val CounterDecoder: EntityDecoder[IO, Counter] = jsonOf[IO, Counter]

    implicit val CounterEncoder: Encoder[Counter] =
        Encoder.instance { (counter: Counter) =>
            json"""{"counter": ${counter.value}}"""
        }

    def serviceCounter(sessions: Ref[IO, Int]) = HttpRoutes.of[IO] {
        case GET -> Root / "counter" =>
            Ok(counterRoutes(sessions))
    }

    /*def countRoutes[F[_]: Defer: Monad](ref: Ref[F, Int]): HttpRoutes[F] = {
        val dsl = new Http4sDsl[F]{}
        import dsl._
        HttpRoutes.of[F] {
            case GET -> Root / "counter" =>
                for {
                    current  <- ref.updateAndGet(_ + 1)
                    resp <- Ok(current.toString)
                } yield resp
        }
    }*/


    def counterRoutes(sessions: Ref[IO, Int]) = for {
      count <-  sessions.updateAndGet(_ + 1)
    } yield Counter(s"${count}")

    def jsonApp(sessions: Ref[IO, Int]) = HttpRoutes.of[IO] {

        case GET -> Root / "counter"  => {
            Ok(counterRoutes(sessions))}

        case GET -> Root / "slow"/chunk/total/time =>
            checkVal(chunk, total, time).attempt.unsafeRunSync match {
                case Left(e) => BadRequest(e.getMessage)
                case Right(r) => slow(r._1, r._2, r._3).map(_.toString).attempt.unsafeRunSync match {
                    case Left(e) => BadRequest(e.getMessage)
                    case Right(ret) => Ok(ret)
                }
            }

    }.orNotFound

    val runRestfullHomework =
        for {
            state <- Resource.eval(Ref.of[IO, Int](0))
            exitCode <- EmberServerBuilder
               .default[IO]
        .withHost(Host.fromString("localhost").get)
        .withPort(Port.fromInt(8080).get)
        .withHttpApp(jsonApp(state))
        .build
            //.use(_ => IO.never)
        .as(ExitCode.Success)
             } yield exitCode
}
object mainServer extends IOApp.Simple {
    def run(): IO[Unit] = {
        RestfullHomework.runRestfullHomework.use(_ => IO.never)
    }
}

object HttpClient {
    val builder: Resource[IO, Client[IO]] = EmberClientBuilder.default[IO].build
    //val request = Request[IO](uri = Uri.fromString("http://localhost:8080/counter").toOption.get)
    val request = Request[IO](uri = Uri.fromString("http://localhost:8080/slow/10/1024/1").toOption.get)

    val result = builder.use(
        client => client.run(request).use(
            resp => {
                if (resp.status.isSuccess)
                    resp.body.compile.to(Array).map(new String(_))
                else
                    IO(s"Error ${resp}")
            }
        )
    )
}

object mainServerHttp4sClient extends IOApp.Simple {
    def run(): IO[Unit] = {
        for {
            _ <- RestfullHomework.runRestfullHomework.use(_ => HttpClient.result.flatMap(IO.println) *> IO.never)
        } yield ()
    }
}