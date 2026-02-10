package org.podval.tools.util

import java.lang.reflect.{AccessibleObject, Field, Method}
import scala.reflect.ClassTag

object Reflection:
  private def getMember[
    C : ClassTag, 
    M <: AccessibleObject
  ](
    m: Class[?] => M
  ): M =
    val member: M = m(summon[ClassTag[C]].runtimeClass)
    member.setAccessible(true)
    member

  final class Invoke[T, C : ClassTag](
    methodName: String,
    parameterTypes: Class[?]*
  ):
    private val method: Method = getMember[C, Method](_.getDeclaredMethod(methodName, parameterTypes*))

    def apply(obj: Object, args: Object*): T = method.invoke(obj, args*).asInstanceOf[T]

  final class Get[T, C : ClassTag](
    fieldName: String
  ):
    private val field: Field = getMember[C, Field](_.getDeclaredField(fieldName))

    def apply(obj: Object): T = field.get(obj).asInstanceOf[T]
