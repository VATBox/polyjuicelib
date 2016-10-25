import sbt._

name := """polyjuice"""
maintainer := "Tal <tal@vatbox.com>"
packageSummary := "Polyjuice js mapper"
packageDescription := "Library that takes js and applies it to the given JSON object from the JVM"
version := "1.1.6"

lazy val `polyjuice` = project.in(file("."))

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
    "org.scalatest" %% "scalatest" % ScalaTest % "test" withSources()
  )
}