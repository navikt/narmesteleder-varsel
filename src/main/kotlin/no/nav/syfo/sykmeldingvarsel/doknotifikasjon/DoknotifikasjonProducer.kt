package no.nav.syfo.sykmeldingvarsel.doknotifikasjon

import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo
import no.nav.syfo.log
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord

class DoknotifikasjonProducer(private val kafkaProducer: KafkaProducer<String, NotifikasjonMedkontaktInfo>, private val topicName: String) {
    fun send(notifikasjonMedkontaktInfo: NotifikasjonMedkontaktInfo, sykmeldingId: String) {
        try {
            kafkaProducer.send(ProducerRecord(topicName, notifikasjonMedkontaktInfo.getBestillingsId(), notifikasjonMedkontaktInfo)).get()
        } catch (ex: Exception) {
            log.error("Noe gikk galt ved skriving av varselbestilling med bestillingsid ${notifikasjonMedkontaktInfo.getBestillingsId()}, sykmeldingid $sykmeldingId", ex.message)
            throw ex
        }
    }
}
