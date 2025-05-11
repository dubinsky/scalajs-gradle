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

// The idea here is:
// for JVM: re-configure existing setup
// ("main" and "test" source sets, associated configurations and tasks)
// so that both JVM-specific and shared code is included;
// for a non-JVM backend: create a parallel setup
// ("main<backend>" and "test<backend>" source sets, associated configurations and tasks).
//
// When Scala plugin gets applied originally, it applies a number of other plugins.
// Some of them do non-source-set-specific things which do not need to be replicated:
// - BasePlugin;
// - JvmEcosystemPlugin;
// - ReportingBasePlugin;
// - JvmToolchainsPlugin.
// Some of them do source-set-specific things which do need to be replicated:
// - ScalaPlugin;
// - ScalaBasePlugin;
// - JavaPlugin.
//
// To replicate whatever needs to be replicated, the corresponding Gradle code was copied and adjusted to:
// - not assume that the only source sets that exist are "main" and "test";
// - use appropriate names for source sets, configurations, and tasks.
//
// It would be much better if this functionality was exposed by Gradle in one method call.
// It would be even better if there was a way to affect the class of the test task created.
// Since the chances of this ever happening are zero, I made method signatures convenient for my needs ;)

// Adopted from org.gradle.api.plugins.scala.ScalaPlugin.
object ScalaPluginAsLibrary:
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
    isCreate: Boolean,
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

    if isCreate then project.getTasks.register(
      scalaDocTaskName,
      classOf[ScalaDoc],
      scalaDocTaskConfigureAction
    ) else project.getTasks
      .named(scalaDocTaskName)
      .configure(scalaDocTaskConfigureAction)
    