package org.podval.tools.scalajs

import org.podval.tools.util.Named

enum JSEnvKind(name: String) extends Named(name) derives CanEqual:
  case NodeJS      extends JSEnvKind("Node.js")
  case JSDOMNodeJS extends JSEnvKind("Node.js+DOM")
  case Playwright  extends JSEnvKind("Playwright")

object JSEnvKind extends Named.Companion[JSEnvKind]("JSEnv"):
  override def default: JSEnvKind = NodeJS
  override def all: Seq[JSEnvKind] = values.toSeq
