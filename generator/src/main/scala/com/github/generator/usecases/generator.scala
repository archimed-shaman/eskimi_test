package com.github.generator.usecases

import java.util.Date

trait ConstructorComponent[In] {
  def constructor: Constructor

  trait Constructor {
    def apply(day: Date): LazyList[In]
  }
}

trait OutputComponent[In] {
  def writer: Writer

  trait Writer {
    def apply(data: LazyList[In])
  }
}

trait StreamComposer[In, Out] {
  self: ConstructorComponent[In] with OutputComponent[In] =>

  def run(day: Date) = {
    writer.apply(make(day))
  }

  private def make(day: Date): LazyList[In] = constructor(day) #::: make(day)
}
