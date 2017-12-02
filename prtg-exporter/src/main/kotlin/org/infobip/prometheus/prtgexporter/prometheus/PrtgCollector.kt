package org.infobip.prometheus.prtgexporter.prometheus

import io.prometheus.client.Collector
import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorData
import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorDataProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PrtgCollector(private val prtgSensorDataProvider: PrtgSensorDataProvider) : CustomCollector() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val labels: List<String> = listOf("sensor_id", "device", "name", "group", "sensor_type")


    override fun collect(): MutableList<MetricFamilySamples> {
        val metricFamilySamples = ArrayList<Collector.MetricFamilySamples>()

        try {
            val allSensorData = prtgSensorDataProvider.getSensorData()
            val map = allSensorData.groupBy { detectType(it) }
            for (entry in map) {
                metricFamilySamples.add(buildSamples(entry.key, entry.value))
            }
        } catch (e: Exception) {
            log.error("Error fetching prtg metrics", e)
        }
        return metricFamilySamples
    }

    private fun buildSamples(type: Type, sensorData: List<PrtgSensorData>): MetricFamilySamples {
        val samples = sensorData.map {
            if (null == it.lastvalue_raw) {
                println("Skipping: $it")
                null
            } else {
                try {
                    MetricFamilySamples.Sample("prtg_sensor_${type}_value", labels,
                            listOf(it.objid.toString(), it.device!!, it.name!!, it.group!!, it.tags!!.split(" ")[0]), it.lastvalue_raw!!)
                } catch (e: Exception) {
                    println("Skipping: $it")
                    null
                }
            }
        }.filter { it != null }

        return Collector.MetricFamilySamples(
                "prtg_sensor_data_$type",
                Collector.Type.GAUGE,
                "prtg_sensor_data_$type",
                samples
        )
    }

    private fun detectType(prtgSensorData: PrtgSensorData): Collector.Type {
        //TODO detect by first tag
        return Collector.Type.GAUGE
    }
}