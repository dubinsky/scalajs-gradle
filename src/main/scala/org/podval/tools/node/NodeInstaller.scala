package org.podval.tools.node

import org.podval.tools.build.{Dependency, Installer, Version}
import org.podval.tools.util.Strings
import java.io.File
import java.nio.file.{Files, Path, Paths}

// Describes Node distribution's packaging and structure.
object NodeInstaller extends Installer[Node]:
  override def versionDefault: Version = Version("25.6.1")
  override def group: String = "org.nodejs"
  override def artifact: String = "node"
  override def backendSuffix: Option[String] = None
  override def name: String = "Node.js"
  override def extension(version: Version): Option[String] = Some(if isZip(version) then "zip" else "tar.gz")
  
  override def classifier(version: Version): Option[String] =
    val fixUpOsAndArch: Boolean = isWindows && !hasWindowsZip(version)
    val dependencyOsName: String = if fixUpOsAndArch then "linux" else osName
    val dependencyOsArch: String = if fixUpOsAndArch then "x86"   else osArch
    Some(s"$dependencyOsName-$dependencyOsArch")

  override def repository: Option[Dependency.Repository] = Some(Dependency.Repository(
    url = "https://nodejs.org/dist",
    artifactPattern = "v[revision]/[artifact](-v[revision]-[classifier]).[ext]",
    ivy = "v[revision]/ivy.xml"
  ))

  override def cacheDirectory: String = "nodejs"

  private val os: Os = Os.get
  private val isWindows: Boolean = os == Os.Windows

  private val osName: String = os match
    case Os.Windows => "win"
    case Os.Mac     => "darwin"
    case Os.Linux   => "linux"
    case Os.FreeBSD => "linux"
    case Os.SunOS   => "sunos"
    case Os.Aix     => "aix"
    case _ => throw IllegalArgumentException(s"Unsupported OS: $os")

  // Gradle Node plugin's code claims that Java returns "arm" on all ARM variants.
  private val osArch: String =
    val name: String =
      if Os.get.hasUname
      then Exec("uname -m")
      else System.getProperty("os.arch")
      
    name.toLowerCase match
      case "x86_64"  => "x64"
      case "amd64"   => "x64"
      case "aarch64" => "x64"
      case "ppc64"   => "ppc64"
      case "ppc64le" => "ppc64le"
      case "s390x"   => "s390x"
      case "i686"    => "x86"
      case "nacl"    => "x86"
      case "armv6l"  => "armv6l"
      case "armv7l"  => "armv7l"
      case "armv8l"  => "arm64" // *not* "armv8l"!
      case _ => throw IllegalArgumentException(s"Unsupported architecture: $name")

  //https://github.com/nodejs/node/pull/5995
  private def hasWindowsZip(version: Version): Boolean =
    val major: Int = version.int(0)
    val minor: Int = version.int(1)
    val patch: Int = version.int(2)

    ((major == 4) && (minor >= 5)) || // >= 4.5.0..6
    ((major == 6) && ((minor > 2) || ((patch == 2) && (patch >= 1)))) || // >= 6.2.1..7
     (major >  6) // 7..
  
  override def isZip(version: Version): Boolean = isWindows && hasWindowsZip(version)
  
  override def archiveSubdirectoryPath(version: Version): Seq[String] = Seq(
    s"$artifact-v$version${Strings.prefix("-", classifier(version))}"
  )

  override def installation(root: File): Node =
    val bin: File = if !isWindows then File(root, "bin") else root

    Node(
      node = File(bin, if isWindows then "node.exe" else "node"),
      npm  = File(bin, if isWindows then "npm.cmd"  else "npm" )
    )

  override def exists(node: Node): Boolean =
    node.node.exists && node.npm.exists

  override def fixup(node: Node): Unit = if !isWindows then
    val npm: Path = node.npm.toPath
    val deleted: Boolean = Files.deleteIfExists(npm)
    if deleted then
      val npmCliJs: String = File(node.root, s"lib/node_modules/npm/bin/npm-cli.js").getAbsolutePath
      Files.createSymbolicLink(
        npm,
        npm.getParent.relativize(Paths.get(npmCliJs))
      )

  override def fromOs: Option[Node] = if Os.get == Os.Windows then None else
    for
      node <- Exec.which("node")
      npm  <- Exec.which("npm")
    yield Node(
      node,
      npm
    )
