package code
import zio.{Config, ConfigProvider}
import zio.config.magnolia._
import zio.config._

/*final case class AppConfig(postgresdb: AppConfig.gisDatabaseConfig)
object AppConfig {
  lazy val config: Config[AppConfig] = deriveConfig[AppConfig]

  final case class DataSource (user: String, portNumber: String, password: String)
  final case class gisDatabaseConfig(  dataSourceClassName :String,
                                       dataSource: DataSource, connectionTimeout: String)

}*/
