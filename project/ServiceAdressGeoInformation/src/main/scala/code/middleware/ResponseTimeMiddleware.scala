package code.middleware

import zio.http._
import zio.{Trace, ZIO}

class ResponseTimeMiddleware extends Middleware[Any] {

  def memoryUsage: ZIO[Any, Nothing, Double] = {
    import java.lang.Runtime._
    ZIO
      .succeed(getRuntime.totalMemory() - getRuntime.freeMemory())
      .map(_ / (1024.0 * 1024.0)) @@ zio.metrics.Metric.gauge("memory_usage")
  }

  override def apply[Env1 <: Any, Err](app: Routes[Env1, Err]): Routes[Env1, Err] =
    app.transform { handler =>
      Handler.fromFunctionZIO[Request] { request =>
        for {
          startTime <- ZIO.succeed(System.currentTimeMillis())
          _ <- memoryUsage
          response <- handler.runZIO(request)
          endTime <- ZIO.succeed(System.currentTimeMillis())
        } yield response.addHeader(Header.Custom("X-Response-Time", s"${endTime - startTime}"))
      }
    }
}
