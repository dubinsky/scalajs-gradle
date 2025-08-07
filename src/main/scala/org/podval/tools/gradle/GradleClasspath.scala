package org.podval.tools.gradle

import org.gradle.api.{GradleException, Project}
import org.gradle.internal.classloader.{ClassLoaderVisitor, ClasspathUtil, VisitableURLClassLoader}
import org.podval.tools.platform.Files
import scala.collection.mutable
import scala.jdk.CollectionConverters.{IterableHasAsJava, IterableHasAsScala}
import java.io.File
import java.net.{URL, URLClassLoader}

object GradleClasspath:
  def addTo(project: Project, configurationName: String): ClassLoader =
    addTo(Configurations.configuration(project, configurationName).asScala)

  def addTo(filesToAdd: Iterable[File]): ClassLoader =
    val urls: Iterable[URL] = filesToAdd.map(_.toURI.toURL)
    val result: ClassLoader = getClass.getClassLoader

    result match
      case visitable: VisitableURLClassLoader =>
        for url: URL <- urls do visitable.addURL(url)
      case classLoader =>
        ClasspathUtil.addUrl(
          classLoader.asInstanceOf[URLClassLoader],
          urls.asJava
        )

    result

  def findOn(name: String): URL =
    var result: Option[URL] = None
    val visitor: ClassLoaderVisitor = new ClassLoaderVisitor:
      override def visitClassPath(classPath: Array[URL]): Unit = classPath
        .find(_.getPath.contains(name))
        .foreach((url: URL) => result = Some(url))

    visitor.visit(getClass.getClassLoader)
    result.getOrElse(throw GradleException(s"Did not find artifact $name on the classpath"))

  def collect: Seq[File] =
    val result: mutable.ArrayBuffer[File] = mutable.ArrayBuffer.empty[File]
    val visitor: ClassLoaderVisitor = new ClassLoaderVisitor:
      override def visitClassPath(classPath: Array[URL]): Unit =
        for
          url: URL <- classPath
          if url.getProtocol == "file"
        do
          result += Files.url2file(url)
    visitor.visit(getClass.getClassLoader)
    result.toSeq
