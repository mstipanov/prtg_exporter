package org.infobip.prometheus.prtgexporter.prometheus

import io.prometheus.client.Collector
import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorDataProvider
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.util.*

@Service
class PrtgCollector(private val prtgSensorDataProvider: PrtgSensorDataProvider,
                    @Value("\${prtg.labels:parentdevicename,parentdeviceid}") private val labelsArray: Array<String>) : CustomCollector() {

    private val labels: List<String> = Arrays.asList(*labelsArray)

    override fun collect(): MutableList<MetricFamilySamples> {
        val metricFamilySamples = ArrayList<Collector.MetricFamilySamples>()
        metricFamilySamples.add(buildUsedCpuCounter())
        return metricFamilySamples
    }

    private fun buildUsedCpuCounter(): Collector.MetricFamilySamples {
        val samples = prtgSensorDataProvider.getSensorData(labels).map {
            MetricFamilySamples.Sample(it.name, it.keyNames,
                    it.keyValues, it.value!!)
        }

        return Collector.MetricFamilySamples(
                "prtg_sensor_data",
                Collector.Type.GAUGE,
                "prtg_sensor_data",
                samples
        )
    }
}