
val zioVersion = "2.0.21"
lazy val root = (project in file("."))
  .enablePlugins(JavaServerAppPackaging, JavaAgent)
  .settings(
name := "ServiceAdressGeoInformation",
version := "0.1",
organization := "alekseyfilatov",
scalaVersion := "2.13.14",
scalacOptions ++= Seq("-Ymacro-annotations", "-deprecation", "-unchecked"),
libraryDependencies ++= Seq(
  "dev.zio"           %% "zio"                % zioVersion,
  "dev.zio"           %% "zio-http"           % "3.0.0-RC8",
  "dev.zio"           %% "zio-json"           % "0.6.2",
  "dev.zio"           %% "zio-macros"         % zioVersion,
  "com.github.kstyrc" %  "embedded-redis"     % "0.6",
  "redis.clients"     %  "jedis"              % "5.1.3",
  "dev.zio"           %% "zio-test"           % zioVersion % Test,
  "dev.zio"           %% "zio-test-sbt"       % zioVersion % Test,
  "dev.zio"           %% "zio-test-magnolia"  % zioVersion % Test,
  "org.slf4j"         %  "slf4j-simple"       % "2.0.13",
  "org.slf4j"         %  "slf4j-api"          % "2.0.12",
  "dev.zio"           %% "zio-logging"        % "2.1.16",
  "dev.zio"           %% "zio-logging-slf4j"  % "2.1.16",
  "ch.qos.logback"    % "logback-classic"     % "1.4.8", //---->>2024-09-20
  "dev.zio"           %% "zio-streams"        % "2.0.15",
  "dev.zio"           %% "zio-config-magnolia" % "4.0.0-RC16",
  "dev.zio"           %% "zio-config-typesafe" % "4.0.0-RC16",
  "io.getquill"       %% "quill-zio"          % "4.8.4",
  "io.getquill"       %% "quill-jdbc-zio"     % "4.8.4",
  "org.postgresql"    %  "postgresql"         % "42.3.1",
  "dev.zio" %% "zio-metrics-connectors" % "2.1.0",
  "dev.zio" %% "zio-metrics-connectors-prometheus" % "2.1.0",
  "io.prometheus" % "simpleclient_common" % "0.16.0"
),
javaAgents += "io.opentelemetry.javaagent" % "opentelemetry-javaagent" % "1.28.0",
testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
)