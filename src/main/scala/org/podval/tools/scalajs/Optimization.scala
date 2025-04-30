package org.podval.tools.scalajs

import org.podval.tools.util.Named

enum Optimization(name: String) extends Named(name) derives CanEqual:
  case Fast extends Optimization("Fast")
  case Full extends Optimization("Full")

object Optimization extends Named.Companion[Optimization]("Optimization"):
  override def default: Optimization = Fast
  override def all: Seq[Optimization] = values.toSeq
