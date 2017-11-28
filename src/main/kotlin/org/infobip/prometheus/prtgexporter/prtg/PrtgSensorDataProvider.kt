package org.infobip.prometheus.prtgexporter.prtg

import com.fasterxml.jackson.databind.ObjectMapper
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl.asyncHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder
import java.util.*


@Service
class PrtgSensorDataProvider @Autowired constructor(sensorValueProviders: Collection<SensorValueProvider>,
                                                    @Value("\${prtg.sensor.details.url}") val prtgSensorDetailsUrl: String,
                                                    @Value("\${prtg.username:}") val prtgUsername: String,
                                                    @Value("\${prtg.password:}") val prtgPassword: String,
                                                    @Value("\${prtg.sensors}") val sensorIds: Array<String>) {
    val sensorValueProviderMap = sensorValueProviders.associateBy { it.sensorType }

    fun getSensorData(keys: List<String>): List<PrtgSensorData> {
        val asyncHttpClient = asyncHttpClient()
        return sensorIds.map { fetchPrtgSensorData(asyncHttpClient, it, keys) }.filter { it.value != null }
    }

    private fun fetchPrtgSensorData(asyncHttpClient: AsyncHttpClient, sensorId: String, keys: List<String>): PrtgSensorData {
        val url = append(append(append(prtgSensorDetailsUrl, "id", sensorId), "username", prtgUsername), "passhash", prtgPassword)

        val whenResponse = asyncHttpClient.prepareGet(url).execute()
        val response = whenResponse.get()
        val objectMapper = ObjectMapper()
        val map = objectMapper.readValue(response.responseBodyAsStream, HashMap::class.java)
        val sensorData = map["sensordata"] as Map<*, *>
        val sensorValueProvider = sensorValueProviderMap[sensorData["sensortype"]]!!
        val prtgSensorData = PrtgSensorData(
                sensorValueProvider.convertName(sensorData["name"].toString()),
                keys,
                keys.map { sensorData[it].toString() },
                sensorValueProvider.convertValue(sensorData["lastvalue"].toString()))
        return prtgSensorData
    }

    private fun append(s: String, key: String, value: Any): String {
        return when {
            value.toString().isBlank() -> s
            s.contains("?") -> "$s&${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
            else -> "$s?${URLEncoder.encode(key, "UTF-8")}=${URLEncoder.encode(value.toString(), "UTF-8")}"
        }
    }
}

data class PrtgSensorData(
        val name: String,
        val keyNames: List<String>,
        val keyValues: List<String>,
        val value: Double?
)