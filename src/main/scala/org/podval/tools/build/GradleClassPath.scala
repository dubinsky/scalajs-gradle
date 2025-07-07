package org.podval.tools.build

import org.gradle.api.GradleException
import org.gradle.internal.classloader.{ClassLoaderVisitor, ClasspathUtil, VisitableURLClassLoader}
import org.podval.tools.util.Files
import scala.collection.mutable
import scala.jdk.CollectionConverters.IterableHasAsJava
import java.io.File
import java.net.{URL, URLClassLoader}

object GradleClassPath:
  def addTo(obj: AnyRef, files: Iterable[File]): ClassLoader =
    val result: ClassLoader = obj.getClass.getClassLoader
    val urls: Iterable[URL] = files.map(_.toURI.toURL)

    result match
      case visitable: VisitableURLClassLoader =>
        for url: URL <- urls do visitable.addURL(url)
      case classLoader =>
        ClasspathUtil.addUrl(
          classLoader.asInstanceOf[URLClassLoader],
          urls.asJava
        )

    result

  def findOn(obj: AnyRef, name: String): URL =
    var result: Option[URL] = None
    val visitor: ClassLoaderVisitor = new ClassLoaderVisitor:
      override def visitClassPath(classPath: Array[URL]): Unit = classPath
        .find(_.getPath.contains(name))
        .foreach((url: URL) => result = Some(url))

    visitor.visit(obj.getClass.getClassLoader)
    result.getOrElse(throw GradleException(s"Did not find artifact $name on the classpath"))

  def collect(obj: AnyRef): Seq[File] =
    val result: mutable.ArrayBuffer[File] = mutable.ArrayBuffer.empty[File]
    val visitor: ClassLoaderVisitor = new ClassLoaderVisitor:
      override def visitClassPath(classPath: Array[URL]): Unit =
        for
          url: URL <- classPath
          if url.getProtocol == "file"
        do
          result += Files.url2file(url)
    visitor.visit(obj.getClass.getClassLoader)
    result.toSeq
