import sbt._

name := """polyjuice"""
maintainer := "Tal <tal@vatbox.com>"
packageSummary := "Polyjuice js mapper"
packageDescription := "Library that takes js and applies it to the given JSON object from the JVM"
version := "1.1.7"
lazy val `polyjuice` = project.in(file("."))

credentials += Credentials(Path.userHome / ".ivy2" / ".sonatypecredentials")
publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots") 
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}
publishMavenStyle := true
pomIncludeRepository := { _ => false }
pomExtra := (
  <url>https://vatbox.github.io/polyjuicelib/</url>
  <licenses>
    <license>
      <name>Apache-style</name>
      <url>https://opensource.org/licenses/Apache-2.0</url>
      <distribution>repo</distribution>
    </license>
  </licenses>
  <scm>
    <url>https://github.com/VATBox/polyjuicelib.git</url>
    <connection>scm:git:git@github.com:VATBox/polyjuicelib.git</connection>
  </scm>
  <developers>
    <developer>
      <id>talgendler</id>
      <name>Tal Gendler</name>
      <url>https://github.com/talgendler</url>
    </developer>
  </developers>)

organization := "com.vatbox"
scalaVersion := "2.11.8"

libraryDependencies ++= {
  val ScalaTest = "2.2.6"
  val Json4sVersion = "3.4.0"
  Seq(
    "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0" % "provided",
    "org.json4s" %% "json4s-native" % Json4sVersion % "provided",
    "org.json4s" %% "json4s-ext" % Json4sVersion % "provided",
    /** Test dependencies */
    "org.scalatest" %% "scalatest" % ScalaTest % "test" withSources(),
    "org.scalacheck" %% "scalacheck" % "1.12.6" % "test"
  )
}