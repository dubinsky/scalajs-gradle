package org.podval.tools.test.detect

import org.objectweb.asm.{AnnotationVisitor, ClassReader, ClassVisitor, Opcodes, Type}
import org.slf4j.{Logger, LoggerFactory}
import scala.util.Using
import java.io.{BufferedInputStream, File, FileInputStream}

// Inspired by org.gradle.api.internal.tasks.testing.detection.TestClassVisitor.
object TestClassVisitor:
  private val logger: Logger = LoggerFactory.getLogger(TestClassVisitor.getClass)

  private def classNameFromInternal(internalName: String): String = // "org/junit/Test"
    Type.getObjectType(internalName).getClassName
  private def annotationNameFromInternal(internalName: String): String = // "Lorg/junit/Test;"
    Type.getType(internalName).getClassName

  def apply(
    sbtTestFrameworkDetector: SbtTestFrameworkDetector,
    classFile: File
  ): TestClassVisitor =
    val result: TestClassVisitor = new TestClassVisitor(sbtTestFrameworkDetector)

    Using(BufferedInputStream(FileInputStream(classFile)))((classStream: BufferedInputStream) =>
      ClassReader(classStream.readAllBytes)
        .accept(result, ClassReader.SKIP_DEBUG | ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES)
    ).recover((e: Throwable) =>
      logger.warn(s"TestClassVisitor: failed to read class file ${classFile.getAbsolutePath}.", e)
    )

    result

final class TestClassVisitor private(
  testFrameworkDetector: SbtTestFrameworkDetector
) extends ClassVisitor(Opcodes.ASM9):

  // Mark as abstract - and thus not a test - to begin with,
  // so if class-file reading fails, the class is ignored.
  private var isAbstract: Boolean = true
  private var isModule: Boolean = false
  
  private var _className: Option[String] = None
  def classNameInternal: Option[String] = _className
  def className: Option[String] = _className.map(TestClassVisitor.classNameFromInternal)

  // Fingerprint may apply to any supertype, not just the superclass.
  private var superTypes: Set[String] = Set.empty
  def getSuperTypes: Set[String] = superTypes

  private var detectors: ClassFileDetectors = ClassFileDetectors.empty

  def addDetector(detector: FingerprintDetector): Unit =
    detectors = detectors.add(detector)

  def appendDetectors(more: ClassFileDetectors): Unit =
    detectors = detectors.append(more)

  def getDetectors: ClassFileDetectors = detectors

  def getApplicableDetectors: FingerprintDetectors =
    if isAbstract
    then Set.empty
    else detectors.getApplicable(isModule)

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

    superTypes
      .flatMap((superType: String) =>
        detect(testFrameworkDetector.subclassDetectors, TestClassVisitor.classNameFromInternal(superType))
      )
      .foreach(addDetector)

  override def visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor =
    detect(testFrameworkDetector.annotatedDetectors, TestClassVisitor.annotationNameFromInternal(desc))
      .foreach(addDetector)
    null
  
  private def detect(detectors: Seq[FingerprintDetector], name: String): FingerprintDetectors =
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
    name == _className.get &&
    (access & Opcodes.ACC_STATIC) == 0
