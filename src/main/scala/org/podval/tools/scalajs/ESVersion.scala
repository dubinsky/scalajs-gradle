package org.podval.tools.scalajs

import org.podval.tools.platform.Named

enum ESVersion(name: String) extends Named(name) derives CanEqual:
  case ES2015 extends ESVersion("2015")
  case ES2016 extends ESVersion("2016")
  case ES2017 extends ESVersion("2017")
  case ES2018 extends ESVersion("2018")
  case ES2019 extends ESVersion("2019")
  case ES2020 extends ESVersion("2020")
  case ES2021 extends ESVersion("2021")


object ESVersion extends Named.Companion[ESVersion]("ESVersion"):
  override def default: ESVersion = ES2015
  override def all: Seq[ESVersion] = values.toSeq
