import Dependencies._

lazy val root = (project in file(".")).
  settings(
    organization := "com.doradosystems",
    scalaVersion := "2.12.3",
    version      := "0.1.0-SNAPSHOT",
    name := "Client Service",
    resolvers ++= Seq(
      //"Dorado Maven Repo" at "https://nexus.doradosystems.com/nexus/content/repositories/public/",
      "Local Maven Repo" at "file:///home/vagrant/.m2/repository"
    ),
    libraryDependencies ++= Seq(
      // Scala dependencies
      scalaTest % Test,
      "org.skinny-framework" %% "skinny-http-client" % "2.5.1",
      "org.json4s" %% "json4s-native" % "3.2.11",
      "org.json4s" %% "json4s-jackson" % "3.2.11",
      // Java dependencies
      "com.doradosystems" % "dorado-integration-test-common" % "1.0.0-SNAPSHOT",
      "commons-io" % "commons-io" % "2.5"
    )
  )
