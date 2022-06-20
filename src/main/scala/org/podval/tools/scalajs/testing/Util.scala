package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint}
import scala.util.control.NonFatal

object Util:
  def safeForeach[T](it: Iterable[T], log: Logger)(f: T => Unit): Unit = it.foreach(i =>
    try f(i)
    catch { case NonFatal(e) => log.trace("", e); log.error(e.toString) } // TODO the message
  )

  def getFingerprints(framework: Framework): Seq[Fingerprint] =
  // TODO why is reflection used instead of the direct call?
    framework.getClass.getMethod("fingerprints").invoke(framework) match
      case fingerprints: Array[Fingerprint] => fingerprints.toList
      case _                                => sys.error(s"Could not call 'fingerprints' on framework $framework")

  def hashCode(f: Fingerprint): Int = f match
    case s: SubclassFingerprint  => (s.isModule, s.superclassName).hashCode
    case a: AnnotatedFingerprint => (a.isModule, a.annotationName).hashCode
    case _                       => 0

  def matches(a: Fingerprint, b: Fingerprint): Boolean = (a, b) match
    case (a: SubclassFingerprint, b: SubclassFingerprint) =>
      a.isModule == b.isModule && a.superclassName == b.superclassName
    case (a: AnnotatedFingerprint, b: AnnotatedFingerprint) =>
      a.isModule == b.isModule && a.annotationName == b.annotationName
    case _ => false

  def toString(f: Fingerprint): String = f match
    case sf: SubclassFingerprint  => s"subclass(${sf.isModule}, ${sf.superclassName})"
    case af: AnnotatedFingerprint => s"annotation(${af.isModule}, ${af.annotationName})"
    case _                        => f.toString

  def isTestForFramework(test: TestDefinition, framework: Framework): Boolean =
    getFingerprints(framework).exists((fingerprint: Fingerprint) =>
      matches(fingerprint, test.fingerprint)
    )
