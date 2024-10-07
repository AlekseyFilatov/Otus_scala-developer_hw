import zio._
import zio.config.typesafe.FromConfigSourceTypesafe
import zio.logging.backend.SLF4J

object Main extends ZIOAppDefault {

  override val bootstrap: ZLayer[ZIOAppArgs, Any, Any] = {
    /*возвращение к исполнителю по умолчанию после выполнения блокирующего кода
    * >> с ZIO.2.1.2 RuntimeFlag.EagerShiftBack*/
    Runtime.setExecutor(Executor.makeDefault(autoBlocking = false)) >>>
    /*задаем собственный размер TreadPoolExecutor*/
    Runtime.setExecutor(Executor.fromThreadPoolExecutor(
      new java.util.concurrent.ThreadPoolExecutor(
        5,
        10,
        5000,
        java.util.concurrent.TimeUnit.MILLISECONDS,
        new java.util.concurrent.LinkedBlockingQueue[Runnable]()
      )
    )) >>>
    /*переопределяем logger*/
    Runtime.removeDefaultLoggers >>> SLF4J.slf4j
  }

  /*def runLog(info :String, head :String) :ZIO[Environment with ZIOAppArgs with Scope, Any, Any] =
    for {
      _ <- ZIO.logInfo(info) @@ zio.logging.loggerName(head)
      _ <- ZIO.logInfo("Confidential user operation") @@ SLF4J.logMarkerName("CONFIDENTIAL")
    } yield ()*/

  override def run: ZIO[Any with ZIOAppArgs with Scope, Nothing, ExitCode] =
    App.server.exitCode
}