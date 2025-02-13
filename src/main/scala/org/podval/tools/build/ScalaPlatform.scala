package org.podval.tools.build

import org.podval.tools.util.Strings

sealed trait ScalaPlatform:
  def version: ScalaVersion

  def backend: ScalaBackend
  
  def toJvm: ScalaPlatform.Jvm
  
  final def isScalaJS: Boolean = backend.isJS

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
  sealed trait Jvm extends ScalaPlatform:
    final override def backend: ScalaBackend = ScalaBackend.Jvm
    final override def toJvm: ScalaPlatform.Jvm = this
    def scalaLibrary: JavaDependency

  sealed trait Js extends ScalaPlatform:
    final override def backend: ScalaBackend = ScalaBackend.JS

  sealed trait Scala3 extends ScalaPlatform:
    final override def version: ScalaVersion = ScalaVersion.Scala3
  
  object Scala3:
    def get(isScalaJS: Boolean): Scala3 = if isScalaJS then JS else Jvm
    
    object Jvm extends Scala3 with ScalaPlatform.Jvm:
      override val scalaLibrary: JavaDependency = JavaDependency(group = "org.scala-lang", artifact = "scala3-library_3")

    object JS extends Scala3 with ScalaPlatform.Js:
      final override def toJvm: ScalaPlatform.Jvm = Jvm
      // Note: there is no Scala 2 equivalent
      val scalaLibrary: ScalaDependency = dependency(group = "org.scala-lang", artifact = "scala3-library")

  sealed trait Scala2 extends ScalaPlatform:
    final override def version: ScalaVersion = ScalaVersion.Scala2

  object Scala2:
    def get(isScalaJS: Boolean): Scala2 = if isScalaJS then JS else Jvm

    object Jvm extends Scala2 with ScalaPlatform.Jvm:
      override val scalaLibrary: JavaDependency = JavaDependency(group = "org.scala-lang", artifact = "scala-library")

    object JS extends Scala2 with ScalaPlatform.Js:
      final override def toJvm: ScalaPlatform.Jvm = Jvm

  final class WithScalaVersion private(
    val scalaPlatform: ScalaPlatform,
    val scalaVersion: Version
  ):
    def isScalaJS: Boolean = scalaPlatform.isScalaJS
    
    def jvmScalaLibrary: Dependency.WithVersion =
      scalaPlatform.toJvm.scalaLibrary.withVersion(scalaVersion)

    private def toJvm: WithScalaVersion = new WithScalaVersion(
      scalaPlatform.toJvm, 
      scalaVersion
    )

    private def toScala2: WithScalaVersion =
      if !scalaPlatform.version.isScala3 then this else new WithScalaVersion(
        scalaPlatform = Scala2.get(scalaPlatform.isScalaJS),
        scalaVersion = ScalaVersion.Scala3.scala2versionMinor
      )

    def to(
      jvm: Boolean,
      scala2: Boolean
    ): WithScalaVersion =
      val forScala2: WithScalaVersion =
        if !scala2
        then this
        else this.toScala2

      if !jvm
      then forScala2
      else forScala2.toJvm
      
    def dependency(
      group: String,
      artifact: String,
      isScalaVersionFull: Boolean = false
    ): ScalaDependency.WithScalaVersion = scalaPlatform
      .dependency(
        group,
        artifact, 
        isScalaVersionFull
      )
      .withScalaVersion(
        scalaVersion
      )
  
  object WithScalaVersion:
    def apply(
      scalaVersion: Version,
      isScalaJS: Boolean
    ): WithScalaVersion = new WithScalaVersion(
      scalaVersion = scalaVersion,
      scalaPlatform =
        if ScalaVersion.Scala3.isSameMajor(scalaVersion)
        then Scala3.get(isScalaJS)
        else Scala2.get(isScalaJS)
    )
