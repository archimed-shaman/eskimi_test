package com.github.generator

import java.io.File
import java.text.SimpleDateFormat

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

  val generator = new StreamComposer[domain.VisitLog, String] with RandVisitLogConstructor with CSVWriter {
    val dirName = output
    val batchSize = 100000
    val maxRows = rows
  }

  generator.run(day)
  logger.info("generator stopped")
}
