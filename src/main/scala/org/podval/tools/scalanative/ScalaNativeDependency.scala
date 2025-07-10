package org.podval.tools.scalanative

import org.podval.tools.build.{ScalaBackend, ScalaDependencyMaker, Version}
import org.podval.tools.jvm.JvmBackend

sealed abstract class ScalaNativeDependency(
  final override val artifact: String,
  what: String
) extends ScalaDependencyMaker:
  final override def description: String = ScalaNativeBackend.describe(what)
  final override def group: String = "org.scala-native"
  final override def versionDefault: Version = ScalaNativeBackend.versionDefault

object ScalaNativeDependency:
  sealed class Jvm(artifact: String, what: String) extends ScalaNativeDependency(artifact, what):
    final override def scalaBackend: ScalaBackend = JvmBackend

  object Linker       extends Jvm("tools", "Build Tools, including Linker")
  object TestAdapter  extends Jvm("test-runner", "Test Runner")
  
  object Compiler     extends Jvm("nscplugin", "Compiler Plugin"):
    override def isScalaVersionFull: Boolean = true

  object JUnit4Plugin extends Jvm("junit-plugin", "JUnit4 Compiler Plugin for generating bootstrappers"):
    override def isScalaVersionFull: Boolean = true
  
  sealed class ScalaNative(artifact: String, what: String) extends ScalaNativeDependency(artifact, what):
    final override def scalaBackend: ScalaBackend = ScalaNativeBackend

  object Scala3Lib  extends ScalaNative("scala3lib", "Scala 3 Library"):
    override def isVersionCompound: Boolean = true
  
  object ScalaLib   extends ScalaNative("scalalib", "Scala 2 Library"):
    override def isVersionCompound: Boolean = true
    override def isPublishedForScala3: Boolean = false

  object TestBridge extends ScalaNative("test-interface", "SBT Test Interface")

  object NativeLib  extends ScalaNative("nativelib" , "Native Library" )
  object CLib       extends ScalaNative("clib"      , "C Library"      )
  object PosixLib   extends ScalaNative("posixlib"  , "Posix Library"  )
  object WindowsLib extends ScalaNative("windowslib", "Windows Library")
  object JavaLib    extends ScalaNative("javalib"   , "Java Library"   )
  object AuxLib     extends ScalaNative("auxlib"    , "Aux Library"    )
