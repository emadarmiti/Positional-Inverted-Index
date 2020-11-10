package Inverted_Index


import java.io.{File, PrintWriter}
import com.github.tototoshi.csv._
import io.circe.generic.auto._
import io.circe.syntax._

import scala.io.Source.fromFile

object TextProcessing {
  var stop_words: Seq[String] = Seq()

  def readStopWords(dir_path: String) {
    val stop_words_file = fromFile(new File(dir_path + "/stop_words.txt"))
    this.stop_words = stop_words_file.getLines().toList
    stop_words_file.close()
  }

  def isStopWord(token: String): Boolean = {
    this.stop_words.contains(token)
  }

  def tokenization(tokens_string: String): Array[String] = {
    val tokens = tokens_string
      .split("[.{},/!@#$%^&*()_+?<>\" ]")
    var tokenized_tokens: Array[String] = Array()
    if (tokens.nonEmpty)
      tokenized_tokens = tokens
        .map(_.toLowerCase())
        .filter(!this.isStopWord(_))
        .reduceLeft(_ + " " + _)
        .split("[' ]")
        .filter(!this.isStopWord(_))
        .filter(_.length >= 3)

    tokenized_tokens
  }
}

case class TermPositionsInDoc(var element: Either[Int, Map[Int, Array[Int]]])

case class SlaveNodeIndex(var file: File, var doc_id: Int) {

  var index: Map[String, Array[TermPositionsInDoc]] = this.createIndex()


  def readDocument(): Array[String] = {

    val doc_file = fromFile(this.file)
    val lines = doc_file
      .getLines()
      .toList
    var tokens = ""
    if (lines.nonEmpty)
      tokens = lines.reduceLeft(_ + _)
    doc_file.close()

    TextProcessing.tokenization(tokens)
  }

  def createIndex(): Map[String, Array[TermPositionsInDoc]] = {


    val tokens = this.readDocument()
    val index = tokens
      .zipWithIndex
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toList
      .map(pair => pair._1 -> Array(TermPositionsInDoc(Right(Map(this.doc_id -> pair._2)))))
      .toMap

    index

  }
}

class PositionalInvertedIndex(dir_path: String) {

  var id_map_path: scala.collection.mutable.Map[Int, String] = scala.collection.mutable.Map()
  var positional_inverted_index: Map[String, Array[TermPositionsInDoc]] = Map()
  var doc_id: Int = 1


  def readStopWords(): Seq[String] = {
    val stop_words_file = fromFile(new File(this.dir_path + "/stop_words.txt"))
    val stop_words = stop_words_file.getLines().toList
    stop_words_file.close()
    stop_words

  }

  def getNextIndex: Int = {
    this.doc_id += 1
    this.doc_id - 1
  }

  def getListOfFiles(dir: String): List[File] = {
    val directory = new File(dir)
    if (directory.exists && directory.isDirectory) {
      directory.listFiles.filter(_.isFile).toList
    } else {
      List[File]()
    }
  }

  def margeIndexes(set_of_inverted_indexes: List[Set[(String, Array[TermPositionsInDoc])]]) {


    this.positional_inverted_index = set_of_inverted_indexes
      .reduceLeft(_ ++ _)
      .groupBy(_._1)
      .view
      .mapValues(_.map(_._2))
      .toMap
      .map(term => {
        term._1 -> term._2.
          reduceLeft((x, y) => {
            x ++ y
          })
      })
      .map(x => {
        val new_list = TermPositionsInDoc(Left(x._2.length)) +: x._2
        x._1 -> new_list
      })


  }

  def writeDocMapToCsv(): Unit = {

    val writer = CSVWriter.open(new File(this.dir_path + "/results/docId_filePath_mapping.csv"))

    writer.writeAll(List(List("Id", "Path")) ++ this.id_map_path.map(x => List(x._1, x._2)).toSeq)
    writer.close()
  }

  def writeIndexToJson(): Unit = {

    var inverted_index_json: String = "{\n}"

    if (this.positional_inverted_index.nonEmpty) {
      inverted_index_json = this.positional_inverted_index.keys
        .map(x => Map(x -> this.positional_inverted_index(x).asJson.findAllByKey("value")))
        .reduceLeft(_.++(_))
        .asJson
        .toString
    }
    val json_file = new PrintWriter(new File(this.dir_path + "/results/pos_inverted_index.json"))
    json_file.write(inverted_index_json)
    json_file.close()
  }

  def build(): Unit = {

    val files = getListOfFiles(dir_path + "/documents")

    if (files.exists(_.toString.endsWith(".txt"))) {
      TextProcessing.readStopWords(this.dir_path)
      val set_of_inverted_indexes = files
        .filter(_.toString.endsWith(".txt"))
        .map(doc => {
          val inverted_index = SlaveNodeIndex(doc, this.getNextIndex)
          this.id_map_path(inverted_index.doc_id) = doc.toString
          inverted_index.index.toSet
        })

      margeIndexes(set_of_inverted_indexes)

      this.writeDocMapToCsv()

      this.writeIndexToJson()

      println("The positional inverted index has been created!")

      return
    }
    println("Error! There are no documents to use!")

  }

  def DocsBasedQuery(query: String): Unit = {

    val tokens = TextProcessing.tokenization(query)

    val tokens_exists_in_index = tokens.filter(this.positional_inverted_index.contains(_))

    if (tokens.nonEmpty && tokens_exists_in_index.length == tokens.length) {

      val result = tokens
        .map(term => this.positional_inverted_index(term))
        .reduceLeft(_ ++ _)
        .filter(_.element match {
          case Right(r) => true
          case Left(r) => false
        })
        .map(x => x.element match {
          case Right(r) => r
        })
        .groupBy(_.keys)
        .map(x => (x._1.head, x._2.map(_.values.head)))
        .filter(_._2.length == tokens.length)
        .map(x => (x._1, x._2.zip(x._2.drop(1))))
        .map(x => (x._1, x._2.map(x => {
          x._1.map(_ + 1).intersect(x._2).length
        }).sum == tokens.length - 1))
        .filter(_._2 == true)

      if (result.nonEmpty) {
        println(result.groupBy(_._2)
          .view.mapValues(_.keys)(true)
          .map(x => this.id_map_path(x)))

        return
      }


    }
    println("No matches for your query!")
  }
}

object Inverted_Index {

  def main(args: Array[String]): Unit = {

    val index = new PositionalInvertedIndex(args(0))
    index.build()
    index.DocsBasedQuery(args(1))


  }

}