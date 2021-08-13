package no.nav.syfo.sykmeldingvarsel.kafka

import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO

data class SendtSykmelding(
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SendtEvent
)

data class SendtEvent(
    val arbeidsgiver: ArbeidsgiverStatus
)

data class ArbeidsgiverStatus(
    val orgnummer: String
)
