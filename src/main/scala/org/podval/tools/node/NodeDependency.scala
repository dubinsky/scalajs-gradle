package org.podval.tools.node

import org.podval.tools.build.{InstallableDependency, Repository, SimpleDependency, Version}
import org.podval.tools.platform.{Architecture, Exec, Os}
import org.podval.tools.util.Strings
import java.io.File
import java.nio.file.{Files, Path, Paths}

// Describes Node distribution's packaging and structure.
object NodeDependency extends SimpleDependency[NodeDependency.type](
  group = "org.nodejs",
  artifact = "node"
) with InstallableDependency[NodeInstallation]:

  // Anything later than that breaks ScalaJS: 17.9.1, 18.15.0, 19.8.1
  val versionDefault: Version = Version("16.19.1")

  override def cacheDirectory: String = "nodejs"

  override def repository: Option[Repository] = Some(Repository(
    url = "https://nodejs.org/dist",
    artifactPattern = "v[revision]/[artifact](-v[revision]-[classifier]).[ext]",
    ivy = "v[revision]/ivy.xml"
  ))

  private val os: Os = Os.get
  private val isWindows: Boolean = os == Os.Windows
  private val architecture: Architecture = Architecture.get
  
  private val osName: String = os match
    case Os.Windows => "win"
    case Os.Mac     => "darwin"
    case Os.Linux   => "linux"
    case Os.FreeBSD => "linux"
    case Os.SunOS   => "sunos"
    case Os.Aix     => "aix"
    case _ => throw IllegalArgumentException(s"Unsupported OS: $os")

  private val osArch: String = architecture match
    case Architecture.x86_64  => "x64"
    case Architecture.amd64   => "x64"
    case Architecture.aarch64 => "x64"
    case Architecture.ppc64   => "ppc64"
    case Architecture.ppc64le => "ppc64le"
    case Architecture.s390x   => "s390x"
    case Architecture.armv6l  => "armv6l"
    case Architecture.armv7l  => "armv7l"
    case Architecture.armv8l  => "arm64" // *not* "armv8l"!
    case Architecture.i686    => "x86"
    case Architecture.nacl    => "x86"

  //https://github.com/nodejs/node/pull/5995
  private def hasWindowsZip(version: Version): Boolean =
    val (majorVersion: Int, minorVersion: Int, microVersion: Int) = version.majorMinorMicro

    ((majorVersion == 4) && (minorVersion >= 5)) || // >= 4.5.0..6
    ((majorVersion == 6) && ((minorVersion > 2) || ((minorVersion == 2) && (microVersion >= 1)))) || // >= 6.2.1..7
     (majorVersion > 6) // 7..

  override def classifier(version: Version): Option[String] =
    val fixUpOsAndArch: Boolean = isWindows && !hasWindowsZip(version)
    val dependencyOsName: String = if fixUpOsAndArch then "linux" else osName
    val dependencyOsArch: String = if fixUpOsAndArch then "x86"   else osArch
    Some(s"$dependencyOsName-$dependencyOsArch")

  override def isZip(version: Version): Boolean = isWindows && hasWindowsZip(version)

  override def extension(version: Version): Option[String] = Some(if isZip(version) then "zip" else "tar.gz")

  override def archiveSubdirectoryPath(version: Version): Seq[String] =
    val classifierStr: String = Strings.prefix("-", classifier(version))
    Seq(
      s"$artifact-v$version$classifierStr"
    )

  override def installation(root: File): NodeInstallation =
    val bin: File = if !isWindows then File(root, "bin") else root

    NodeInstallation(
      node = File(bin, if isWindows then "node.exe" else "node"),
      npm  = File(bin, if isWindows then "npm.cmd"  else "npm" )
    )

  override def exists(installation: NodeInstallation): Boolean =
    installation.node.exists && installation.npm.exists

  override def fixup(installation: NodeInstallation): Unit = if !isWindows then
    val npm: Path = installation.npm.toPath
    val deleted: Boolean = Files.deleteIfExists(npm)
    if deleted then
      val npmCliJs: String = File(installation.root, s"lib/node_modules/npm/bin/npm-cli.js").getAbsolutePath
      Files.createSymbolicLink(
        npm,
        npm.getParent.relativize(Paths.get(npmCliJs))
      )

  override def fromOs: Option[NodeInstallation] = if Os.get == Os.Windows then None else
    for
      node <- Exec.which("node")
      npm <- Exec.which("npm")
    yield NodeInstallation(
      node,
      npm
    )
