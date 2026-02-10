package org.podval.tools.backend

import org.gradle.api.{Action, Project, Task}
import org.gradle.api.file.SourceDirectorySet
import org.gradle.api.internal.file.DefaultSourceDirectorySet
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.plugins.jvm.internal.JvmPluginServices
import org.gradle.api.tasks.{AbstractCopyTask, SourceSet, SourceTask}
import org.podval.tools.build.Backend
import org.podval.tools.util.{Configurations, Reflection, Strings, Tasks}
import scala.jdk.CollectionConverters.ListHasAsScala
import java.io.File

final class SingleBackendProject(
  project: Project,
  jvmPluginServices: JvmPluginServices,
  backend: Backend,
  sharedProjects: Set[SharedProject]
) extends SingleProject(
  project
):
  override def announcement: String =
    val sharedString: String = if sharedProjects.isEmpty then "" else s" [+${Strings.toString(sharedProjects, _.name)}]"
    s"using Scala backend ${backend.name}$sharedString"

  private lazy val isRunningInIntelliJ: Boolean = IntelliJIdea.runningIn

  override def apply(): Unit =
    // Create extension.
    BackendExtension.create(project, backend, isRunningInIntelliJ)
    
    // Apply the backend.
    backend.apply(project, jvmPluginServices, isRunningInIntelliJ)
    backend.registerTasks(project)
  
  override def afterEvaluate(): Unit =
    sharedProjects.map(_.project).foreach(addSharedSources)

    val extension: BackendExtension = BackendExtension.get(project)
    
    backend.afterEvaluate(
      project,
      projectScalaLibrary = extension.getScalaLibrary,
      pluginScalaLibrary  = extension.getPluginScalaLibrary
    )

  private def addSharedSources(shared: Project): Unit =
    def add(): Unit = addSources(
      (
        sourceSetGetter: Project => SourceSet,
        _: String,
        directorySetGetter: SourceSet => SourceDirectorySet
      ) => Reflection
        .Get[java.util.List[Object], DefaultSourceDirectorySet]("source")(directorySetGetter(sourceSetGetter(shared)))
        .asScala
        .toSeq
        .filter(_.isInstanceOf[File])
        .map(_.asInstanceOf[File])
    )

    if !isRunningInIntelliJ then add() else
      // Add dependency on the shared sibling.
      Configurations.addDependency(project, Configurations.implementationName(project), shared)

      // Add shared sources before the execution of the tasks that need them and not at start
      // so that IntelliJ does not run into "duplicate content roots" issue during project import;
      // we do not bother to remove them after the execution of the task since
      // project import already happened and adding is done in idempotent way.
      Set(
        classOf[SourceTask], // compilation
        classOf[AbstractArchiveTask], // archives
        classOf[AbstractCopyTask] // resources
      ).foreach(Tasks.configureEach(
        project,
        _,
        // Note: task action below *must* be Action and not just lambda:
        _.doFirst(new Action[Task] {
          override def execute(task: Task): Unit = //noinspection ConvertExpressionToSAM
            // TODO remove Configurations.addDependency(project, Configurations.implementationName(project), shared)
            add()
        })
      ))
  