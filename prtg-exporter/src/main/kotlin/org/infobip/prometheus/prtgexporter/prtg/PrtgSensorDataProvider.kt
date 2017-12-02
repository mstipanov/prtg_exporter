package org.infobip.prometheus.prtgexporter.prtg

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.asynchttpclient.AsyncHttpClient
import org.asynchttpclient.Dsl.asyncHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.net.URLEncoder


@Service
class PrtgSensorDataProvider @Autowired constructor(@Value("\${prtg.url:http://127.0.0.1:8080}") val prtgUrl: String,
                                                    @Value("\${prtg.username:}") val prtgUsername: String,
                                                    @Value("\${prtg.password:}") val prtgPassword: String) {
    fun getSensorData(): Array<PrtgSensorData> {
        val asyncHttpClient = asyncHttpClient()
        return fetchPrtgSensorData(asyncHttpClient)
    }

    private fun fetchPrtgSensorData(asyncHttpClient: AsyncHttpClient): Array<PrtgSensorData> {
        val url = append(append("$prtgUrl/api/table.json?content=sensors&columns=objid,device,name,group,tags,lastvalue&count=100000&start=0&filter_active=-1", "username", prtgUsername), "passhash", prtgPassword)

        val whenResponse = asyncHttpClient.prepareGet(url).execute()
        val response = whenResponse.get()
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
        var sensors: Array<PrtgSensorData>? = null
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
