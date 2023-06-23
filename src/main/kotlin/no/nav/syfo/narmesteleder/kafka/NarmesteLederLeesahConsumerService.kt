package no.nav.syfo.narmesteleder.kafka

import java.time.Duration
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.log
import no.nav.syfo.narmesteleder.OppdaterNarmesteLederService
import org.apache.kafka.clients.consumer.KafkaConsumer

class NarmesteLederLeesahConsumerService(
    private val kafkaConsumer: KafkaConsumer<String, NarmesteLederLeesah>,
    private val applicationState: ApplicationState,
    private val topic: String,
    private val oppdaterNarmesteLederService: OppdaterNarmesteLederService,
) {

    companion object {
        private const val POLL_DURATION_SECONDS = 10L
    }

    fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        log.info("Starting consuming topic $topic")
        while (applicationState.ready) {
            kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS)).forEach {
                try {
                    log.info("Mottatt narmesteleder-oppdatering med id ${it.key()}")
                    oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(it.value())
                } catch (e: Exception) {
                    log.error(
                        "Noe gikk galt ved mottak av narmesteleder-melding med offset ${it.offset()} og key ${it.key()}: ${e.message}"
                    )
                    throw e
                }
            }
        }
    }
}
