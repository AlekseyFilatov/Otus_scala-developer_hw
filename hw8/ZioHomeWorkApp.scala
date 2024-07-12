package module3.zio_homework

import module3.zio_homework.terminal
import zio.ZIO.{debug, environment}
import zio.clock.Clock
import zio.console.Console
import zio.random.Random
import zio.test.environment.{TestClock, live}
import zio.{ExitCode, URIO, ZLayer}


object ZioHomeWorkApp extends zio.App {

  override def run(args: List[String]): URIO[zio.clock.Clock with zio.random.Random with zio.console.Console, ExitCode] = ???

}
import zio._

object MyZioAppUsingUnsafeRun extends scala.App {
  zio.Runtime.default.unsafeRunSync(runApp)
    /*.fold(
    e => println(s"Failure: $e"),
    v => println(s"Success: $v"))*/
}

object MyZioEnvAppUsingUnsafeRun extends scala.App {
  zio.Runtime.default.unsafeRunSync(appSpeedUp)
}

