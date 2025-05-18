package org.podval.tools.build.scalanative

import java.util.concurrent.Executors
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future, Promise}

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
// TODO If this was exposed in Scala Native, I wouldn't need to copy it ;)
object ScalaNativeAwait:
  def await[T](
    trace: Throwable => Unit
  )(body: ExecutionContext => Future[T]): T =
    // Fatal errors, eg. StackOverflowErrors are not propagated by Futures
    // Use a helper promise to get notified about the underlying problem
    val promise = Promise[T]()
    val executor =
      Executors.newFixedThreadPool(
        Runtime.getRuntime.availableProcessors,
        (task: Runnable) =>
          val thread = Executors.defaultThreadFactory().newThread(task)
          val defaultExceptionHandler = thread.getUncaughtExceptionHandler
          thread.setUncaughtExceptionHandler {
            (thread: Thread, ex: Throwable) =>
              promise.tryFailure(ex)
              ex match
                case _: InterruptedException => trace(ex)
                case _ => defaultExceptionHandler.uncaughtException(thread, ex)
          }
          thread
      )
    implicit val ec: ExecutionContext =
      ExecutionContext.fromExecutor(executor, trace(_))

    // Schedule the task and record completion
    body(ec).onComplete(promise.complete)
    try Await.result(promise.future, Duration.Inf)
    finally executor.shutdown()

