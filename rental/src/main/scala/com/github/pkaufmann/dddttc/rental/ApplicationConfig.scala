package com.github.pkaufmann.dddttc.rental

case class ApplicationConfig(
                              brokerUrl: String,
                              brokerPassword: String,
                              brokerType: String,
                              port: Int,
                              driver: String,
                              url: String,
                              user: String,
                              password: String
                            )
