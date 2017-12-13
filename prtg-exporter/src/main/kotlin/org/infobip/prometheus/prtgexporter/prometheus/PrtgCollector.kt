package org.infobip.prometheus.prtgexporter.prometheus

import io.prometheus.client.Collector
import org.infobip.prometheus.prtgexporter.prtg.PrtgChannelData
import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorData
import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorDataProvider
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.*

@Service
class PrtgCollector(private val prtgSensorDataProvider: PrtgSensorDataProvider) : CustomCollector() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val labels: List<String> = listOf("sensor_id", "device", "name", "channel_id", "channel_name", "group", "sensor_type")


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

    private fun buildSamples(type: String, sensorData: List<PrtgSensorData>): MetricFamilySamples {
        val samples: List<MetricFamilySamples.Sample> = sensorData.map { sensor ->
            val init: List<Pair<PrtgSensorData, PrtgChannelData?>> = listOf()
            val list = sensor.channels?.fold(init) { acc, channel -> acc.plus(sensor to channel) } ?: listOf(sensor to null)
            list.mapNotNull { (sensor, channel) ->
                if (null === channel) {
                    if (null === sensor.lastvalue_raw) {
                        null
                    } else {
                        try {
                            MetricFamilySamples.Sample("prtg_sensor_$type", labels,
                                    listOf(sensor.objid.toString(), sensor.device!!, sensor.name!!, "", "", sensor.group!!, type), sensor.lastvalue_raw!!)
                        } catch (e: Exception) {
                            log.error("Error writing metric for sensor: $sensor ", e)
                            null
                        }
                    }
                } else {
                    if (null === channel.objid || null === channel.lastvalue_raw) {
                        null
                    } else {
                        try {
                            MetricFamilySamples.Sample("prtg_sensor_$type", labels,
                                    listOf(sensor.objid.toString(), sensor.device!!, sensor.name!!, channel.objid.toString(), channel.name ?: "", sensor.group!!, type), channel.lastvalue_raw!!)
                        } catch (e: Exception) {
                            log.error("Error writing metric for sensor: $sensor and channel: $channel", e)
                            null
                        }
                    }
                }
            }
        }.flatten()

        return Collector.MetricFamilySamples(
                "prtg_sensor_$type",
                Collector.Type.GAUGE,
                "prtg_sensor_$type",
                samples
        )
    }

    private fun detectType(prtgSensorData: PrtgSensorData): String {
        return prtgSensorData.tags!!.split(" ")[0]
    }
}