package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Action, Project}
import org.gradle.api.artifacts.{ConfigurablePublishArtifact, Configuration}
import org.gradle.api.file.{ConfigurableFileCollection, RegularFile}
import org.gradle.api.plugins.{JavaBasePlugin, JavaPlugin}
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.internal.JvmFeatureInternal
import org.gradle.api.plugins.scala.ScalaBasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.{ScalaSourceDirectorySet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaDoc
import org.gradle.language.scala.tasks.AbstractScalaCompile

// see org.gradle.api.plugins.scala.ScalaPlugin
import org.gradle.api.plugins.scala.ScalaPlugin as Original

abstract class ScalaPlugin:
  def apply(project: Project): Unit =
    project.getPluginManager.apply(classOf[ScalaBasePlugin])
    project.getPluginManager.apply(classOf[JavaPlugin])

    val mainFeature: JvmFeatureInternal = JavaPluginHelper.getJavaComponent(project).getMainFeature

    ScalaPlugin.configureScaladoc(project, mainFeature)

    val incrementalAnalysisElements: Configuration = project.getConfigurations.getByName("incrementalScalaAnalysisElements")
    val compileTaskName: String = mainFeature.getSourceSet.getCompileTaskName("scala")
    val compileScala: TaskProvider[AbstractScalaCompile] = project.getTasks.withType(classOf[AbstractScalaCompile]).named(compileTaskName)
    val compileScalaMapping: Provider[RegularFile] = project.getLayout.getBuildDirectory.file(s"tmp/scala/compilerAnalysis/$compileTaskName.mapping")
    compileScala.configure(task => task.getAnalysisMappingFile.set(compileScalaMapping))
    incrementalAnalysisElements.getOutgoing.artifact(
      compileScalaMapping,
      new Action[ConfigurablePublishArtifact]:
        override def execute(configurablePublishArtifact: ConfigurablePublishArtifact): Unit = configurablePublishArtifact.builtBy(compileScala)
    )

object ScalaPlugin:
  private def configureScaladoc(project: Project, feature: JvmFeatureInternal): Unit =
    project.getTasks.withType(classOf[ScalaDoc]).configureEach(scalaDoc =>
      scalaDoc.getConventionMapping.map("classpath", () =>
        val files: ConfigurableFileCollection = project.files()
        files.from(feature.getSourceSet.getOutput)
        files.from(feature.getSourceSet.getCompileClasspath)
        files
      )
      scalaDoc.setSource(feature.getSourceSet.getExtensions.getByType(classOf[ScalaSourceDirectorySet]))
      scalaDoc.getCompilationOutputs.from(feature.getSourceSet.getOutput)
    )
    project.getTasks.register(
      Original.SCALA_DOC_TASK_NAME, 
      classOf[ScalaDoc],
      new Action[ScalaDoc]:
        override def execute(scalaDoc: ScalaDoc): Unit =
          scalaDoc.setDescription("Generates Scaladoc for the main source code.")
          scalaDoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)
    )
    