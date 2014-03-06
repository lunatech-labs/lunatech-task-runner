name := "task-runner-squeryl-mysql-store"

libraryDependencies ++= Seq(
  "org.squeryl" %% "squeryl" % "0.9.5-6",
  "mysql" % "mysql-connector-java" % "5.1.29")

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
  "-Ywarn-all",
//  "-Ywarn-dead-code",        // N.B. doesn't work well with the ??? hole // Also doesn't work well with specs
  "-Ywarn-numeric-widen",
  "-Ywarn-value-discard")
