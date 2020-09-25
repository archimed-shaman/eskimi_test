package com.github.generator

import com.github.generator.usecases._
import com.typesafe.scalalogging.LazyLogging

object main extends App with LazyLogging {
  // TODO: args check, now it throws exceptions
  val day = args.lift(0).map(s => new java.text.SimpleDateFormat("yyyy-MM-dd").parse(s)).get
  val batchSize = args.lift(1).map(s => s.toInt).getOrElse(1000000)

  logger.info(s"""generator started with args [day: ${day}; batchSize: ${batchSize}]""")

  // val generator = new StreamComposerImpl[domain.VisitLog] with RandomVisitLogConstructor

  // generator.run()

  class test extends RandVisitLogConstructor
  val c = new test
  c.constructor.apply(day).take(batchSize).foreach(println)

  logger.info("generator stopped")
}
