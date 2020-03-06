import sbt.Keys.{scalaVersion, scalacOptions}

name := "ddd-to-the-code-workshop-sample-scala"

version := "0.1"

val globalScalaVersion = "2.13.1"
val http4sVersion = "0.21.1"
val circeVersion = "0.12.3"

scalaVersion := globalScalaVersion

val options = Seq(
  "-language:higherKinds",
  "-explaintypes",
  "-Ymacro-annotations",
  "Xlog-implicits"
)

lazy val support = (project in file("support")).settings(
  scalaVersion := globalScalaVersion,
  libraryDependencies ++= Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.scala-lang" % "scala-reflect" % scalaVersion.value,
    "com.chuusai" %% "shapeless" % "2.3.3",
    "org.tpolecat" %% "doobie-core" % "0.8.8",
    "org.apache.activemq" % "activemq-all" % "5.15.11",
    "org.typelevel" %% "cats-effect" % "2.1.0",
    "org.log4s" %% "log4s" % "1.8.2" % "provided",
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.tpolecat" %% "doobie-h2" % "0.8.8",
    "org.flywaydb" % "flyway-core" % "6.2.1",
    "io.circe" %% "circe-core" % circeVersion,
    "org.scalatest" %% "scalatest" % "3.1.0" % Test,
    "org.scalamock" %% "scalamock" % "4.4.0" % Test
  ),
  scalacOptions ++= options
)

val deps = {
  Seq(
    "org.typelevel" %% "cats-core" % "2.0.0",
    "org.typelevel" %% "cats-effect" % "2.1.0",
    "org.tpolecat" %% "doobie-core" % "0.8.8",
    "org.tpolecat" %% "doobie-h2" % "0.8.8",
    "org.tpolecat" %% "doobie-hikari" % "0.8.8",
    "com.h2database" % "h2" % "1.4.192",
    "org.http4s" %% "http4s-dsl" % http4sVersion,
    "org.http4s" %% "http4s-blaze-server" % http4sVersion,
    "org.http4s" %% "http4s-twirl" % http4sVersion,
    "org.flywaydb" % "flyway-core" % "6.2.1",
    "io.circe" %% "circe-core" % circeVersion,
    "io.circe" %% "circe-generic" % circeVersion,
    "io.circe" %% "circe-parser" % circeVersion,
    "io.circe" %% "circe-literal" % circeVersion,
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "org.apache.activemq" % "activemq-all" % "5.15.11",
    "com.github.pureconfig" %% "pureconfig" % "0.12.3",
    "org.scalatest" %% "scalatest" % "3.1.0" % Test,
    "org.scalamock" %% "scalamock" % "4.4.0" % Test
  )
}

lazy val registration = (project in file("registration"))
  .enablePlugins(SbtTwirl)
  .dependsOn(support % "compile->compile;test->test")
  .settings(
    scalaVersion := globalScalaVersion,
    sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq(sourceDirectory.value / "main" / "twirl"),
    libraryDependencies ++= deps,
    scalacOptions ++= options
  )

lazy val accounting = (project in file("accounting"))
  .enablePlugins(SbtTwirl)
  .dependsOn(support % "compile->compile;test->test")
  .settings(
    scalaVersion := globalScalaVersion,
    sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq(sourceDirectory.value / "main" / "twirl"),
    libraryDependencies ++= deps,
    scalacOptions ++= options
  )

lazy val rental = (project in file("rental"))
  .enablePlugins(SbtTwirl)
  .dependsOn(support % "compile->compile;test->test")
  .settings(
    scalaVersion := globalScalaVersion,
    sourceDirectories in(Compile, TwirlKeys.compileTemplates) := Seq(sourceDirectory.value / "main" / "twirl"),
    libraryDependencies ++= deps,
    scalacOptions ++= options
  )

lazy val root = (project in file("."))
  .aggregate(support, registration, rental, accounting)