package no.nav.syfo.sykmeldingvarsel.db

import no.nav.syfo.sykmeldingvarsel.VarselType
import java.time.OffsetDateTime
import java.util.UUID

data class SendtVarsel(
    val sykmeldingId: String,
    val narmesteLederId: UUID,
    val bestillingId: UUID,
    val varselType: VarselType,
    val timestamp: OffsetDateTime
)
