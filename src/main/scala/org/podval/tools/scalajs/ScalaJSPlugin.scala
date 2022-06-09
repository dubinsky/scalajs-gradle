package org.podval.tools.scalajs

import org.gradle.api.{Plugin, Project}

final class ScalaJSPlugin extends Plugin[Project]:
  override def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[org.gradle.api.plugins.scala.ScalaPlugin])

    project.getExtensions.create("scalajs", classOf[Extension])

    project.getTasks.create("sjsLinkFastOpt"    , classOf[LinkTask.Main.FastOpt  ])
    project.getTasks.create("sjsLinkFullOpt"    , classOf[LinkTask.Main.FullOpt  ])
    project.getTasks.create("sjsLink"           , classOf[LinkTask.Main.Extension])

    project.getTasks.create("sjsLinkTestFastOpt", classOf[LinkTask.Test.FastOpt  ])
    project.getTasks.create("sjsLinkTestFullOpt", classOf[LinkTask.Test.FullOpt  ])
    project.getTasks.create("sjsLinkTest"       , classOf[LinkTask.Test.Extension])

    project.getTasks.create("sjsRunFastOpt"     , classOf[RunTask      .FastOpt  ])
    project.getTasks.create("sjsRunFullOpt"     , classOf[RunTask      .FullOpt  ])
    project.getTasks.create("sjsRun"            , classOf[RunTask      .Extension])

    project.getTasks.create("sjsTestFastOpt"    , classOf[TestTask     .FastOpt  ])
    project.getTasks.create("sjsTestFullOpt"    , classOf[TestTask     .FullOpt  ])
    project.getTasks.create("sjsTest"           , classOf[TestTask     .Extension])
