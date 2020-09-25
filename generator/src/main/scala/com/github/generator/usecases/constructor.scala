package com.github.generator.usecases

import java.util.{Locale, Random}
import java.util.Date
import com.github.generator.domain.VisitLog

trait RandVisitLogConstructor extends ConstructorComponent[VisitLog] {
  def constructor = new VisitLogConstructor

  class VisitLogConstructor extends Constructor {
    def apply(day: Date): LazyList[VisitLog] = makeRandomSession(day)
  }

  private def makeRandomSession(day: Date): LazyList[VisitLog] = {

    makeUIDStream(RandVisitLogConstructor.maxKnownUsers)
      .flatMap { uid =>
        val country = makeRandomCountry
        val city = makeRandomCity(country)
        val gender = makeRandomGender
        val yob = makeRandomYob

        makeRandomEvents(day, uid, country, city, gender, yob)
      } #::: makeRandomSession(day)
  }

  private def makeUIDStream(maxValue: Int): LazyList[String] = makeRandomUID(maxValue) #:: makeUIDStream(maxValue)

  private def makeRandomEvents(day: Date,
                               uid: String,
                               country: String,
                               city: Option[String],
                               gender: Option[Int],
                               yob: Option[Int]): List[VisitLog] = {
    val sequenceSize = RandVisitLogConstructor.rnd.nextInt(25)
    (0 to sequenceSize).map(_ => makeRandomEvent(day, uid, country, city, gender, yob)).toList
  }

  private def makeRandomEvent(day: Date,
                              uid: String,
                              country: String,
                              city: Option[String],
                              gender: Option[Int],
                              yob: Option[Int]): VisitLog = {
    VisitLog(uid, country, city, gender, yob, makeRandomKeywords, makeRandomSiteID, makeRandomTimeWithinDay(day))
  }

  private def makeRandomUID(maxValue: Int): String = {
    val id = randomInt(maxValue)
    s"dmp$id" // StringBuilder may be more effective in real life
  }

  private def makeRandomCountry: String = {
    val i = randomInt(RandVisitLogConstructor.countryCodes.length)
    RandVisitLogConstructor.countryCodes(i)
  }

  private def makeRandomCity(cc: String): Option[String] = {
    GeoData.cities
      .get(cc)
      .map(l => l(randomInt(l.length)))
  }

  private def makeRandomGender: Option[Int] = randomIntOpt(3)

  private def makeRandomYob: Option[Int] = randomIntOpt(200)

  private def makeRandomKeywords: Option[List[Int]] = {
    randomInt(RandVisitLogConstructor.maxKeywordList) match {
      case 0 => None
      case listSize =>
        Some(
          (0 to listSize)
            .map(_ => randomInt(RandVisitLogConstructor.maxKnownKeywords))
            .toList
        )
    }
  }

  private def makeRandomSiteID: Option[Int] =
    randomIntOptGauss(RandVisitLogConstructor.maxSiteId / 2, RandVisitLogConstructor.maxSiteId / 5)

  private def makeRandomTimeWithinDay(day: Date): Date = {
    val d = day.clone().asInstanceOf[Date]
    d.setHours(randomInt(24))
    d.setMinutes(randomInt(60))
    d.setSeconds(randomInt(60))
    d
  }

  private def randomInt(max: Int): Int = {
    RandVisitLogConstructor.rnd.nextInt(max)
  }

  private def randomIntOpt(max: Int): Option[Int] = {
    RandVisitLogConstructor.rnd.nextInt(max) match {
      case 0 => None
      case v => Some(v)
    }
  }

  private def randomIntOptGauss(mean: Int, std: Int): Option[Int] = {
    (RandVisitLogConstructor.rnd.nextGaussian * std + mean).toInt match {
      case v if (v < mean - 3 * std) || (v > mean + 3 * std) => None
      case v                                                 => Some(v)
    }
  }
}

object RandVisitLogConstructor {
  private val maxKnownUsers = 1000000
  private val maxSiteId = 25000
  private val maxKeywordList = 10
  private val maxKnownKeywords = 1000

  private val countryCodes = Locale.getISOCountries()
  private val rnd = new Random()
}
