package org.podval.tools.test.task

import org.gradle.api.tasks.testing.{AbstractTestTask, TestListener}
import org.gradle.internal.event.ListenerBroadcast
import java.lang.reflect.{Field, Method}

// see:
// https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/resources/org/jetbrains/plugins/gradle/IJTestLogger.groovy
// https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/tooling-extension-impl/resources/org/jetbrains/plugins/gradle/tooling/internal/init/IjTestEventLoggerInit.gradle
// https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/tooling-extension-impl/resources/org/jetbrains/plugins/gradle/tooling/internal/init/IjTestEventLogger.gradle
// https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/tooling-extension-impl/resources/org/jetbrains/plugins/gradle/tooling/internal/init/FileComparisonTestEventLoggerInit.gradle
// https://github.com/JetBrains/intellij-community/blob/master/plugins/gradle/tooling-extension-impl/resources/org/jetbrains/plugins/gradle/tooling/internal/init/FileComparisonTestEventLogger.gradle
object IntelliJIdea:
  private val testListenerClassNames: Set[String] = Set(
    "IJTestEventLogger$1",
    "FileComparisonTestEventLogger$1"
  )

  def runningIn(task: AbstractTestTask): Boolean =
    var result: Boolean = false

    val testListenerSubscriptions: Field = classOf[AbstractTestTask].getDeclaredField("testListenerSubscriptions")
    testListenerSubscriptions.setAccessible(true)

    val broadcastSubscriptionsGet: Method = Class
      .forName("org.gradle.api.tasks.testing.AbstractTestTask$BroadcastSubscriptions")
      .getDeclaredMethod("get")
    broadcastSubscriptionsGet.setAccessible(true)
    broadcastSubscriptionsGet
      .invoke(
        testListenerSubscriptions.get(task)
      )
      .asInstanceOf[ListenerBroadcast[TestListener]]
      .visitListeners((testListener: TestListener) =>
        val className: String = testListener.getClass.getName
//        task.getLogger.lifecycle(s"=== TestListener class name: $className")
        if testListenerClassNames.contains(className) then result = true
      )

    result
