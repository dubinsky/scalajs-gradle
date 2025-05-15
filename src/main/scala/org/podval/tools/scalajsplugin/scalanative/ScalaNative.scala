package org.podval.tools.scalajsplugin.scalanative

import org.podval.tools.build.{CreateExtension, DependencyRequirement, ScalaBackendKind, ScalaDependency, ScalaPlatform,
  Version}
import org.podval.tools.scalajsplugin.nonjvm.NonJvm
import org.podval.tools.test.framework.JUnit4ScalaNative

object ScalaNative extends NonJvm[ScalaNativeTask]:
  override def taskClass        : Class[ScalaNativeTask        ] = classOf[ScalaNativeTask        ]
  override def linkTaskClass    : Class[ScalaNativeLinkMainTask] = classOf[ScalaNativeLinkMainTask]
  override def testLinkTaskClass: Class[ScalaNativeLinkTestTask] = classOf[ScalaNativeLinkTestTask]
  override def runTaskClass     : Class[ScalaNativeRunMainTask ] = classOf[ScalaNativeRunMainTask ]
  override def testTaskClass    : Class[ScalaNativeTestTask    ] = classOf[ScalaNativeTestTask    ]

  override def backendKind: ScalaBackendKind.NonJvm = ScalaBackendKind.Native
  override def pluginDependenciesConfigurationName: String = "scalanative"
  override def createExtension: Option[CreateExtension[?]] = None
  override def areCompilerPluginsBuiltIntoScala3: Boolean = false
  override def junit4: ScalaDependency.Maker = JUnit4ScalaNative
  override def scalaCompileParameters(isScala3: Boolean): Seq[String] = Seq.empty
  override def versionExtractor(version: Version): Version = version.compound.right
  override def versionComposer(projectScalaVersion: Version, backendVersion: Version): Version =
    projectScalaVersion.compound(backendVersion)
  
  private val group: String = "org.scala-native"
  
  private sealed class Maker(
    final override val artifact: String,
    what: String,
    final override val isScalaVersionFull: Boolean = false
  ) extends ScalaDependency.Maker:
    final override def description: String = describe(what)
    final override def versionDefault: Version = ScalaNative.backendKind.versionDefault
    final override def group: String = ScalaNative.group

  override def implementation: Seq[ScalaDependency.Maker] = Seq(
    Maker("nativelib" , "Native Library" ),
    Maker("clib"      , "C Library"      ),
    Maker("posixlib"  , "Posix Library"  ),
    Maker("windowslib", "Windows Library"),
    Maker("javalib"   , "Java Library"   ),
    Maker("auxlib"    , "Aux Library"    )
  )

  override def linker: ScalaDependency.Maker = Maker("tools", "Build Tools, including Linker")
  override def testBridge: ScalaDependency.Maker = Maker("test-interface", "SBT Test Interface")
  override def testAdapter: ScalaDependency.Maker = new Maker("test-runner", "Test Runner") with ScalaDependency.MakerJvm

  override def library(isScala3: Boolean): ScalaDependency.Maker =
    if isScala3
    then new Maker("scala3lib", "Scala 3 Library")
    else new Maker("scalalib", "Scala 2 Library") with ScalaDependency.MakerScala2

  override def compiler: ScalaDependency.Maker = new Maker(
    "nscplugin",
    "Compiler Plugin",
    isScalaVersionFull = true
  ) with ScalaDependency.MakerJvm

  override def junit4Plugin: ScalaDependency.Maker = new Maker(
    "junit-plugin",
    "JUnit4 Compiler Plugin for generating bootstrappers",
    isScalaVersionFull = true
  ) with ScalaDependency.MakerJvm

  override def additionalPluginDependencyRequirements: Seq[DependencyRequirement[ScalaPlatform]] = Seq.empty

  override def additionalImplementationDependencyRequirements(
    backendVersion: Version,
    scalaVersion: Version,
    isScala3: Boolean
  ): Seq[DependencyRequirement[ScalaPlatform]] = Seq.empty

// TODO exclusions?
  // // Exclude cross published version dependencies leading to conflicts in Scala 3 vs 2.13
  // // When using Scala 3 exclude Scala 2.13 standard native libraries,
  // // when using Scala 2.13 exclude Scala 3 standard native libraries
  // // Use full name, Maven style published artifacts cannot use artifact/cross version for exclusion rules
  // nativeStandardLibraries.map { lib =>
  //   val scalaBinVersion =
  //     if (scalaVersion.value.startsWith("3.")) "2.13"
  //     else "3"
  //   ExclusionRule()
  //     .withOrganization(organization)
  //     .withName(s"${lib}_native${ScalaNativeCrossVersion.currentBinaryVersion}_${scalaBinVersion}")
