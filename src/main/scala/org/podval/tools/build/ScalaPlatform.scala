package org.podval.tools.build

import org.podval.tools.util.Strings

sealed class ScalaPlatform(
  val version: ScalaVersion,
  backend: ScalaBackend
):
  final def dependency(
    group: String,
    artifact: String,
    isScalaVersionFull: Boolean = false
  ): ScalaDependency = ScalaDependency(
    group,
    artifact,
    scalaPlatform = this,
    isScalaVersionFull
  )
  
  private final def artifactAndScalaVersion(artifactName: String): Option[(String, Version)] =
    val (artifactAndBackend: String, scalaVersionOpt: Option[String]) = Strings.split(artifactName, '_')
    val (artifact: String, backendSuffixOpt: Option[String]) = Strings.split(artifactAndBackend, '_')
    val matches: Boolean =
      (backend.suffix == backendSuffixOpt) &&
      scalaVersionOpt.isDefined
    if !matches then None else Some((artifact, Version(scalaVersionOpt.get)))

  final def scalaVersionForArtifactName(
    artifactName: String,
    artifactNameExpected: String
  ): Option[Version] = artifactAndScalaVersion(artifactName)
    .flatMap((artifact, scalaVersion) =>
      val matches: Boolean =
        (artifact == artifactNameExpected) &&
        version.isScalaVersionAcceptable(scalaVersion)
      if !matches then None else Some(scalaVersion)
    )

  final def artifactNameSuffix(
    isScalaVersionFull: Boolean,
    scalaVersion: Version
  ): String =
    val versionSuffix: String =
      if isScalaVersionFull
      then scalaVersion.toString
      else version.versionSuffix(scalaVersion)

    s"${backend.suffixString}_$versionSuffix"

object ScalaPlatform:
  def get(scalaVersion: Version, isScalaJS: Boolean): ScalaPlatform =
    if ScalaVersion.Scala3.isSameMajor(scalaVersion)
    then if isScalaJS then Scala3.JS else Scala3.Jvm
    else if isScalaJS then Scala2.JS else Scala2.Jvm

  def getJvmScalaLibrary(scalaVersion: Version): Dependency.WithVersion =
    val dependency: JavaDependency = 
      if ScalaVersion.Scala3.isSameMajor(scalaVersion)
      then ScalaPlatform.Scala3.Jvm.scalaLibrary
      else ScalaPlatform.Scala2.Jvm.scalaLibrary
    dependency.withVersion(scalaVersion)  
  
  object Scala3:
    object Jvm extends ScalaPlatform(ScalaVersion.Scala3, ScalaBackend.Jvm):
      val scalaLibrary: JavaDependency = JavaDependency(group = "org.scala-lang", artifact = "scala3-library_3")

    object JS  extends ScalaPlatform(ScalaVersion.Scala3, ScalaBackend.JS ):
      // Note: there is no Scala 2 equivalent
      val scalaLibrary: ScalaDependency = dependency(group = "org.scala-lang", artifact = "scala3-library")

  object Scala2:
    object Jvm extends ScalaPlatform(ScalaVersion.Scala2, ScalaBackend.Jvm):
      val scalaLibrary: JavaDependency = JavaDependency(group = "org.scala-lang", artifact = "scala-library")

    object JS extends ScalaPlatform(ScalaVersion.Scala2 , ScalaBackend.JS )
