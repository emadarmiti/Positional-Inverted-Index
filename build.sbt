name := "positional_inverted_index"

version := "0.1"

scalaVersion := "2.13.3"

val circeVersion = "0.12.3"

libraryDependencies ++= Seq(
  "io.circe" %% "circe-core",
  "io.circe" %% "circe-generic",
  "io.circe" %% "circe-parser"
).map(_ % circeVersion)
libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.6"