package org.podval.tools.scalajs

enum Optimization(val description: String) derives CanEqual:
  case Fast extends Optimization(description = " - fast")
  case Full extends Optimization(description = " - full optimization")
