package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Action, Project, Task}
import org.gradle.api.artifacts.{ConfigurablePublishArtifact, Configuration}
import org.gradle.api.file.{ConfigurableFileCollection, RegularFile}
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.{ScalaSourceDirectorySet, SourceSet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaDoc
import org.gradle.language.scala.tasks.AbstractScalaCompile

// Adopted from org.gradle.api.plugins.scala.ScalaPlugin.
object ScalaPluginAsLibrary:
  // Adopted from org.gradle.api.plugins.scala.ScalaPlugin.
  def configureIncrementalAnalysisElements(
    project: Project,
    compileTaskName: String,
    incrementalScalaAnalysisElementsConfigurationName: String
  ): Unit =
    // TODO and for the test source set?
    val incrementalAnalysisElements: Configuration = project.getConfigurations.getByName(incrementalScalaAnalysisElementsConfigurationName)
    val compileScalaMapping: Provider[RegularFile] = project.getLayout.getBuildDirectory.file(s"tmp/scala/compilerAnalysis/$compileTaskName.mapping")
    val compileScala: TaskProvider[AbstractScalaCompile] = project.getTasks.withType(classOf[AbstractScalaCompile]).named(compileTaskName)
    compileScala.configure(_.getAnalysisMappingFile.set(compileScalaMapping))
    incrementalAnalysisElements.getOutgoing.artifact(
      compileScalaMapping,
      new Action[ConfigurablePublishArtifact]:
        override def execute(configurablePublishArtifact: ConfigurablePublishArtifact): Unit = configurablePublishArtifact.builtBy(compileScala)
    )

  def configureScaladoc(
    project: Project,
    isRegisterNotReplaceScaladocTask: Boolean,
    sourceSet: SourceSet,
    backendDisplayName: String,
    scalaDocTaskName: String
  ): Unit =
    project.getTasks.withType(classOf[ScalaDoc]).configureEach(scalaDoc =>
      // only for this backend
      if scalaDoc.getName == scalaDocTaskName then
        scalaDoc.getConventionMapping.map("classpath", () =>
          val files: ConfigurableFileCollection = project.files()
          files.from(sourceSet.getOutput)
          files.from(sourceSet.getCompileClasspath)
          files
        )
        scalaDoc.setSource(sourceSet.getExtensions.getByType(classOf[ScalaSourceDirectorySet]))
        scalaDoc.getCompilationOutputs.from(sourceSet.getOutput)
    )
    
    val scalaDocTaskConfigureAction: Action[Task] = (scalaDoc: Task) =>
      scalaDoc.setDescription(s"Generates Scaladoc for the $backendDisplayName main source code.")
      scalaDoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)

    if isRegisterNotReplaceScaladocTask then project.getTasks.register(
      scalaDocTaskName,
      classOf[ScalaDoc],
      scalaDocTaskConfigureAction
    ) else project.getTasks
      .named(scalaDocTaskName)
      .configure(scalaDocTaskConfigureAction)
    