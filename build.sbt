
name := "salesforce-message-handler"

organization := "com.gu"

description:= "handle outbound messages from salesforce to update zuora and identity"

version := "1.0"

scalaVersion := "2.12.19"

scalacOptions ++= Seq(
  "-deprecation",
  "-encoding", "UTF-8",
  "-release:8",
  "-Ywarn-dead-code"
)

enablePlugins(ScalaxbPlugin)

Compile / scalaxb / scalaxbPackageName := "salesforce.soap"
Compile / scalaxb / scalaxbGenerateDispatchClient := false // we don't need to use the 'dispatch' library

val AwsVersion = "1.12.705"

libraryDependencies ++= Seq(
  "com.amazonaws" % "aws-lambda-java-core" % "1.2.1",
  "com.amazonaws" % "aws-java-sdk-sqs" % AwsVersion,
  "com.amazonaws" % "aws-java-sdk-s3" % AwsVersion,
  "org.scala-lang.modules" %% "scala-xml" % "2.2.0",
  "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
  "com.typesafe" % "config" % "1.4.3",
  "org.slf4j" % "slf4j-simple" % "1.7.35",
  "com.typesafe.play" %% "play-json" % "2.9.4",
  "org.scala-lang.modules" %% "scala-xml" % "2.0.1",
  "org.specs2" %% "specs2-core" % "4.13.2" % Test,
  "org.specs2" %% "specs2-matcher-extra" % "4.13.2" % Test,
  "org.specs2" %% "specs2-mock" % "4.13.2" % Test,
  "org.hamcrest" % "hamcrest-all" % "1.3" % Test
)

/* required to bump jackson versions due to CVE-2020-36518 */ 
val jacksonVersion         = "2.13.2"
val jacksonDatabindVersion = "2.13.2.2"

val jacksonDependencies = Seq(
  "com.fasterxml.jackson.core"     % "jackson-core" %  jacksonVersion,
  "com.fasterxml.jackson.core"     % "jackson-annotations" %  jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jdk8" %  jacksonVersion,
  "com.fasterxml.jackson.datatype" % "jackson-datatype-jsr310" %  jacksonVersion,
  "com.fasterxml.jackson.core" % "jackson-databind" % jacksonDatabindVersion,
  "com.fasterxml.jackson.dataformat" % "jackson-dataformat-cbor" % jacksonVersion,
  "com.fasterxml.jackson.module"     % "jackson-module-parameter-names" % jacksonVersion,
  "com.fasterxml.jackson.module"     %% "jackson-module-scala" % jacksonVersion,
)

dependencyOverrides ++= jacksonDependencies

/* EOF jackson version overrides */

assembly / assemblyJarName := s"${name.value}.jar"
assembly / assemblyOutputPath := file(s"${(assembly/assemblyJarName).value}")

//Having a merge strategy here is necessary as there is an conflict in the file contents for the jackson libs, there are two same versions with different contents.
//As a result we're picking the first file found on the classpath - this may not be required if the contents match in a future release
assembly / assemblyMergeStrategy := {
  case PathList("META-INF", _@_*) => MergeStrategy.discard
  case _ => MergeStrategy.first
}
