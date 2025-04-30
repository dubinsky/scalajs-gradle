package org.podval.tools.scalanative

import scala.scalanative.util.Scope
import java.util.concurrent.locks.ReentrantLock

// see scala.scalanative.sbtplugin.ScalaNativePluginInternal
/* Provider of Scope that can be shared between different threads.
 * Closed automatically when all concurrent users release their access  */
object ScalaNativeSharedScope:
  private val modificationLock = new ReentrantLock()
  private var scope = Scope.unsafe()
  private var counter = 0

  def apply[T](use: Scope => T): T =
    try use(acquire())
    finally release()

  private def acquire(): Scope =
    modificationLock.lock()
    try
      counter += 1
      scope
    finally modificationLock.unlock()

  private def release(): Unit =
    modificationLock.lock()
    try
      counter -= 1
      if counter == 0 then
        scope.close()
        scope = Scope.unsafe()
    finally modificationLock.unlock()
