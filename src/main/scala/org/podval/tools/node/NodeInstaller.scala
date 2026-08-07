package org.podval.tools.node

import org.podval.tools.build.{Dependency, Installer, Version}
import org.podval.tools.util.Strings
import java.io.File
import java.nio.file.{Files, Path, Paths}

// Describes Node distribution's packaging and structure.
object NodeInstaller extends Installer[Node]:
  private val os: Os = Os.get
  private val isWindows: Boolean = os == Os.Windows

  override def versionDefault: Version = Version("26.7.0")
  override def group: String = "org.nodejs"
  override def artifact: String = "node"
  override def backendSuffix: Option[String] = None
  override def name: String = "Node.js"
  override def extension: Option[String] = Some(if isWindows then "zip" else "tar.gz")
  override def cacheDirectory: String = "nodejs"

  override def repository: Option[Dependency.Repository] = Some(Dependency.Repository(
    url = "https://nodejs.org/dist",
    artifactPattern = "v[revision]/[artifact](-v[revision]-[classifier]).[ext]",
    ivy = "v[revision]/ivy.xml"
  ))

  override val classifier: Option[String] =
    val osName: String = os match
      case Os.Windows => "win"
      case Os.Mac     => "darwin"
      case Os.Linux   => "linux"
      case Os.FreeBSD => "linux"
      case Os.SunOS   => "sunos"
      case Os.Aix     => "aix"
      case _ => throw IllegalArgumentException(s"Unsupported OS: $os")

    // Gradle Node plugin's code claims that Java returns "arm" on all ARM variants.
    val arch: String =
      if Os.get.hasUname
      then Exec("uname -m")
      else System.getProperty("os.arch")

    val osArch: String = arch.toLowerCase match
      case "x86_64"  => "x64"
      case "amd64"   => "x64"
      case "aarch64" => "arm64"
      case "ppc64"   => "ppc64"
      case "ppc64le" => "ppc64le"
      case "s390x"   => "s390x"
      case "i686"    => "x86"
      case "nacl"    => "x86"
      case "armv6l"  => "armv6l"
      case "armv7l"  => "armv7l"
      case "armv8l"  => "arm64" // *not* "armv8l"!
      case _ => throw IllegalArgumentException(s"Unsupported architecture: $name")

    Some(s"$osName-$osArch")

  override def archiveSubdirectoryPath(version: Version): Seq[String] = Seq(
    s"$artifact-v$version${Strings.prefix("-", classifier)}"
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
