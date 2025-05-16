package org.podval.tools.scalanative

import org.podval.tools.nonjvm.NonJvmTestAdapter
import org.slf4j.{Logger, LoggerFactory}
import sbt.testing.Framework
import scala.concurrent.ExecutionContext
import scala.scalanative.build.{Build, Config, Logger as LoggerN}
import scala.scalanative.testinterface.adapter.TestAdapter
import scala.scalanative.util.Scope
import java.io.File
import java.nio.file.Path

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
object ScalaNativeBuild:
  val logger: Logger = LoggerFactory.getLogger(ScalaNativeBuild.getClass)

  def link(config: Config): Path =
    logger.info(s"ScalaNativeBuild.nativeLinkImpl($config)")

    interceptBuildException(
      buildCachedSync(
        config,
        (throwable: Throwable) => logger.warn(s"Trace: $throwable")
      )
    )
    
  private def buildCachedSync(
    config: Config,
    trace: Throwable => Unit
  ): Path =
    implicit val scope: Scope = Scope.forever
    ScalaNativeAwait.await(trace) { implicit ec: ExecutionContext =>
      Build.buildCached(config
        .withLogger(nativeLogger)
      )
    }

  def createTestEnvironment(
    binaryTestFile: File
  ): ScalaNativeTestEnvironment = ScalaNativeTestEnvironment(
    sourceMapper = None, // TODO
    testAdapter = new NonJvmTestAdapter:
      override def loadFrameworks(frameworkNames: List[List[String]]): List[Option[Framework]] =
        testAdapter.loadFrameworks(frameworkNames)
      override def close(): Unit = testAdapter.close()
      private lazy val testAdapter: TestAdapter = TestAdapter(TestAdapter
        .Config()
        .withLogger(nativeLogger)
        .withBinaryFile(binaryTestFile)
      )
  )

  private def nativeLogger: LoggerN = new LoggerN:
    override def trace(msg: Throwable): Unit = logger.trace("", msg)
    override def debug(msg: String): Unit = logger.debug(msg)
    override def info (msg: String): Unit = logger.info (msg)
    override def warn (msg: String): Unit = logger.warn (msg)
    override def error(msg: String): Unit = logger.error(msg)

  // TODO is there a Gradle MessageOnlyException analogue?
  /** Run `op`, rethrows `BuildException`s as `MessageOnlyException`s. */
  def interceptBuildException[T](op: => T): T =
    op
  //    try op
  //    catch
  //      case ex: BuildException => throw new MessageOnlyException(ex.getMessage)
  //      case ex: LinkingException => throw new MessageOnlyException(ex.getMessage)
