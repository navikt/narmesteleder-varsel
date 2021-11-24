package no.nav.syfo.sykmeldingvarsel.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.log
import no.nav.syfo.sykmeldingvarsel.SendtSykmeldingVarselService
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration

class SendtSykmeldingConsumerService(
    private val kafkaConsumer: KafkaConsumer<String, SendtSykmelding>,
    private val sendtSykmeldingVarselService: SendtSykmeldingVarselService,
    private val topic: String,
    private val applicationState: ApplicationState
) {
    companion object {
        private const val POLL_DURATION_SECONDS = 10L
    }

    fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        while (applicationState.ready) {
            kafkaConsumer.poll(Duration.ofSeconds(POLL_DURATION_SECONDS)).forEach {
                try {
                    log.info("Mottatt sendt sykmelding med id ${it.value().kafkaMetadata.sykmeldingId}")
                    sendtSykmeldingVarselService.handterSendtSykmelding(it.value())
                } catch (ex: Exception) {
                    log.error("Error in consuming sendt sykmelding", ex)
                    throw ex
                }
            }
        }
    }
}
