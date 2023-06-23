package no.nav.syfo.sykmeldingvarsel

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.syfo.application.db.DatabaseInterface
import no.nav.syfo.application.metrics.SENDT_SYKMELDING_VARSEL_COUNTER
import no.nav.syfo.log
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.narmesteleder.db.finnNarmestelederForSykmeldt
import no.nav.syfo.sykmeldingvarsel.db.SendtVarsel
import no.nav.syfo.sykmeldingvarsel.db.harSendtVarsel
import no.nav.syfo.sykmeldingvarsel.db.lagreSendtVarsel
import no.nav.syfo.sykmeldingvarsel.doknotifikasjon.DoknotifikasjonProducer
import no.nav.syfo.sykmeldingvarsel.doknotifikasjon.tilNotifikasjonMedkontaktInfo
import no.nav.syfo.sykmeldingvarsel.kafka.SendtSykmelding

class SendtSykmeldingVarselService(
    private val database: DatabaseInterface,
    private val doknotifikasjonProducer: DoknotifikasjonProducer,
) {
    fun handterSendtSykmelding(sendtSykmelding: SendtSykmelding) {
        val nlSporsmal =
            sendtSykmelding.event.sporsmals?.find { it.shortName == ShortNameDTO.NY_NARMESTE_LEDER }
        if (nlSporsmal?.svar == "JA") {
            log.info(
                "Den sykmeldte har bedt om ny leder. Sender ikke varsel for sykmeldingId ${sendtSykmelding.kafkaMetadata.sykmeldingId}"
            )
            return
        }
        val harSendtSammeVarsel =
            database.harSendtVarsel(
                sendtSykmelding.kafkaMetadata.sykmeldingId,
                VarselType.SENDT_SYKMELDING
            )

        if (harSendtSammeVarsel) {
            log.warn(
                "Har allerede sendt varsel om sendt sykmelding for sykmelding med id ${sendtSykmelding.kafkaMetadata.sykmeldingId}"
            )
        } else {
            val narmesteLeder =
                database.finnNarmestelederForSykmeldt(
                    fnr = sendtSykmelding.kafkaMetadata.fnr,
                    orgnummer = sendtSykmelding.event.arbeidsgiver.orgnummer
                )
            if (narmesteLeder == null) {
                log.info(
                    "Mangler nærmeste leder for sykmeldingid ${sendtSykmelding.kafkaMetadata.sykmeldingId}, sender ikke varsel"
                )
            } else {
                val bestillingId = UUID.randomUUID()
                val doknotifikasjon =
                    tilNotifikasjonMedkontaktInfo(bestillingId.toString(), narmesteLeder)
                doknotifikasjonProducer.send(
                    doknotifikasjon,
                    sendtSykmelding.kafkaMetadata.sykmeldingId
                )
                database.lagreSendtVarsel(
                    SendtVarsel(
                        sykmeldingId = sendtSykmelding.kafkaMetadata.sykmeldingId,
                        narmesteLederId = narmesteLeder.narmesteLederId,
                        bestillingId = bestillingId,
                        varselType = VarselType.SENDT_SYKMELDING,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                    ),
                )
                SENDT_SYKMELDING_VARSEL_COUNTER.inc()
                log.info(
                    "Har sendt varsel om sendt sykmelding med id ${sendtSykmelding.kafkaMetadata.sykmeldingId}, bestillingId $bestillingId"
                )
            }
        }
    }
}
