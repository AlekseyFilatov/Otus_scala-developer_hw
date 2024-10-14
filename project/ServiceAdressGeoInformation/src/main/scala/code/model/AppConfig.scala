package code.model

import zio._
import zio.config._
import zio.config.magnolia.deriveConfig
import zio.config.typesafe._

final case class AppConfig(httpPort: Int)

object AppConfig {
  lazy val config: Config[AppConfig] = deriveConfig[AppConfig].nested("appConfig")

  val live: Layer[Config.Error, AppConfig] =
    ZLayer.fromZIO(
      ZIO.config[AppConfig](config).tap { config =>
        ZIO.logInfo(s"""
                       |appConfig server configuration:
                       |port: ${config.httpPort}
                       |""".stripMargin)
      }
    )

}
