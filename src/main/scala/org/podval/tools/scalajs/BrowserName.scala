package org.podval.tools.scalajs

import org.podval.tools.platform.Named

enum BrowserName(name: String) extends Named(name) derives CanEqual:
  case Chrome   extends BrowserName("chrome")
  case Chromium extends BrowserName("chromium")
  case Firefox  extends BrowserName("firefox")
  case Webkit   extends BrowserName("webkit")
  
object BrowserName extends Named.Companion[BrowserName]("BrowserName"):
  override def default: BrowserName = Chromium
  override def all: Seq[BrowserName] = values.toSeq
