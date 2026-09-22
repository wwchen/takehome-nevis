package com.nevis.search

import com.nevis.search.config.AppProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication

@SpringBootApplication
@EnableConfigurationProperties(AppProperties::class)
class NevisSearchApplication

fun main(args: Array<String>) {
    runApplication<NevisSearchApplication>(*args)
}
