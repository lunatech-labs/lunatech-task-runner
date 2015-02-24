name := "task-runner-squeryl-mysql-store"

libraryDependencies ++= Seq(
  "org.squeryl" %% "squeryl" % "0.9.5-7",
  "mysql" % "mysql-connector-java" % "5.1.34")
  
scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",       // yes, this is 2 args
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-unchecked",
  "-Xfatal-warnings",
  "-Xlint",
  "-Yno-adapted-args",
//  "-Ywarn-all",              // N.B. Doesn't seem to work in Scala 2.11
//  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole // Also doesn't work well with specs
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard")
