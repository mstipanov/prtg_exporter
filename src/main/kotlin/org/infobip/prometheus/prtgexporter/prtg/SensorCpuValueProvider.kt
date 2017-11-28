package org.infobip.prometheus.prtgexporter.prtg

import org.springframework.stereotype.Service

@Service
class SensorCpuValueProvider : SensorValueProvider {
    override fun convertName(s: String): String = "cpu_used"

    override val sensorType: String
        get() = "wmihypervserver"

    override fun convertValue(valString: String): Double? {
        if (null == valString || valString.isBlank() || valString.trim() == "-")
            return null;
        val split = valString.split(" ")
        val v = split[0].toDoubleOrNull() ?: return null
        return v / 100
    }
}