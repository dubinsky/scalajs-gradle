package org.podval.tools.scalajs

enum ModuleKind derives CanEqual:
  case NoModule
  case ESModule
  case CommonJSModule
