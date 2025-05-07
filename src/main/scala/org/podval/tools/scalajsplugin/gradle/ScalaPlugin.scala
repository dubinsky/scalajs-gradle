package org.podval.tools.scalajsplugin.gradle

import org.gradle.api.{Action, Project, Task}
import org.gradle.api.artifacts.{ConfigurablePublishArtifact, Configuration}
import org.gradle.api.file.{ConfigurableFileCollection, RegularFile}
import org.gradle.api.plugins.internal.JavaPluginHelper
import org.gradle.api.plugins.jvm.internal.{JvmFeatureInternal, JvmPluginServices}
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.{ScalaSourceDirectorySet, TaskProvider}
import org.gradle.api.tasks.scala.ScalaDoc
import org.gradle.language.scala.tasks.AbstractScalaCompile
import org.podval.tools.scalajsplugin.GradleNames

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
// - JavaPlugin;
// - JavaBasePlugin.
//
// Actually, I am not sure that JavaBasePlugin is really needs to be replicated;
// on the other hand, I do not see where the "main" and "test" source sets are created,
// so it is possible that some DefaultJvmFeature, DefaultXXXContainer or DefaultXXXExtension also need to be replicated.
//
// To replicate whatever needs to be replicated, the corresponding Gradle code is copied and adjusted to:
// - create entities when needed and reconfigure existing ones otherwise;
// - not assume that the only source sets that exist are "main" and "test";
// - use appropriate names for source sets, configurations and tasks.
//
// It is extremely unfortunate that Gradle does not provide extension mechanisms and the code actually needs to be copied ;)

// see org.gradle.api.plugins.scala.ScalaPlugin
final class ScalaPlugin(
  isCreate: Boolean,
  sourceRoot: String,
  sharedSourceRoot: String,
  gradleNames: GradleNames,
  backendDisplayName: String,
  project: Project,
  jvmPluginServices: JvmPluginServices
):
  def apply(): Unit =
    ScalaBasePlugin(
      isCreate,
      sourceRoot,
      sharedSourceRoot,
      gradleNames,
      project,
      jvmPluginServices
    ).apply()

    val feature: JvmFeatureInternal = JavaPluginHelper.getJavaComponent(project).getFeatures.findByName(gradleNames.featureName)
    configureScaladoc(feature)

    val incrementalAnalysisElements: Configuration = project.getConfigurations.getByName(gradleNames.incrementalScalaAnalysisElementsConfigurationName)
    val compileTaskName: String = feature.getSourceSet.getCompileTaskName(gradleNames.scalaCompileTaskName)
    val compileScalaMapping: Provider[RegularFile] = project.getLayout.getBuildDirectory.file(s"tmp/scala/compilerAnalysis/$compileTaskName.mapping")
    val compileScala: TaskProvider[AbstractScalaCompile] = project.getTasks.withType(classOf[AbstractScalaCompile]).named(compileTaskName)
    compileScala.configure(_.getAnalysisMappingFile.set(compileScalaMapping))
    incrementalAnalysisElements.getOutgoing.artifact(
      compileScalaMapping,
      new Action[ConfigurablePublishArtifact]:
        override def execute(configurablePublishArtifact: ConfigurablePublishArtifact): Unit = configurablePublishArtifact.builtBy(compileScala)
    )

  private def configureScaladoc(feature: JvmFeatureInternal): Unit =
    project.getTasks.withType(classOf[ScalaDoc]).configureEach(scalaDoc =>
      // only for this backend
      if scalaDoc.getName == gradleNames.scalaDocTaskName then
        scalaDoc.getConventionMapping.map("classpath", () =>
          val files: ConfigurableFileCollection = project.files()
          files.from(feature.getSourceSet.getOutput)
          files.from(feature.getSourceSet.getCompileClasspath)
          files
        )
        scalaDoc.setSource(feature.getSourceSet.getExtensions.getByType(classOf[ScalaSourceDirectorySet]))
        scalaDoc.getCompilationOutputs.from(feature.getSourceSet.getOutput)
    )
    
    val scalaDocTaskConfigureAction: Action[Task] = (scalaDoc: Task) =>
      scalaDoc.setDescription(s"Generates Scaladoc for the $backendDisplayName main source code.")
      scalaDoc.setGroup(JavaBasePlugin.DOCUMENTATION_GROUP)
    if isCreate then project.getTasks.register(
      gradleNames.scalaDocTaskName,
      classOf[ScalaDoc],
      scalaDocTaskConfigureAction
    ) else project.getTasks
      .named(gradleNames.scalaDocTaskName)
      .configure(scalaDocTaskConfigureAction)
    