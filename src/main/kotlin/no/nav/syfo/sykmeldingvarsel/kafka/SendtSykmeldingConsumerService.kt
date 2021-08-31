package no.nav.syfo.sykmeldingvarsel.kafka

import no.nav.syfo.application.ApplicationState
import no.nav.syfo.log
import no.nav.syfo.sykmeldingvarsel.SendtSykmeldingVarselService
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

class SendtSykmeldingConsumerService(
    private val kafkaConsumer: KafkaConsumer<String, SendtSykmelding>,
    private val sendtSykmeldingVarselService: SendtSykmeldingVarselService,
    private val topic: String,
    private val applicationState: ApplicationState
) {

    fun startConsumer() {
        kafkaConsumer.subscribe(listOf(topic))
        while (applicationState.ready) {
            kafkaConsumer.poll(Duration.ZERO).forEach {
                try {
                    if (skalBehandle(it)) {
                        log.info("Mottatt sendt sykmelding med id ${it.value().kafkaMetadata.sykmeldingId}")
                        sendtSykmeldingVarselService.handterSendtSykmelding(it.value())
                    }
                } catch (ex: Exception) {
                    log.error("Error in consuming sendt sykmelding", ex)
                    throw ex
                }
            }
        }
    }

    fun skalBehandle(cr: ConsumerRecord<String, SendtSykmelding>): Boolean {
        val partisjonOffset = mapOf(0 to 6587214, 1 to 6578122, 2 to 6588686)
        if (cr.offset() > partisjonOffset[cr.partition()]!!) {
            if (cr.value().kafkaMetadata.timestamp.isAfter(OffsetDateTime.of(LocalDate.of(2021, 8, 31).atStartOfDay(), ZoneOffset.UTC))) {
                return true
            }
        }
        return false
    }
}
