package org.podval.tools.scalajsplugin.scalanative

import org.gradle.api.Project
import org.podval.tools.build.{DependencyRequirement, ScalaPlatform}
import org.podval.tools.scalajsplugin.{AddTestTask, BackendDelegate, BackendDelegateKind}

final class ScalaNativeDelegate(
  project: Project,
  isModeMixed: Boolean
) extends BackendDelegate(
  project,
  isModeMixed
):
  override protected def kind: BackendDelegateKind = BackendDelegateKind.Native

  override protected def configurationToAddToClassPath: Option[String] = None

  override protected def setUpProject(): AddTestTask[ScalaNativeTestTask] = ???
  
  override protected def configureProject(isScala3: Boolean): Unit = ()
    // TODO disable compileJava task for the Scala Native sourceSet - unless Scala Native compiler deals with Java classes?

  override protected def dependencyRequirements(
    pluginScalaPlatform: ScalaPlatform,
    projectScalaPlatform: ScalaPlatform
  ): Seq[DependencyRequirement] = Seq.empty
