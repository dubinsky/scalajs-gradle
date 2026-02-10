package org.podval.tools.test.run

import sbt.testing.{OptionalThrowable, Status}

sealed trait Result derives CanEqual

object Result:
  case object Success extends Result

  case object Failed extends Result

  final case class Failure(throwable: Throwable) extends Result

  final case class Skipped(hasThrowable: Boolean) extends Result
  
  def apply(
    status: Status,
    optionalThrowable: OptionalThrowable
  ): Result =
    // Note: Can't use `Option.when()` here: it does not exist in Scala 2.21.
    val throwable: Option[Throwable] = if optionalThrowable.isEmpty then None else Some(optionalThrowable.get)
    
    status.name match
      case "Success" =>
        Success
      case "Error" | "Failure" =>
        throwable.fold(Failed)(Failure(_))
      case "Skipped" | "Ignored" | "Canceled" | "Pending" =>
        Skipped(throwable.isDefined)
