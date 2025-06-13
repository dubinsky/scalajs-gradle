package org.podval.tools.build

import org.podval.tools.util.Files
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

private object DependencyInstallable:
  private val logger: Logger = LoggerFactory.getLogger(DependencyInstallable.getClass)

trait DependencyInstallable[T] extends Dependency:
  import DependencyInstallable.logger
  
  def repository: Option[Repository] = None

  // Where retrieved distributions are cached
  def cacheDirectory: String = maker.artifact

  def archiveSubdirectoryPath(version: PreVersion): Seq[String] = Seq.empty

  def isZip(version: PreVersion): Boolean = false

  def installation(root: File): T

  def exists(installation: T): Boolean

  def fixup(installation: T): Unit = ()

  def fromOs: Option[T] = None

  final def getInstalled(version: Option[String], context: BuildContextCore): T = get(
    version = version,
    context = context,
    ifDoesNotExist = (dependencyWithVersion, _) => context.fatalError(s"Needed dependency does not exist: $dependencyWithVersion")
  )

  final def getInstalledOrInstall(version: Option[String], context: BuildContext): T = get(
    version = version,
    context = context,
    ifDoesNotExist = (dependencyWithVersion, result) => install(context, dependencyWithVersion, result)
  )

  private def get(
    version: Option[String],
    context: BuildContextCore,
    ifDoesNotExist: (DependencyWithVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =
      val dependencyWithVersion: DependencyWithVersion = withVersion(version)
      val result: T = installation(
        root = Files.fileSeq(
          installsInto(context, dependencyWithVersion),
          archiveSubdirectoryPath(dependencyWithVersion.version)
        )
      )
  
      if exists(result)
      then logger.info(s"Existing $dependencyWithVersion detected: $result")
      else ifDoesNotExist(dependencyWithVersion, result)
      result
    
    version match
      case Some(version) => getInternal(Version(version))
      case None => fromOs.getOrElse:
        logger.info(s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: ${maker.versionDefault}")
        getInternal(maker.versionDefault)
  
  private def installsInto(context: BuildContextCore, dependencyWithVersion: DependencyWithVersion): File =
    Files.file(context.frameworks, cacheDirectory, dependencyWithVersion.fileName)

  private def install(
    context: BuildContext,
    dependencyWithVersion: DependencyWithVersion,
    result: T
  ): Unit =
    logger.warn(s"Installing $dependencyWithVersion as $result")

    val artifact: File = context.getArtifact(
      repository,
      dependencyWithVersion.dependencyNotation
    ).getOrElse(context.fatalError(s"No artifact found for: $dependencyWithVersion"))

    context.unpackArchive(
      file = artifact,
      isZip = isZip(dependencyWithVersion.version),
      into = installsInto(context, dependencyWithVersion)
    )

    if !exists(result) then context.fatalError(s"Does not exist after installation: $result")
    fixup(result)
