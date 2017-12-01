package org.infobip.prometheus.prtgexporter.prtg

interface SensorValueProvider {
    val sensorType: String

    fun convertName(s: String): String

    fun convertValue(s: String): Double?
}