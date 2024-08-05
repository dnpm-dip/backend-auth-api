
/*
 build.sbt adapted from https://github.com/pbassiner/sbt-multi-project-example/blob/master/build.sbt
*/


name         := "auth-api"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.13"
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
     fake_auth_service,
     authup_client,
     standalone_authup_client
  )

lazy val api = project
  .settings(
    name := "auth-api",
    settings,
    libraryDependencies ++= Seq(
      dependencies.service_base,
      dependencies.play,
    )
  )

lazy val fake_auth_service = project
  .settings(
    name := "fake-auth-service",
    settings,
    libraryDependencies ++= Seq(
      dependencies.scalatest_play,
      dependencies.mtb_api,
      dependencies.rd_api
    )
  )
  .dependsOn(api)


lazy val authup_client = project
  .settings(
    name := "authup-client",
    settings,
    libraryDependencies ++= Seq(
      dependencies.scalatest_play,
      dependencies.play_ws,
    )
  )
  .dependsOn(api)

lazy val standalone_authup_client = project
  .settings(
    name := "standalone-authup-client",
    settings,
    libraryDependencies ++= Seq(
      dependencies.scala_xml,
      dependencies.scalatest_play,
      dependencies.play_standalone_ws,
      dependencies.play_standalone_json,
      dependencies.mtb_api,
      dependencies.rd_api
    )
  )
  .dependsOn(api)

//-----------------------------------------------------------------------------
// DEPENDENCIES
//-----------------------------------------------------------------------------

lazy val dependencies =
  new {
    val scala_xml            = "org.scala-lang.modules" %% "scala-xml"               % "2.2.0"
    val scalatest            = "org.scalatest"          %% "scalatest"               % "3.1.1" % Test
    val scalatest_play       = "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1" % Test
    val play                 = "com.typesafe.play"      %% "play"                    % "2.9.1"
    val play_ws              = "com.typesafe.play"      %% "play-ws"                 % "2.9.1"
    val play_standalone_ws   = "com.typesafe.play"      %% "play-ahc-ws-standalone"  % "2.2.5"
    val play_standalone_json = "com.typesafe.play"      %% "play-ws-standalone-json" % "2.2.5"
    val service_base         = "de.dnpm.dip"            %% "service-base"            % "1.0-SNAPSHOT"
    val mtb_api              = "de.dnpm.dip"            %% "mtb-query-service-api"   % "1.0-SNAPSHOT" % Test
    val rd_api               = "de.dnpm.dip"            %% "rd-query-service-api"    % "1.0-SNAPSHOT" % Test
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

