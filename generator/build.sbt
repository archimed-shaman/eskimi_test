lazy val root = (project in file("."))
  .settings(
    name := "generator",
    organization := "com.github",
    scalaVersion := "2.13.3",
    libraryDependencies ++= {
      Seq(
        "com.nrinaudo"               %% "kantan.csv"         % "0.6.1",
        "com.nrinaudo"               %% "kantan.csv-generic" % "0.6.1",
        "ch.qos.logback"             % "logback-classic"     % "1.2.3",
        "com.typesafe.scala-logging" %% "scala-logging"      % "3.9.2"
      )
    }
  )
