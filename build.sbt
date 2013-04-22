name := "Wesin" 

version := "1.0.0"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-deprecation")

libraryDependencies += "org.scalatest" % "scalatest_2.10" % "1.9.1" % "test"
// libraryDependencies += "org.scalatest" %% "scalatest" % "1.8" % "test"

libraryDependencies += "junit" % "junit" % "4.10" % "test"

// libraryDependencies += "com.assembla.scala-incubator" % "graph-core_2.9.2" % "1.5.1"
libraryDependencies += "com.assembla.scala-incubator" % "graph-core_2.10" % "1.6.1"

// org.scalastyle.sbt.ScalastylePlugin.Settings