package org.infobip.prometheus.prtgexporter.prtg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl.asyncHttpClient
import org.asynchttpclient.Response
import org.infobip.prometheus.prtgexporter.AbstractProcessor
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicReference


@Service
class PrtgSensorDataProvider @Autowired constructor(@Value("\${prtg.url:http://127.0.0.1:8080}") val prtgUrl: String,
                                                    @Value("\${prtg.username:}") val prtgUsername: String,
                                                    @Value("\${prtg.password:}") val prtgPassword: String,
                                                    @Value("\${prtg.sensors.initial.count:1000}") var sensorCount: Int,
                                                    @Value("\${prtg.sensors.page.size:1000}") val pageSize: Int,
                                                    @Value("\${prtg.pause:20000}") val pause: Long) : AbstractProcessor() {
    private val log = LoggerFactory.getLogger(javaClass)

    private val asyncHttpClient = asyncHttpClient()
    private val fetchPrtgAllSensorData = AtomicReference<Collection<PrtgSensorData>>()

    fun getSensorData(): Collection<PrtgSensorData> {
        return fetchPrtgAllSensorData.get() ?: return fetch()
    }

    override fun process() {
        try {
            fetch()
            Thread.sleep(pause)
        } catch (e: Exception) {
            log.error("Error fetching data", e)
        }
    }

    private fun fetch(): Collection<PrtgSensorData> {
        log.info("Fetching data - start")
        val collection = fetchPrtgAllSensorData(asyncHttpClient)
        fetchPrtgAllSensorData.set(collection)
        log.info("Fetching data - end")
        return collection
    }

    private fun fetchPrtgAllSensorData(asyncHttpClient: AsyncHttpClient): Collection<PrtgSensorData> {
        val allSensors = mutableListOf<PrtgSensorData>()
        var sensors = fetchPrtgSensorData(0, sensorCount, asyncHttpClient)
        allSensors.addAll(sensors)
        while (sensors.size == sensorCount) {
            sensors = fetchPrtgSensorData(allSensors.size, pageSize, asyncHttpClient)
            allSensors.addAll(sensors)
        }
        this.sensorCount = allSensors.size
        return allSensors
    }

    private fun fetchPrtgSensorData(start: Int, count: Int, asyncHttpClient: AsyncHttpClient): Collection<PrtgSensorData> {
        var toRequest = count
        var from = start
        val futures = mutableListOf<Future<Response>>()

        var requestSize = Math.min(pageSize, toRequest)
        while (requestSize > 0) {
            val url = append(append("$prtgUrl/api/table.json?content=sensors&columns=objid,device,name,group,tags,lastvalue&start=$from&count=$requestSize&filter_active=-1", "username", prtgUsername), "passhash", prtgPassword)
            futures.add(asyncHttpClient.prepareGet(url).execute())
            toRequest -= requestSize
            from += requestSize
            requestSize = Math.min(pageSize, toRequest)
        }

        val list = futures.map { parseResponse(it.get()) }.flatten()
        return list
    }

    private fun parseResponse(response: Response): Collection<PrtgSensorData> {
        val objectMapper = ObjectMapper()
        val prtgResponse = objectMapper.readValue(response.responseBodyAsStream, PrtgResponse::class.java)
        return prtgResponse.sensors!!
    }

    private fun append(s: String, key: String, value: Any): String {
        return when {
            value.toString().isBlank() -> s
            s.contains("?") -> "$s&${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
            else -> "$s?${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
        }
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgResponse(
        var sensors: Collection<PrtgSensorData>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class PrtgSensorData(
        var objid: Long? = null,
        var device: String? = null,
        var name: String? = null,
        var group: String? = null,
        var tags: String? = null,
        var lastvalue: String? = null,
        var lastvalue_raw: Double? = null
)
