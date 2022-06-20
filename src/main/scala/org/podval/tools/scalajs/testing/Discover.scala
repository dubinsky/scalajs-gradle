package org.podval.tools.scalajs.testing

import org.gradle.api.logging.Logger
import sbt.internal.inc.Analysis
import sbt.testing.{AnnotatedFingerprint, Fingerprint, Framework, SubclassFingerprint, SuiteSelector}
import xsbt.api.{Discovered, Discovery}
import xsbti.api.{AnalyzedClass, ClassLike, Companions, Definition}

// Note: based on sbt.Tests from org.scala-sbt.actions
// TODO add back discovery of mains for running?
object Discover:
  def discover(
    fingerprints: Seq[Fingerprint],
    analysis: Analysis,
    log: Logger
  ): Seq[TestDefinition] =
    val definitions: Seq[Definition] = analysis.apis.internal.values.toSeq.flatMap { (ac: AnalyzedClass) =>
      val companions: Companions = ac.api
      Seq(
        companions.classApi: Definition,
        companions.objectApi: Definition
      ) ++
        companions.classApi .structure.declared .toSeq ++
        companions.classApi .structure.inherited.toSeq ++
        companions.objectApi.structure.declared .toSeq ++
        companions.objectApi.structure.inherited.toSeq
    }

    def debug(message: String): Unit = log.debug(message, null, null, null)

    val subclasses: Seq[(String, Boolean, SubclassFingerprint)] = fingerprints.collect {
      case sub: SubclassFingerprint  => (sub.superclassName, sub.isModule, sub)
    }
    debug(s"Subclass fingerprints: $subclasses")

    val annotations: Seq[(String, Boolean, AnnotatedFingerprint)] = fingerprints.collect {
      case ann: AnnotatedFingerprint => (ann.annotationName, ann.isModule, ann)
    }
    debug("Annotation fingerprints: $annotations")

    def firsts[A, B, C](s: Seq[(A, B, C)]): Set[A] = s.map(_._1).toSet

    def defined(
      in: Seq[(String, Boolean, Fingerprint)],
      names: Set[String],
      IsModule: Boolean
    ): Seq[Fingerprint] =
      in.collect { case (name, IsModule, fingerprint) if names(name) => fingerprint }

    for
      (definition: Definition, discovered: Discovered) <-
        Discovery(
          firsts(subclasses ),
          firsts(annotations)
        )(definitions.filter {
          case c: ClassLike => c.topLevel
          case _            => false
        })
      fingerprint: Fingerprint <-
        defined(subclasses , discovered.baseClasses, discovered.isModule) ++
        defined(annotations, discovered.annotations, discovered.isModule)
    // TODO: To pass in correct explicitlySpecified and selectors
    yield new TestDefinition(
      name = definition.name,
      fingerprint = fingerprint,
      explicitlySpecified = false,
      selectors = Array(new SuiteSelector)
    )
