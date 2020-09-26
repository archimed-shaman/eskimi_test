package com.github.generator

import java.io.File
import java.text.SimpleDateFormat

import scala.collection.parallel.CollectionConverters._

import com.github.generator.usecases._
import com.github.generator.infrastructure._
import com.typesafe.scalalogging.LazyLogging

object main extends App with LazyLogging {
  // TODO: args check, now it throws exceptions

  val format = new SimpleDateFormat("yyyy-MM-dd")

  val day = args.lift(0).map(s => format.parse(s)).get
  val rows = args.lift(1).map(s => s.toInt).getOrElse(10000000)
  val dir = args.lift(2).getOrElse("generated")

  logger.info(s"""generator started with args [day: ${day}; rows: ${rows}; dir: ${dir}]""")

  val output = s"${dir}/${format.format(day)}"

  // create dir, if not exists
  val outputDir = new File(output)
  if (!outputDir.exists()) {
    outputDir.mkdirs()
  }

  val batchSize = 1000000

  (batchSize to rows by batchSize).par.map { batch: Int =>
    val generator = new StreamComposer[domain.VisitLog](batchSize) with RandVisitLogConstructor with CSVWriter {
      val dirName = output
      val fileName = s"part_${batch}"
    }

    generator.run(day)
    logger.info(s"batch $batch complete")
  }

  logger.info("generator stopped")
}
