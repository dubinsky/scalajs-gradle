package org.podval.tools.build

import org.podval.tools.util.Files
import org.slf4j.{Logger, LoggerFactory}
import java.io.File

private object InstallableDependency:
  private val logger: Logger = LoggerFactory.getLogger(InstallableDependency.getClass)

trait InstallableDependency[T] extends Dependency:
  import InstallableDependency.logger
  
  def repository: Option[Repository] = None

  // Where retrieved distributions are cached
  def cacheDirectory: String = artifact

  def archiveSubdirectoryPath(version: Version): Seq[String] = Seq.empty

  // TODO use enumeration; determine from the file name
  def isZip(version: Version): Boolean = false

  def installation(root: File): T

  def exists(installation: T): Boolean

  def fixup(installation: T): Unit = ()

  def fromOs: Option[T] = None

  def versionDefault: Version
  
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
    ifDoesNotExist: (Dependency.WithVersion, T) => Unit
  ): T =
    def getInternal(version: Version) =      
      val dependencyWithVersion: Dependency.WithVersion = withVersion(version)
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
        logger.info(s"Needed dependency is not installed locally and no version to install is specified: $this; installing default version: $versionDefault")
        getInternal(versionDefault)
  
  private def installsInto(context: BuildContextCore, dependencyWithVersion: Dependency.WithVersion): File =
    Files.file(context.frameworks, cacheDirectory, dependencyWithVersion.fileName)

  private def install(
    context: BuildContext,
    dependencyWithVersion: Dependency.WithVersion,
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
