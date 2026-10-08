package org.podval.tools.build

import org.podval.tools.jvm.JvmBackend
import org.podval.tools.scalajs.ScalaJSBackend
import org.podval.tools.scalanative.ScalaNativeBackend
import org.podval.tools.test.testproject.{Fragments, TestProject}
import org.podval.tools.util.Files
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.prop.{TableDrivenPropertyChecks, TableFor3, TableFor5}
import java.io.File

class ArtifactSuffixTest extends AnyFlatSpec, Matchers, TableDrivenPropertyChecks:
  private val version: String = "1.2.3"
  private val scala3: ScalaVersion.Known = ScalaBinaryVersion.Scala3WithScala3Library.scalaVersionDefault
  private val scala213: ScalaVersion.Known = ScalaBinaryVersion.Scala2_13.scalaVersionDefault
  private val scala212: ScalaVersion.Known = ScalaBinaryVersion.Scala2_12.scalaVersionDefault

  private val suffixes: TableFor3[Backend, ScalaVersion.Known, String] = Table(
    ("backend", "scala", "expected"),
    (JvmBackend, scala3, "_3"),
    (JvmBackend, scala213, "_2.13"),
    (JvmBackend, scala212, "_2.12"),
    (ScalaJSBackend, scala3, "_sjs1_3"),
    (ScalaJSBackend, scala213, "_sjs1_2.13"),
    (ScalaJSBackend, scala212, "_sjs1_2.12"),
    (ScalaNativeBackend, scala3, "_native0.5_3"),
    (ScalaNativeBackend, scala213, "_native0.5_2.13"),
    (ScalaNativeBackend, scala212, "_native0.5_2.12"),
    (JvmBackend, ScalaVersion(Version("3.8.0-RC1")), "_3.8.0-RC1"),
    (ScalaJSBackend, ScalaVersion(Version("3.10.0-RC1")), "_sjs1_3.10.0-RC1"),
    (ScalaNativeBackend, ScalaVersion(Version("2.13.0-RC1")), "_native0.5_2.13.0-RC1")
  )

  "Artifact.suffix" should "follow the backend and the Scala binary version" in :
    forAll(suffixes): (backend: Backend, scalaVersion: ScalaVersion.Known, expected: String) =>
      Artifact.suffix(backend, ScalaLibrary.fromScalaVersion(scalaVersion)) shouldBe expected

  private val mainJars: TableFor5[String, Backend, ScalaVersion.Known, Boolean, String] = Table(
    ("caseName", "backend", "scala", "turnSuffixOff", "jarFile"),
    ("jvm-scala3-default", JvmBackend, scala3, false, "suffixlib_3-1.2.3.jar"),
    ("jvm-scala3-off", JvmBackend, scala3, true, "suffixlib-1.2.3.jar"),
    ("jvm-scala213-default", JvmBackend, scala213, false, "suffixlib_2.13-1.2.3.jar"),
    ("js-scala3-default", ScalaJSBackend, scala3, false, "suffixlib_sjs1_3-1.2.3.jar"),
    ("js-scala3-off", ScalaJSBackend, scala3, true, "suffixlib-1.2.3.jar"),
    ("native-scala3-default", ScalaNativeBackend, scala3, false, "suffixlib_native0.5_3-1.2.3.jar")
  )

  "jars, the POM, and module metadata" should "follow useArtifactSuffix" in :
    forAll(mainJars): (
      caseName: String,
      backend: Backend,
      scalaVersion: ScalaVersion.Known,
      turnSuffixOff: Boolean,
      jarFile: String
    ) =>
      val artifactId: String = jarFile.stripSuffix(s"-$version.jar")
      val sourcesFile: String = s"$artifactId-$version-sources.jar"
      val project: TestProject = writeProject(
        caseName = caseName,
        backend = backend,
        scalaVersion = scalaVersion,
        turnSuffixOff = turnSuffixOff,
        extraPluginIds = Seq("maven-publish"),
        extraBuild = Seq(libraryPublishing(custom = false))
      )

      project.build(
        "clean",
        "jar",
        "sourcesJar",
        "generatePomFileForLibraryPublication",
        "generateMetadataFileForLibraryPublication"
      )

      assertLibs(project, jarFile, sourcesFile)
      read(project, "build/publications/library/pom-default.xml") should include(
        s"<artifactId>$artifactId</artifactId>"
      )
      val module: String = read(project, "build/publications/library/module.json")
      module should include(s""""module": "$artifactId"""")
      module should include(s""""name": "$jarFile"""")
      module should include(s""""name": "$sourcesFile"""")

  "a custom artifactId" should "not gain the suffix" in :
    val artifactId: String = "suffixlib_3"
    val project: TestProject = writeProject(
      caseName = "jvm-scala3-custom",
      backend = JvmBackend,
      scalaVersion = scala3,
      turnSuffixOff = false,
      extraPluginIds = Seq("maven-publish"),
      extraBuild = Seq(libraryPublishing(custom = true))
    )

    project.build("clean", "jar", "generatePomFileForCustomPublication")

    File(project.projectDir, s"build/libs/$artifactId-$version.jar").isFile shouldBe true
    read(project, "build/publications/custom/pom-default.xml") should include(
      "<artifactId>custom</artifactId>"
    )

  private val pluginPublications: TableFor3[String, Boolean, String] = Table(
    ("caseName", "turnSuffixOff", "artifactId"),
    ("gradle-plugin-default", false, "suffixlib_3"),
    ("gradle-plugin-off", true, "suffixlib")
  )

  "Gradle plugin publications" should "suffix pluginMaven and leave the marker id alone" in :
    forAll(pluginPublications): (caseName: String, turnSuffixOff: Boolean, artifactId: String) =>
      val project: TestProject = writeProject(
        caseName = caseName,
        backend = JvmBackend,
        scalaVersion = scala3,
        turnSuffixOff = turnSuffixOff,
        extraPluginIds = Seq("java-gradle-plugin", "maven-publish"),
        extraBuild = Seq(
          """gradlePlugin {
            |  plugins {
            |    dummy {
            |      id = 'org.example.dummy'
            |      implementationClass = 'org.gradle.api.plugins.JavaLibraryPlugin'
            |    }
            |  }
            |}
            |""".stripMargin
        )
      )

      project.build(
        "clean",
        "generatePomFileForPluginMavenPublication",
        "generatePomFileForDummyPluginMarkerMavenPublication"
      )

      val marker: String = read(project, "build/publications/dummyPluginMarkerMaven/pom-default.xml")
      read(project, "build/publications/pluginMaven/pom-default.xml") should include(
        s"<artifactId>$artifactId</artifactId>"
      )
      marker should include("<artifactId>org.example.dummy.gradle.plugin</artifactId>")
      marker should include(s"<artifactId>$artifactId</artifactId>")

  "a mixed project" should "inherit useArtifactSuffix and let one backend override it" in :
    val project: TestProject = TestProject(Seq("artifact-suffix", "mixed-inherit"))
    val writer = project.writer(None)
    writer.writeSettings(Seq(
      Fragments.settingsManagement,
      Fragments.rootProjectName("suffixmix"),
      Fragments.includeScalaJsPluginBuild,
      "include 'js'",
      "include 'jvm'"
    ))
    writer.writeBuild(Seq(
      Fragments.applyScalaJsPlugin,
      Fragments.scalaVersion(scala3),
      "group = 'org.example'",
      s"version = '$version'",
      """subprojects {
        |  group = 'org.example'
        |  version = rootProject.version
        |}
        |scalaBackend {
        |  useArtifactSuffix = false
        |}
        |""".stripMargin
    ))
    val flags: String =
      """tasks.register('flags') {
        |  doLast {
        |    println("FLAGS jvm=${scalaBackend.jvm} js=${scalaBackend.js} native=${scalaBackend.native}")
        |  }
        |}
        |""".stripMargin
    val childPlugins: String =
      """plugins {
        |  id 'maven-publish'
        |}
        |""".stripMargin
    val publishing: String = childPlugins + libraryPublishing(custom = false) + flags
    Files.write(File(project.projectDir, "js/build.gradle"), publishing)
    Files.write(
      File(project.projectDir, "jvm/build.gradle"),
      childPlugins +
        """scalaBackend {
          |  useArtifactSuffix = true
          |}
          |""".stripMargin +
        libraryPublishing(custom = false) +
        flags
    )

    val output: String = project.build(
      ":js:jar",
      ":js:sourcesJar",
      ":js:generatePomFileForLibraryPublication",
      ":js:flags",
      ":jvm:jar",
      ":jvm:generatePomFileForLibraryPublication",
      ":jvm:flags"
    )

    assertLibsAt(project, "js", "js-1.2.3.jar", "js-1.2.3-sources.jar")
    assertLibsAt(project, "jvm", "jvm_3-1.2.3.jar")
    read(project, "js/build/publications/library/pom-default.xml") should include("<artifactId>js</artifactId>")
    read(project, "jvm/build/publications/library/pom-default.xml") should include("<artifactId>jvm_3</artifactId>")
    output should include("FLAGS jvm=false js=true native=false")
    output should include("FLAGS jvm=true js=false native=false")

  // archiveFile.name is asserted, not only classpath containment.
  // A flag read in the plugin's first afterEvaluate finalizes the default true,
  // so both sides then use the suffixed name and a contains-check still passes.
  private val downstreamCompiles = Table(
    ("caseName", "backend", "producerFlag", "expectedJar"),
    ("js-default", ScalaJSBackend, "", s"producer_sjs1_3-$version.jar"),
    ("jvm-default", JvmBackend, "", s"producer_3-$version.jar"),
    (
      "js-off",
      ScalaJSBackend,
      """scalaBackend {
        |  useArtifactSuffix = false
        |}
        |""".stripMargin,
      s"producer-$version.jar"
    ),
    (
      "js-after-evaluate",
      ScalaJSBackend,
      """afterEvaluate {
        |  scalaBackend.useArtifactSuffix = false
        |}
        |""".stripMargin,
      s"producer-$version.jar"
    )
  )

  "a downstream project" should "compile against the jar the producer writes" in :
    forAll(downstreamCompiles): (
      caseName: String,
      backend: Backend,
      producerFlag: String,
      expectedJar: String
    ) =>
      val project: TestProject = TestProject(Seq("artifact-suffix", "downstream", caseName))
      val writer = project.writer(None)
      writer.writeSettings(Seq(
        Fragments.settingsManagement,
        Fragments.rootProjectName("suffixdep"),
        Fragments.includeScalaJsPluginBuild,
        "include 'producer'",
        "include 'consumer'"
      ))
      writer.writeBuild(Seq(
        s"""subprojects {
           |  group = 'org.example'
           |  version = '$version'
           |}
           |""".stripMargin
      ))
      writeFile(
        project.projectDir,
        "producer/build.gradle",
        s"""${plugins()}
           |${Fragments.scalaVersion(scala3)}
           |$producerFlag
           |""".stripMargin
      )
      writeFile(
        project.projectDir,
        "consumer/build.gradle",
        s"""${plugins()}
           |${Fragments.scalaVersion(scala3)}
           |dependencies {
           |  implementation project(':producer')
           |}
           |def expectedName = '$expectedJar'
           |tasks.withType(org.gradle.api.tasks.scala.ScalaCompile).configureEach {
           |  doFirst {
           |    def archive = rootProject.project(':producer').tasks.named('jar', org.gradle.jvm.tasks.Jar).get().archiveFile.get().asFile
           |    if (archive.name != expectedName) {
           |      throw new GradleException("jar.archiveFile " + archive.name + " != " + expectedName)
           |    }
           |    if (!classpath.files.contains(archive)) {
           |      throw new GradleException("classpath " + classpath.files*.name + " missing " + archive)
           |    }
           |  }
           |}
           |""".stripMargin
      )
      writeFile(project.projectDir, "producer/gradle.properties", s"${Backend.property}=${backend.name}\n")
      writeFile(project.projectDir, "consumer/gradle.properties", s"${Backend.property}=${backend.name}\n")
      writeFile(
        project.projectDir,
        "producer/src/main/scala/lib/Lib.scala",
        """package lib
          |object Lib:
          |  def marker: Int = 1
          |""".stripMargin
      )
      writeFile(
        project.projectDir,
        "consumer/src/main/scala/app/App.scala",
        """package app
          |object App:
          |  def use: Int = lib.Lib.marker
          |""".stripMargin
      )

      project.build(":consumer:compileScala")

  "a mixed project" should "let downstream projects compile against each backend jar" in :
    val project: TestProject = TestProject(Seq("artifact-suffix", "mixed-downstream"))
    val writer = project.writer(None)
    writer.writeSettings(Seq(
      Fragments.settingsManagement,
      Fragments.rootProjectName("suffixmixdown"),
      Fragments.includeScalaJsPluginBuild,
      "include 'mix'",
      "include 'mix:js'",
      "include 'mix:jvm'",
      "include 'consumer-js'",
      "include 'consumer-jvm'"
    ))
    writer.writeBuild(Seq())
    writeFile(
      project.projectDir,
      "mix/build.gradle",
      s"""${plugins()}
         |${Fragments.scalaVersion(scala3)}
         |group = 'org.example'
         |version = '$version'
         |subprojects {
         |  group = 'org.example'
         |  version = '$version'
         |}
         |scalaBackend {
         |  useArtifactSuffix = false
         |}
         |""".stripMargin
    )
    writeFile(project.projectDir, "mix/js/build.gradle", "")
    writeFile(
      project.projectDir,
      "mix/jvm/build.gradle",
      """scalaBackend {
        |  useArtifactSuffix = true
        |}
        |""".stripMargin
    )
    writeDownstream(
      project.projectDir,
      "consumer-js",
      ScalaJSBackend,
      ":mix:js",
      s"js-$version.jar"
    )
    writeDownstream(
      project.projectDir,
      "consumer-jvm",
      JvmBackend,
      ":mix:jvm",
      s"jvm_3-$version.jar"
    )
    writeFile(
      project.projectDir,
      "mix/js/src/main/scala/lib/Lib.scala",
      """package lib
        |object Lib:
        |  def marker: Int = 1
        |""".stripMargin
    )
    writeFile(
      project.projectDir,
      "mix/jvm/src/main/scala/lib/Lib.scala",
      """package lib
        |object Lib:
        |  def marker: Int = 1
        |""".stripMargin
    )

    project.build(":consumer-js:compileScala", ":consumer-jvm:compileScala")

  private def writeDownstream(
    root: File,
    projectName: String,
    backend: Backend,
    producerPath: String,
    expectedJar: String
  ): Unit =
    writeFile(root, s"$projectName/gradle.properties", s"${Backend.property}=${backend.name}\n")
    writeFile(
      root,
      s"$projectName/build.gradle",
      s"""${plugins()}
         |${Fragments.scalaVersion(scala3)}
         |dependencies {
         |  implementation project('$producerPath')
         |}
         |def expectedName = '$expectedJar'
         |tasks.withType(org.gradle.api.tasks.scala.ScalaCompile).configureEach {
         |  doFirst {
         |    def archive = rootProject.project('$producerPath').tasks.named('jar', org.gradle.jvm.tasks.Jar).get().archiveFile.get().asFile
         |    if (archive.name != expectedName) {
         |      throw new GradleException("jar.archiveFile " + archive.name + " != " + expectedName)
         |    }
         |    if (!classpath.files.contains(archive)) {
         |      throw new GradleException("classpath " + classpath.files*.name + " missing " + archive)
         |    }
         |  }
         |}
         |""".stripMargin
    )
    writeFile(
      root,
      s"$projectName/src/main/scala/app/App.scala",
      """package app
        |object App:
        |  def use: Int = lib.Lib.marker
        |""".stripMargin
    )

  private def writeFile(dir: File, path: String, content: String): Unit =
    Files.write(File(dir, path), content)

  private def writeProject(
    caseName: String,
    backend: Backend,
    scalaVersion: ScalaVersion.Known,
    turnSuffixOff: Boolean,
    extraPluginIds: Seq[String],
    extraBuild: Seq[String]
  ): TestProject =
    val project: TestProject = TestProject(Seq("artifact-suffix", caseName))
    val writer = project.writer(None)

    writer.writeSettings(Seq(
      Fragments.settingsManagement,
      Fragments.rootProjectName("suffixlib"),
      Fragments.includeScalaJsPluginBuild
    ))

    val flag: String =
      if !turnSuffixOff then ""
      else
        """scalaBackend {
          |  useArtifactSuffix = false
          |}
          |""".stripMargin

    writer.writeBuild(Seq(
      plugins(extraPluginIds*),
      Fragments.scalaVersion(scalaVersion),
      "group = 'org.example'",
      s"version = '$version'",
      flag
    ) ++ extraBuild)

    writer.writeProperties(Seq(Backend.property -> backend.name))
    project

  private def plugins(extraIds: String*): String =
    val added: String = extraIds.map(id => s"  id '$id'").mkString("\n")
    val insertion: String = if added.isEmpty then "" else added + "\n"
    Fragments.applyScalaJsPlugin.replace("}\n", s"$insertion}\n")

  private def libraryPublishing(custom: Boolean): String =
    val customPublication: String =
      if !custom then ""
      else
        """    custom(MavenPublication) {
          |      from components.java
          |      artifactId = 'custom'
          |    }
          |""".stripMargin

    s"""publishing {
       |  publications {
       |    library(MavenPublication) {
       |      from components.java
       |    }
       |$customPublication
       |  }
       |}
       |java.withSourcesJar()
       |""".stripMargin

  private def assertLibs(project: TestProject, files: String*): Unit =
    assertLibsAt(project, ".", files*)

  private def assertLibsAt(project: TestProject, projectPath: String, files: String*): Unit =
    val libs: File = File(project.projectDir, s"$projectPath/build/libs")
    Option(libs.list()).map(_.toSeq.sorted).getOrElse(Seq.empty) shouldBe files.sorted

  private def read(project: TestProject, path: String): String =
    Files.read(File(project.projectDir, path)).mkString("\n")
