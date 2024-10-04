package code.middleware

import zio.{Trace, http}
import zio.http._
import zio.logging.LogAnnotation
import zio.metrics.MetricKeyType.Counter
import zio.metrics.{Metric, MetricState}

import java.util.UUID

class LoggingMiddleware extends http.Middleware[Any] {
  private val urlPathAnnotation: LogAnnotation[String] = LogAnnotation[String](
    name = "url_path",
    combine = (_, r) => r,
    render = identity
  )

  private val countRequests: Metric[Counter, Any, MetricState.Counter] = Metric.counter("countRequests").contramap[Any](_ => 1L)


  override def apply[Env1 <: Any, Err](app: Routes[Env1, Err]): Routes[Env1, Err] =
    app.transform { handler =>
      Handler.fromFunctionZIO[Request] { request =>
        handler.runZIO(request) @@
          LogAnnotation.TraceId(UUID.randomUUID()) @@
          urlPathAnnotation(request.path.toString()) @@
          countRequests.tagged("path", request.path.toString())
      }
    }
}

