package no.nav.syfo.application.metrics

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

const val METRICS_NS = "narmesteledervarsel"

val HTTP_HISTOGRAM: Histogram =
    Histogram.Builder()
        .labelNames("path")
        .name("requests_duration_seconds")
        .help("http requests durations for incoming requests in seconds")
        .register()

val SENDT_SYKMELDING_VARSEL_COUNTER: Counter =
    Counter.build()
        .namespace(METRICS_NS)
        .name("sendtsm_varsel_counter")
        .help("Antall sendte varsel om sendt sykmelding")
        .register()
