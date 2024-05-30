name := "finagle-scala"

version := "1.0"

scalaVersion := s"2.13.10"

// Run in a separate JVM, to make sure sbt waits until all threads have
// finished before returning.
// If you want to keep the application running while executing other
// sbt tasks, consider https://github.com/spray/sbt-revolver/
fork := true

libraryDependencies ++= Seq(
  // finagle
  "com.twitter" %% "finagle-core" % "22.7.0",
  "com.twitter" %% "finagle-http" % "22.7.0",
  "org.slf4j" % "slf4j-api" % "1.7.5",
  "org.slf4j" % "slf4j-log4j12" % "1.7.5",
  "org.apache.logging.log4j" % "log4j-1.2-api" % "2.17.2"
)

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-encoding", "utf8",
  "-feature",
  "-language:postfixOps",
  "-language:implicitConversions"
)
