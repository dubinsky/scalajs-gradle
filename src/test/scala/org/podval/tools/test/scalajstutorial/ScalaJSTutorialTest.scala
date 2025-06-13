package org.podval.tools.test.scalajstutorial

import org.podval.tools.backend.scalajs.{JSEnvKind, ScalaJSBackend}
import org.podval.tools.build.ScalaBackend
import org.podval.tools.test.testproject.{Feature, Fixture, ForClass, GroupingFunSpec, TestProject}

class ScalaJSTutorialTest extends GroupingFunSpec:
  groupTestByFixtureAndCombined()
  
  override protected def features: Seq[Feature] = Seq(Feature("ScalaJS Tutorial"))
  override protected def fixtures: List[Fixture] = List(ScalaJSTutorialScalaTestFixture)
  override protected def backends: Set[ScalaBackend] = Set(ScalaJSBackend)
  override protected def checkRun: Boolean = true

  override protected def testTaskMore: Seq[String] = Seq(
    s"jsEnv = '${JSEnvKind.JSDOMNodeJS.name}'"
  )
  
  override protected def buildGradleFragments: Seq[String] = Seq(
    ScalaJSTutorialTest.linkTask(
      ScalaJSTutorialScalaTestFixture.mainSources.headOption.map(_.name)
    ),
    ScalaJSTutorialTest.runTask(JSEnvKind.JSDOMNodeJS)
  )

object ScalaJSTutorialTest:
  private def linkTask(mainClassName: Option[String]): String =
    s"""link {
       |  optimization     = 'Full'          // one of: 'Fast', 'Full'
       |  moduleKind       = 'NoModule'      // one of: 'NoModule', 'ESModule', 'CommonJSModule'
       |  moduleSplitStyle = 'FewestModules' // one of: 'FewestModules', 'SmallestModules'
       |  prettyPrint      = false
       |${mainClassName.fold("")(moduleInitializer)}
       |}
       |""".stripMargin

  private def moduleInitializer(mainClassName: String): String =
    s"""moduleInitializers {
       |    main {
       |      className = '${ForClass.testPackage}.$mainClassName'
       |      mainMethodName = 'main'
       |      mainMethodHasArgs = true
       |    }
       |  }
       |""".stripMargin

  private def runTask(jsEnvKind: JSEnvKind) =
    s"""run {
       |  jsEnv = '${jsEnvKind.name}'
       |}
       |""".stripMargin