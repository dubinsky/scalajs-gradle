package org.podval.tools.test.detect

import org.objectweb.asm.{AnnotationVisitor, ClassVisitor, Opcodes}

// Note: inspired by org.gradle.api.internal.tasks.testing.detection.TestClassVisitor.
final class TestClassVisitor(
  testFrameworkDetector: SbtTestFrameworkDetector
) extends ClassVisitor(Opcodes.ASM9):

  // Mark as abstract - and thus not a test - to begin with,
  // so if class-file reading fails, the class is ignored.
  private var isAbstract: Boolean = true
  private var isModule: Boolean = false
  private var _className: Option[String] = None
  def className: Option[String] = _className

  // Fingerprint may apply to any supertype, not just the superclass.
  private var superTypes: Set[String] = Set.empty
  def getSuperTypes: Set[String] = superTypes

  private var detectors: ClassFileDetectors = ClassFileDetectors.empty

  private def addDetectors(more: Set[FingerprintDetector]): Unit =
    detectors = detectors.add(more)

  def addDetectors(more: ClassFileDetectors): Unit =
    detectors = detectors.add(more)

  def getDetectors: ClassFileDetectors = detectors

  def getApplicableDetectors: Set[FingerprintDetector] =
    if isAbstract
    then Set.empty
    else detectors.getApplicableDetectors(isModule)

  override def visit(
    version: Int,
    access: Int,
    className: String,
    signature: String,
    superClassName: String,
    interfaces: Array[String]
  ): Unit =
    if className.endsWith("$") then
      isModule = true
      _className = Some(className.init)
    else
      isModule = false
      _className = Option(className)

    isAbstract = (access & Opcodes.ACC_ABSTRACT) != 0

    superTypes = Option(superClassName).toSet ++ interfaces.toSet

    addDetectors(superTypes.flatMap(detectSubclassOf))

  private def detectSubclassOf(className: String): Set[FingerprintDetector] =
    // TODO use org.objectweb.asm.Type?
    val name: String = className.replace('/', '.') // example: "org/scalatest/Suite"
    detect(testFrameworkDetector.subclassDetectors, name)

  override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor =
    addDetectors(detectAnnotatedWith(desc))
    null

  private def detectAnnotatedWith(annotation: String): Set[FingerprintDetector] =
    // TODO use org.objectweb.asm.Type?
    val name: String = annotation.tail.init.replace('/', '.') // example: "Lorg/junit/Test;"
    detect(testFrameworkDetector.annotatedDetectors, name)

  private def detect(detectors: Seq[FingerprintDetector], name: String) =
    detectors.toSet.filter(_.name == name)

  override def visitInnerClass(
    name: String,
    outerName: String,
    innerName: String,
    access: Int
  ): Unit =
    if ignoreNonStaticInnerClass && innerClassIsNonStatic(name, access) then isAbstract = true

  private def ignoreNonStaticInnerClass: Boolean = false

  private def innerClassIsNonStatic(name: String, access: Int): Boolean =
    name == _className.get && (access & Opcodes.ACC_STATIC) == 0
