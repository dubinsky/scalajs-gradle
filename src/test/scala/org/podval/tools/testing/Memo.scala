package org.podval.tools.testing

// TODO find in the standard library - LazyRef?
final class Memo[A](getter: => A):
  private lazy val value: A = getter

  def get: A = value

  def map[B](f: A => B): Memo[B] = Memo(f(get))
