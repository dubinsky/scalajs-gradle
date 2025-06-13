package org.podval.tools.backend.scalajs

import org.podval.tools.util.Named

enum JSEnvKind(name: String) extends Named(name) derives CanEqual:
  case NodeJSEnv       extends JSEnvKind("NodeJSEnv")
  case JSDOMNodeJSEnv  extends JSEnvKind("JSDOMNodeJSEnv")

object JSEnvKind extends Named.Companion[JSEnvKind]("JSEnv"):
  override def default: JSEnvKind = JSDOMNodeJSEnv
  override def all: Seq[JSEnvKind] = values.toSeq
