package com.github.generator.infrastructure

import java.io.File
import java.text.SimpleDateFormat

import scala.annotation.tailrec

import kantan.csv._
import kantan.csv.ops._
import kantan.csv.generic._

import com.github.generator.domain
import com.github.generator.usecases._

trait CSVWriter extends OutputComponent[domain.VisitLog] {
  def writer = new CSVWriterImpl

  val dirName: String
  val fileName: String

  implicit def ListEncoder[T]: CellEncoder[Array[T]] = {
    CellEncoder.from(_.map(i => s"'${i}'").mkString("[", ",", "]"))
  }

  implicit def OptionalInt: CellEncoder[Option[Int]] = {
    CellEncoder.from({
      case None    => "-1"
      case Some(i) => i.toString
    })
  }

  implicit val DateEncoder = CellEncoder.dateEncoder(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"))

  class CSVWriterImpl extends Writer {

    override def apply(data: IterableOnce[domain.VisitLog]) = {
      val out = new File(dirName, fileName)
      val writer = out.asCsvWriter[domain.VisitLog](
        rfc
          .withHeader("dmp", "country", "city", "gender", "yob", "keywords", "site_id", "event_date")
          .withQuotePolicy(CsvConfiguration.QuotePolicy.WhenNeeded)
      )

      writer.write(data).close()
    }
  }
}
