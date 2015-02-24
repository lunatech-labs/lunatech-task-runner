name := "task-runner-core"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.3.9",
  "org.specs2" %% "specs2-core" % "2.4.16" % "test",
  "org.specs2" %% "specs2-mock" % "2.4.16" % "test",
  "org.specs2" %% "specs2-matcher-extra" % "2.4.16" % "test",
  "org.mockito" % "mockito-core" % "1.10.19" % "test")

resolvers ++= Seq(
  "Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases"
)
  

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
