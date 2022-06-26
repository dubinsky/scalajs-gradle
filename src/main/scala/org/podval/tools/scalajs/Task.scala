package org.podval.tools.scalajs

// TODO switch to ZIO?
final class Task[T] private(action: () => T):
  def run(): T = action()

  def map[S](f: T => S): Task[S] = Task[S](f(run()))

  def flatMap[S](f: T => Task[S]): Task[S] = Task[S](f(run()).run())

object Task:
  def apply[T](action: => T): Task[T] = new Task(() => action)

  //  val noop: Task[Unit] = Task(())

  // Note: does not work when tasks.isEmpty
  def join[T](tasks: Seq[Task[T]]): Task[Seq[T]] = Task(for task <- tasks yield task.run())

//  def reduced[S](i: IndexedSeq[Task[S]], f: (S, S) => S): Task[S] =
//    i match {
//      case Seq()     => sys.error("Cannot reduce empty sequence")
//      case Seq(x)    => x
//      case Seq(x, y) => reducePair(x, y, f)
//      case _ =>
//        val (a, b) = i.splitAt(i.size / 2)
//        reducePair(reduced(a, f), reduced(b, f), f)
//    }
//
//  def reducePair[S](a: Task[S], b: Task[S], f: (S, S) => S): Task[S] =
//    multInputTask[λ[L[x] => (L[S], L[S])]]((a, b))(AList.tuple2[S, S]) map f.tupled
