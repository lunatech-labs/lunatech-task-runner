name := "task-runner-core"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "org.specs2" %% "specs2" % "2.3.8" % "test",
  "org.mockito" % "mockito-core" % "1.9.5" % "test")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/")

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
