
/*
 build.sbt adapted from https://github.com/pbassiner/sbt-multi-project-example/blob/master/build.sbt
*/


name         := "auth-api"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.12"
ThisBuild / version      := "1.0-SNAPSHOT"


//-----------------------------------------------------------------------------
// PROJECTS
//-----------------------------------------------------------------------------

lazy val global = project
  .in(file("."))
  .settings(
    settings,
    publish / skip := true
  )
  .aggregate(
     api,
     authup_client
//     oauth2_api,
//     tests
  )

lazy val api = project
  .settings(
    name := "auth-api",
    settings,
    libraryDependencies ++= Seq(
      dependencies.dnpm_core,
      dependencies.play,
      dependencies.play_ws,
    )
  )

lazy val authup_client = project
  .settings(
    name := "authup-client",
    settings,
    libraryDependencies ++= Seq(
    )
  )
  .dependsOn(api)
/*
lazy val oauth2_api = project
  .settings(
    name := "oauth2-api",
    settings,
    libraryDependencies ++= Seq(
    )
  )
  .dependsOn(api)
*/

//-----------------------------------------------------------------------------
// DEPENDENCIES
//-----------------------------------------------------------------------------

lazy val dependencies =
  new {
    val scalatest   = "org.scalatest"     %% "scalatest"   % "3.1.1" % Test
    val dnpm_core   = "de.dnpm.dip"       %% "core"        % "1.0-SNAPSHOT"
    val play        = "com.typesafe.play" %% "play"        % "2.9.1"
    val play_ws     = "com.typesafe.play" %% "play-ws"      % "2.9.1"
  }


//-----------------------------------------------------------------------------
// SETTINGS
//-----------------------------------------------------------------------------

lazy val settings = commonSettings


lazy val compilerOptions = Seq(
  "-encoding", "utf8",
  "-unchecked",
  "-feature",
  "-language:postfixOps",
  "-Xfatal-warnings",
  "-deprecation",
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++=
    Seq("Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository") ++
      Resolver.sonatypeOssRepos("releases") ++
      Resolver.sonatypeOssRepos("snapshots")
  
)

