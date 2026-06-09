package org.podval.tools.scalajs

import org.podval.tools.nonjvm.Named

enum ESVersion(val year: Int) extends Named(year.toString) derives CanEqual:
  case ES2015 extends ESVersion(2015)
  case ES2016 extends ESVersion(2016)
  case ES2017 extends ESVersion(2017)
  case ES2018 extends ESVersion(2018)
  case ES2019 extends ESVersion(2019)
  case ES2020 extends ESVersion(2020)
  case ES2021 extends ESVersion(2021)
  case ES2022 extends ESVersion(2022)
  case ES2023 extends ESVersion(2023)
  case ES2024 extends ESVersion(2024)
  case ES2025 extends ESVersion(2025)
  case ES2026 extends ESVersion(2026)

object ESVersion extends Named.Companion[ESVersion]("ESVersion"):
  override def default: ESVersion = ES2015
  override def all: Seq[ESVersion] = values.toSeq
