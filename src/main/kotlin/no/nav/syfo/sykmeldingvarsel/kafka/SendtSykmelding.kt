package no.nav.syfo.sykmeldingvarsel.kafka

import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO

data class SendtSykmelding(
    val kafkaMetadata: KafkaMetadataDTO,
    val event: SendtEvent
)

data class SendtEvent(
    val arbeidsgiver: ArbeidsgiverStatus,
    val sporsmals: List<SporsmalOgSvarDTO>?
)

data class ArbeidsgiverStatus(
    val orgnummer: String
)
