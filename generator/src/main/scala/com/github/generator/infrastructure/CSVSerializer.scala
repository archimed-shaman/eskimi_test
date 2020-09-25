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
  val batchSize: Int
  val maxRows: Int

  implicit def ListEncoder[T]: CellEncoder[List[T]] = {
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

    override def apply(data: LazyList[domain.VisitLog]) = {
      // writeBatch(data, batchSize)

      data
        .grouped(maxRows / batchSize)
        .zipWithIndex
        .map {
          case (part, i) =>
            // (batchSize to maxRows by batchSize).foreach { part =>
            val out = new File(dirName, s"part_${(i + 1) * batchSize}.csv")
            val writer = out.asCsvWriter[domain.VisitLog](
              rfc
                .withHeader("dmp", "country", "city", "gender", "yob", "keywords", "site_id", "event_date")
                .withQuotePolicy(CsvConfiguration.QuotePolicy.WhenNeeded)
            )

            val ps = part.take(batchSize)
            writer.write(ps).close()
        }
    }

    // @tailrec
    // private def writeBatch(data: LazyList[domain.VisitLog], batchN: Int): Unit = batchN match {
    //   case part if part > maxRows => ()
    //   case part => {

    //     val (head, tail) = data.splitAt(batchSize)

    //     val out = new File(dirName, s"part_${part}.csv")
    //     val writer = out.asCsvWriter[domain.VisitLog](
    //       rfc
    //         .withHeader("dmp", "country", "city", "gender", "yob", "keywords", "site_id", "event_date")
    //         .withQuotePolicy(CsvConfiguration.QuotePolicy.WhenNeeded)
    //     )

    //     val ps = head.collect
    //     writer.write(ps).close()

    //     writeBatch(tail, batchN + batchSize)
    //   }
    // }
  }
}
