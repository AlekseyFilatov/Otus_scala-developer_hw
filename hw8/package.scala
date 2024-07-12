package module3

import zio.{Has, Schedule, Task, UIO, ULayer, URIO, ZIO, ZLayer}
import zio.clock.{Clock, nanoTime, sleep}
import zio.console._
import zio.duration.durationInt
import zio.macros.accessible
import zio.random._
import zio.test.Assertion.isGreaterThan
import zio.test._
import zio.test.environment.{Live, TestClock, TestEnvironment}

import java.io.IOException
import java.util.concurrent.TimeUnit
import scala.io.StdIn
import scala.language.postfixOps

package object zio_homework {
  /**
   * 1.
   * Используя сервисы Random и Console, напишите консольную ZIO программу которая будет предлагать пользователю угадать число от 1 до 3
   * и печатать в консоль угадал или нет. Подумайте, на какие наиболее простые эффекты ее можно декомпозировать.
   */



  lazy val guessProgram = for {
    rand <- zio.random.nextIntBetween(1,3)
    _ <- zio.console.putStrLn(s"Check random number: $rand")
    v <- zio.console.getStrLn.map(str => str.toInt)
    _ <- zio.ZIO.when(rand != v)(ZIO.fail(new Exception(s"$v is not an $rand!")))
    _ <- zio.console.putStrLn("Success!")
  } yield  v

  /**
   * 2. реализовать функцию doWhile (общего назначения), которая будет выполнять эффект до тех пор, пока его значение в условии не даст true
   *
   */

  def doWhile = zio.console.putStrLn("Retry!").retryWhile(_ => scala.util.Random.nextBoolean())

  /**
   * 3. Реализовать метод, который безопасно прочитает конфиг из файла, а в случае ошибки вернет дефолтный конфиг
   * и выведет его в консоль
   * Используйте эффект "load" из пакета config
   */


  def loadConfigOrDefault = config.load.orElse(ZIO.effect(config.AppConfig("123","321"))).debug

 /**
   * 4. Следуйте инструкциям ниже для написания 2-х ZIO программ,
   * обратите внимание на сигнатуры эффектов, которые будут у вас получаться,
   * на изменение этих сигнатур
   */


  /**
   * 4.1 Создайте эффект, который будет возвращать случайеым образом выбранное число от 0 до 10 спустя 1 секунду
   * Используйте сервис zio Random
   */
  lazy val eff: ZIO[Random with Clock with Console, Nothing, Int] = for {
    _ <- zio.clock.sleep(1 seconds)
    int <- zio.random.nextIntBetween(0, 10)
    _ <- zio.console.putStrLn(s"$int").orDie
  } yield int

  /**
   * 4.2 Создайте коллукцию из 10 выше описанных эффектов (eff)
   */

  lazy val effects = for {
    list <- ZIO.collectAllPar((1 to 10).map(x => eff))
  } yield list.toList


  /**
   *  эффект содержит в себе текущее время
   */
  val currentTime: URIO[Clock, Long] = zio.clock.currentTime(TimeUnit.SECONDS)

  /**
   * Напишите эффект, который будет считать время выполнения любого эффекта
   */
  def printEffectRunningTime[R, E, A](zio: ZIO[R, E, A]): ZIO[Console with Clock with R, E, A] = for{
    start <- currentTime
    r <- zio
    end <- currentTime
    _ <- putStrLn(s"Running time ${end - start}").orDie
  } yield r

  lazy val effectsSumm = for {
    el <- effects
  } yield el.sum


  /**
   * 4.3 Напишите программу которая вычислит сумму элементов коллекции "effects",
   * напечатает ее в консоль и вернет результат, а также залогирует затраченное время на выполнение,
   * можно использовать ф-цию printEffectRunningTime, которую мы разработали на занятиях
   */

  lazy val app = for {
    sum <- printEffectRunningTime(effectsSumm)
    _ <- zio.console.putStrLn(sum.toString)
  } yield ()


  /**
   * 4.4 Усовершенствуйте программу 4.3 так, чтобы минимизировать время ее выполнения
   */
  val appSpeedUp = {
    for {
      fiber <- app.fork
      //testClock <- ZIO.environment[TestClock].map(_.get)
      testClock <- ZIO.environment[zio.test.environment.TestClock].map(_.get)
      _ <- testClock.adjust(1 minute)
      //_ <- TestClock.adjust(1 minute)
      res <- fiber.join.orDie
    } yield res
  }.provideLayer(zio.test.environment.testEnvironment)

  /**
   * 5. Оформите ф-цию printEffectRunningTime разработанную на занятиях в отдельный сервис, так чтобы ее
   * можно было использовать аналогично zio.console.putStrLn например
   */
  object terminal {
    type Terminal = Has[Terminal.ServicePrint]

    object Terminal {
      trait ServicePrint {
        def printEffectRunningTimeService[R, E](zio: ZIO[R, E, Unit]): ZIO[Console with Clock with R, E, Unit]
      }

      object ServicePrint {
        val liveService: ServicePrint = new ServicePrint {
          override def printEffectRunningTimeService[R, E](zio: ZIO[R, E, Unit]): ZIO[Console with Clock with R, E, Unit] =
            for{
              start <- currentTime
              r <- zio
              end <- currentTime
              _ <- putStrLn(s"Running time ${end - start}").orDie
            } yield ()
        }
      }
      val live: ZLayer[Any, Nothing, Terminal] = ZLayer.succeed(Terminal.ServicePrint.liveService)
    }
  }


   /**
     * 6.
     * Воспользуйтесь написанным сервисом, чтобы созадть эффект, который будет логировать время выполнения прогаммы из пункта 4.3
     *
     * 
     */

  lazy val appWithTimeLogg = for {
    terminal <- ZIO.access[terminal.Terminal](_.get)
    _ <- terminal.printEffectRunningTimeService(app).orDie
  } yield ()

  /**
    * 
    * Подготовьте его к запуску и затем запустите воспользовавшись ZioHomeWorkApp
    */

  lazy val runApp = appWithTimeLogg.provideSomeLayer[zio.console.Console with zio.clock.Clock with zio.random.Random](terminal.Terminal.live)

}
