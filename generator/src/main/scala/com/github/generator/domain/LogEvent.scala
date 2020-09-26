package com.github.generator.domain

import java.util.Date

case class VisitLog(
    dmpId: String,
    country: String,
    city: Option[String] = None,
    gender: Option[Int] = None,
    yob: Option[Int] = None,
    keywords: Option[Array[Int]] = None,
    siteId: Option[Int] = None,
    eventDate: Date = new java.util.Date()
)
