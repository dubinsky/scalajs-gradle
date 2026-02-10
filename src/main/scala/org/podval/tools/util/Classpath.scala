package org.podval.tools.util

import org.gradle.internal.classloader.{ClassLoaderVisitor, ClasspathUtil, VisitableURLClassLoader}
import scala.jdk.CollectionConverters.IterableHasAsJava
import java.io.File
import java.net.{URL, URLClassLoader}

object Classpath:
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
  
  def get: List[URL] =
    var result: List[URL] = List.empty
    val visitor: ClassLoaderVisitor = new ClassLoaderVisitor:
      override def visitClassPath(classPath: Array[URL]): Unit = result = result ++ classPath
    visitor.visit(getClass.getClassLoader)
    result
