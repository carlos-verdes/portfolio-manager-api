/*
 * Copyright 2021 Call Folio
 *
 * SPDX-License-Identifier: MIT
 */

package com.callfolio.portfolio

import pureconfig.ConfigSource
import pureconfig.generic.auto._

case class ServerConfig(host: String, port: Int, other: Int)

object ServerConfig {

  def load(conf: ConfigSource = ConfigSource.default): ServerConfig =
    conf.at("server").loadOrThrow[ServerConfig]
}
