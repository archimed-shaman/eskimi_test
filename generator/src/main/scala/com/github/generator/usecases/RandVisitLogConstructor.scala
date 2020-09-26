package com.github.generator.usecases

import java.util.Date
import com.github.generator.domain.VisitLog

trait RandVisitLogConstructor extends ConstructorComponent[VisitLog] {
  def constructor = new VisitLogConstructor

  class VisitLogConstructor extends Constructor {
    override def apply(day: Date): IterableOnce[VisitLog] =
      Iterator.continually(RandVisitLogConstructor.makeRandomEvent(day))
  }

}

object RandVisitLogConstructor {
  import scala.util.Random
  import java.util.Locale

  private val maxKnownUsers = 1000000
  private val maxSiteId = 25000
  private val maxKeywordList = 10
  private val maxKnownKeywords = 1000

  private val countryCodes = Locale.getISOCountries()

  final def makeRandomEvent(day: Date): VisitLog = {
    val uid = makeRandomUID(maxKnownUsers)
    val country = makeRandomCountry
    val city = makeRandomCity(country)
    val gender = makeRandomGender
    val yob = makeRandomYob
    VisitLog(uid, country, city, gender, yob, makeRandomKeywords, makeRandomSiteID, makeRandomTimeWithinDay(day))
  }

  private def makeRandomUID(maxValue: Int): String = {
    val id = randomInt(maxValue)
    s"dmp$id" // StringBuilder may be more effective in real life
  }

  private def makeRandomCountry: String = {
    val i = randomInt(countryCodes.length)
    countryCodes(i)
  }

  private def makeRandomCity(cc: String): Option[String] = {
    GeoData.cities
      .get(cc)
      .map(l => l(randomInt(l.length)))
  }

  private def makeRandomGender: Option[Int] = randomIntOpt(3)

  private def makeRandomYob: Option[Int] = randomIntOpt(200)

  private def makeRandomKeywords: Option[Array[Int]] = {
    randomInt(maxKeywordList) match {
      case 0 => None
      case listSize =>
        Some(
          (0 to listSize)
            .map(_ => randomInt(maxKnownKeywords))
            .toArray
        )
    }
  }

  private def makeRandomSiteID: Option[Int] =
    randomIntOptGauss(maxSiteId / 2, maxSiteId / 5)

  private def makeRandomTimeWithinDay(day: Date): Date = {
    val d = day.clone().asInstanceOf[Date]
    d.setHours(randomInt(24))
    d.setMinutes(randomInt(60))
    d.setSeconds(randomInt(60))
    d
  }

  private def randomInt(max: Int): Int = {
    Random.nextInt(max)
  }

  private def randomIntOpt(max: Int): Option[Int] = {
    Random.nextInt(max) match {
      case 0 => None
      case v => Some(v)
    }
  }

  private def randomIntOptGauss(mean: Int, std: Int): Option[Int] = {
    (Random.nextGaussian * std + mean).toInt match {
      case v if (v < mean - 3 * std) || (v > mean + 3 * std) => None
      case v                                                 => Some(v)
    }
  }
}
