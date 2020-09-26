package com.github.generator.usecases

import java.util.Date

trait ConstructorComponent[T] {
  def constructor: Constructor

  trait Constructor {
    def apply(day: Date): IterableOnce[T]
  }
}

trait OutputComponent[T] {
  def writer: Writer

  trait Writer {
    def apply(data: IterableOnce[T])
  }
}

class StreamComposer[T](batchSize: Int) {
  self: ConstructorComponent[T] with OutputComponent[T] =>

  def run(day: Date) = {
    writer.apply(constructor(day).iterator.take(batchSize))
  }
}
