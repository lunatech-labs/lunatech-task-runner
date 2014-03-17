lazy val core = project

lazy val `mysql-job-store` = project.dependsOn(core % "test->test;compile->compile")

organization in ThisBuild := "com.lunatech.task-runner"

version in ThisBuild := "0.3-SNAPSHOT"

publishTo in ThisBuild <<= version { (v: String) =>
  val path = if(v.trim.endsWith("SNAPSHOT")) "snapshots-public" else "releases-public"
  Some(Resolver.url("Lunatech Artifactory", new URL("http://artifactory.lunatech.com/artifactory/%s/" format path)))
}

// Don't publish the (aggregate) root project
publish := ()

publishArtifact := false
