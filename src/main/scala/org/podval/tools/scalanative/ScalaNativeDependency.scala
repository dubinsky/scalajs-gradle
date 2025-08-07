package org.podval.tools.scalanative

import org.podval.tools.nonjvm.NonJvmBackend.Dep

object ScalaNativeDependency:
  object LibraryScala3  extends Dep("scala3lib"     , "Scala 3 Library", _.scala3.withVersionCompound)
  object LibraryScala2  extends Dep("scalalib"      , "Scala 2 Library", _.scala2.withVersionCompound)
  object CompilerPlugin extends Dep("nscplugin"     , "Compiler Plugin")
  object Junit4Plugin   extends Dep("junit-plugin"  , "JUnit4 Compiler Plugin for generating bootstrappers")
  object Linker         extends Dep("tools"         , "Build Tools, including Linker")
  object TestAdapter    extends Dep("test-runner"   , "Test Runner")
  object TestBridge     extends Dep("test-interface", "SBT Test Interface")

  object NativeLib      extends Dep("nativelib"     , "Native Library")
  object CLib           extends Dep("clib"          , "C Library")
  object PosixLib       extends Dep("posixlib"      , "Posix Library")
  object WindowsLib     extends Dep("windowslib"    , "Windows Library")
  object JavaLib        extends Dep("javalib"       , "Java Library")
  object AuxLib         extends Dep("auxlib"        , "Aux Library")
  