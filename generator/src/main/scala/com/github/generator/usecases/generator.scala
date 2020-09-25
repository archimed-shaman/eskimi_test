package com.github.generator.usecases

import java.util.Date

trait ConstructorComponent[T] {
  def constructor: Constructor

  trait Constructor {
    def apply(day: Date): LazyList[T]
  }
}

// trait SerializerComponent[T, S] {
//   def serializer: Constructor

//   trait Constructor {
//     def apply(data: T): S
//   }
// }

trait OutputComponent[T] {
  def writer: Writer

  trait Writer {
    def apply(data: LazyList[T])
  }
}

trait StreamComposer {
  def run(day: Date)
}

trait StreamComposerImpl[DataT] extends StreamComposer {
  self: ConstructorComponent[DataT] with OutputComponent[DataT] =>

  def run(day: Date) = {
    writer(make(day))
  }

  private def make(day: Date): LazyList[DataT] = constructor(day) #::: make(day)
}
