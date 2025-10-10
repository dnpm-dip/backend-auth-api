// build.sbt adapted from https://github.com/pbassiner/sbt-multi-project-example/blob/master/build.sbt

import scala.util.Properties.envOrElse


name         := "auth-api"
ThisBuild / organization := "de.dnpm.dip"
ThisBuild / scalaVersion := "2.13.16"
ThisBuild / version      := envOrElse("VERSION","1.1.0")

val ownerRepo  = envOrElse("REPOSITORY","dnpm-dip/backend-auth-api").split("/")
ThisBuild / githubOwner      := ownerRepo(0)
ThisBuild / githubRepository := ownerRepo(1)


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
    val scalatest            = "org.scalatest"          %% "scalatest"               % "3.2.18" % Test
    val scalatest_play       = "org.scalatestplus.play" %% "scalatestplus-play"      % "7.0.1" % Test
    val play                 = "org.playframework"      %% "play"                    % "3.0.7"
    val play_ws              = "org.playframework"      %% "play-ws"                 % "3.0.7"
    val play_standalone_ws   = "org.playframework"      %% "play-ahc-ws-standalone"  % "3.0.7"
    val play_standalone_json = "org.playframework"      %% "play-ws-standalone-json" % "3.0.7"
    val service_base         = "de.dnpm.dip"            %% "service-base"            % "1.1.0"
    val mtb_api              = "de.dnpm.dip"            %% "mtb-query-service-api"   % "1.1.0" % Test
    val rd_api               = "de.dnpm.dip"            %% "rd-query-service-api"    % "1.1.0" % Test
  }


//-----------------------------------------------------------------------------
// SETTINGS
//-----------------------------------------------------------------------------

lazy val settings = commonSettings


// Compiler options from: https://alexn.org/blog/2020/05/26/scala-fatal-warnings/
lazy val compilerOptions = Seq(
  // Feature options
  "-encoding", "utf-8",
  "-explaintypes",
  "-feature",
  "-language:existentials",
  "-language:experimental.macros",
  "-language:higherKinds",
  "-language:implicitConversions",
  "-language:postfixOps",
  "-Ymacro-annotations",

  // Warnings as errors!
  "-Xfatal-warnings",

  // Linting options
  "-unchecked",
  "-Xcheckinit",
  "-Xlint:adapted-args",
  "-Xlint:constant",
  "-Xlint:delayedinit-select",
  "-Xlint:deprecation",
  "-Xlint:doc-detached",
  "-Xlint:inaccessible",
  "-Xlint:infer-any",
  "-Xlint:missing-interpolator",
  "-Xlint:nullary-unit",
  "-Xlint:option-implicit",
  "-Xlint:package-object-classes",
  "-Xlint:poly-implicit-overload",
  "-Xlint:private-shadow",
  "-Xlint:stars-align",
  "-Xlint:type-parameter-shadow",
  "-Wdead-code",
  "-Wextra-implicit",
  "-Wnumeric-widen",
  "-Wunused:imports",
  "-Wunused:locals",
  "-Wunused:patvars",
  "-Wunused:privates",
  "-Wunused:implicits",
  "-Wvalue-discard",
)

lazy val commonSettings = Seq(
  scalacOptions ++= compilerOptions,
  resolvers ++= Seq(
    "Local Maven Repository" at "file://" + Path.userHome.absolutePath + "/.m2/repository",
    Resolver.githubPackages("dnpm-dip"),
    Resolver.githubPackages("KohlbacherLab"),
    Resolver.sonatypeCentralSnapshots
  )
)

