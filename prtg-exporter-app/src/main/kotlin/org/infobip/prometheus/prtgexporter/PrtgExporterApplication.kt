package org.infobip.prometheus.prtgexporter

import org.infobip.prometheus.prtgexporter.prometheus.CustomCollector
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class PrtgExporterApplication {
    @Bean
    fun registerCollectors(collectors: Collection<CustomCollector>): Collection<CustomCollector> {
        collectors.forEach { it.register<CustomCollector>() }
        return collectors
    }
}

fun main(args: Array<String>) {
    SpringApplication.run(PrtgExporterApplication::class.java, *args)
}
