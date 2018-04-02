package org.infobip.prometheus.prtgexporter.prometheus

import org.infobip.prometheus.prtgexporter.prtg.PrtgSensorData

interface Enricher{
    fun enrich(data: Collection<PrtgSensorData>): Collection<PrtgSensorData>
}
