package no.nav.syfo.sykmeldingvarsel.db

import java.time.OffsetDateTime
import java.util.UUID
import no.nav.syfo.sykmeldingvarsel.VarselType

data class SendtVarsel(
    val sykmeldingId: String,
    val narmesteLederId: UUID,
    val bestillingId: UUID,
    val varselType: VarselType,
    val timestamp: OffsetDateTime,
)
