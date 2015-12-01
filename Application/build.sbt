name := """Application"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayJava)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
  javaJdbc,
  cache,
  javaWs
)

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
<<<<<<< HEAD
routesGenerator := InjectedRoutesGenerator
=======
routesGenerator := InjectedRoutesGenerator
>>>>>>> 40c62623dfa2b538e911082383ecffc40cde4b78
