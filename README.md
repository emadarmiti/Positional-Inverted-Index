## Introduction

- This project is an implementation of the Positional Inverted Index using Scala.

## How to use

to run the code

- add these dependencies to the build.sbt file:

  - val circeVersion = "0.12.3"

  - libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser").map(\_ % circeVersion)
  - libraryDependencies += "com.github.tototoshi" %% "scala-csv" % "1.3.6"

- in the sbt shell run this command " run --directory_path -query "

  - -query is the query to search for
  - --directory_path is the diretory path where tha files are exist.
    the directory should has these things:

    1. stop_words.txt -> a text file contains the stop words
    2. /documents -> where the documents are stored
    3. /results -> an empty folder to save the results inside it

    /--directory_path
    ├── documents
    │   ├── doc1.txt
    │   └── ...
    ├── stop_words.txt
    │
    └── results
