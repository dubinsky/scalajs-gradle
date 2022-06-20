package org.podval.tools.scalajs.testing

// TODO add pure etc. - or use a bit of ZIO?
final class Task[T] private(action: () => T):
  def run(): T = action()

  def map[S](f: T => S): Task[S] = Task[S](f(run()))

  def flatMap[S](f: T => Task[S]): Task[S] = Task[S](f(run()).run())

object Task:
  def apply[T](action: => T): Task[T] = new Task(() => action)

  val noop: Task[Unit] = Task(())
  
  def join[T](tasks: Seq[Task[T]]): Task[Seq[T]] = Task(for task <- tasks yield task.run())
