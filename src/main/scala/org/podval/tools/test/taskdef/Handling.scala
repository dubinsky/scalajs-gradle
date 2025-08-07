package org.podval.tools.test.taskdef

import sbt.testing.{OptionalThrowable, Status}

sealed trait Handling derives CanEqual

object Handling:
  case object Success extends Handling

  case object Failed extends Handling

  final case class Failure(throwable: Throwable) extends Handling

  final case class Skipped(hasThrowable: Boolean) extends Handling
  
  def forEvent(
    status: Status,
    optionalThrowable: OptionalThrowable
  ): Handling =
    // Note: Can't use `Option.when()` here: it does not exist in Scala 2.21.
    val throwable: Option[Throwable] = if optionalThrowable.isEmpty then None else Some(optionalThrowable.get)
    
    status.name match
      case "Success" =>
        Success
      case "Error" | "Failure" =>
        throwable.fold(Failed)(Failure(_))
      case "Skipped" | "Ignored" | "Canceled" | "Pending" =>
        Skipped(throwable.isDefined)
