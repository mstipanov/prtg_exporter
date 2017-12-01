package org.infobip.prometheus.prtgexporter.prtg

import org.springframework.stereotype.Service

@Service
class SensorMemoryValueProvider : SensorValueProvider {
    override fun convertName(s: String): String = "memory_used"

    override val sensorType: String
        get() = "wmimemory"

    override fun convertValue(valString: String): Double? {
        if (valString.isBlank() || valString.trim() == "-")
            return null;
        val split = valString.split(" ")
        if (split.size == 1) {
            return split[0].toDoubleOrNull()
        }

        val v = split[0].toDoubleOrNull() ?: return null
        return v * multiplier(split[1])
    }

    private fun multiplier(s: String): Long {
        return when (s) {
            "MByte" -> 1024
            "GByte" -> 1024 * 1024
            else -> {
                throw IllegalArgumentException("Can't convert to bytes multiplier: $s")
            }
        }
    }
}