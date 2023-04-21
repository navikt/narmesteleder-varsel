package no.nav.syfo.sykmeldingvarsel

import io.kotest.core.spec.style.FunSpec
import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
import no.nav.syfo.model.sykmeldingstatus.ShortNameDTO
import no.nav.syfo.model.sykmeldingstatus.SporsmalOgSvarDTO
import no.nav.syfo.model.sykmeldingstatus.SvartypeDTO
import no.nav.syfo.narmesteleder.db.lagreNarmesteLeder
import no.nav.syfo.narmesteleder.model.NarmesteLeder
import no.nav.syfo.sykmeldingvarsel.db.SendtVarsel
import no.nav.syfo.sykmeldingvarsel.db.harSendtVarsel
import no.nav.syfo.sykmeldingvarsel.db.lagreSendtVarsel
import no.nav.syfo.sykmeldingvarsel.doknotifikasjon.DoknotifikasjonProducer
import no.nav.syfo.sykmeldingvarsel.kafka.ArbeidsgiverStatus
import no.nav.syfo.sykmeldingvarsel.kafka.SendtEvent
import no.nav.syfo.sykmeldingvarsel.kafka.SendtSykmelding
import no.nav.syfo.testutils.TestDB
import org.amshove.kluent.shouldBeEqualTo
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SendtSykmeldingVarselServiceTest : FunSpec({
    val testDb = TestDB.database
    val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    val sendtSykmeldingVarselService = SendtSykmeldingVarselService(testDb, doknotifikasjonProducer)

    val fnrAnsatt = "12345678910"
    val fnrLeder = "01987654321"
    val orgnummer = "999000"

    afterTest {
        TestDB.dropData()
        clearMocks(doknotifikasjonProducer)
    }

    context("SendtSykmeldingVarselService") {
        test("Sender varsel til nl hvis nl finnes og varsel ikke er sendt før") {
            val sykmeldingId = UUID.randomUUID().toString()
            val narmesteLederId = UUID.randomUUID()
            testDb.lagreNarmesteLeder(
                NarmesteLeder(
                    narmesteLederId = narmesteLederId,
                    fnr = fnrAnsatt,
                    orgnummer = orgnummer,
                    narmesteLederFnr = fnrLeder,
                    narmesteLederTelefonnummer = "90909090",
                    narmesteLederEpost = "epost@nav.no",
                    aktivFom = LocalDate.now(),
                    arbeidsgiverForskutterer = true,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user",
                    ),
                    SendtEvent(
                        ArbeidsgiverStatus(orgnummer),
                        listOf(
                            SporsmalOgSvarDTO("Be om ny nærmeste leder?", ShortNameDTO.NY_NARMESTE_LEDER, SvartypeDTO.JA_NEI, "NEI"),
                        ),
                    ),
                ),
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo true
            verify { doknotifikasjonProducer.send(any(), any()) }
        }
        test("Sender ikke varsel hvis nl gjelder annet arbeidsforhold") {
            val sykmeldingId = UUID.randomUUID().toString()
            val narmesteLederId = UUID.randomUUID()
            testDb.lagreNarmesteLeder(
                NarmesteLeder(
                    narmesteLederId = narmesteLederId,
                    fnr = fnrAnsatt,
                    orgnummer = orgnummer,
                    narmesteLederFnr = fnrLeder,
                    narmesteLederTelefonnummer = "90909090",
                    narmesteLederEpost = "epost@nav.no",
                    aktivFom = LocalDate.now(),
                    arbeidsgiverForskutterer = true,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user",
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer = "999888"), emptyList()),
                ),
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo false
            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
        test("Sender ikke varsel til nl hvis varsel er sendt tidligere") {
            val sykmeldingId = UUID.randomUUID().toString()
            val narmesteLederId = UUID.randomUUID()
            testDb.lagreNarmesteLeder(
                NarmesteLeder(
                    narmesteLederId = narmesteLederId,
                    fnr = fnrAnsatt,
                    orgnummer = orgnummer,
                    narmesteLederFnr = fnrLeder,
                    narmesteLederTelefonnummer = "90909090",
                    narmesteLederEpost = "epost@nav.no",
                    aktivFom = LocalDate.now(),
                    arbeidsgiverForskutterer = true,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )
            testDb.lagreSendtVarsel(
                SendtVarsel(
                    sykmeldingId = sykmeldingId,
                    narmesteLederId = narmesteLederId,
                    bestillingId = UUID.randomUUID(),
                    varselType = VarselType.SENDT_SYKMELDING,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user",
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer), emptyList()),
                ),
            )

            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
        test("Sender ikke varsel til nl hvis nl ikke finnes") {
            val sykmeldingId = UUID.randomUUID().toString()
            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user",
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer), emptyList()),
                ),
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo false
            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
        test("Sender ikke varsel til nl hvis den sykmeldte har bedt om ny leder") {
            val sykmeldingId = UUID.randomUUID().toString()
            val narmesteLederId = UUID.randomUUID()
            testDb.lagreNarmesteLeder(
                NarmesteLeder(
                    narmesteLederId = narmesteLederId,
                    fnr = fnrAnsatt,
                    orgnummer = orgnummer,
                    narmesteLederFnr = fnrLeder,
                    narmesteLederTelefonnummer = "90909090",
                    narmesteLederEpost = "epost@nav.no",
                    aktivFom = LocalDate.now(),
                    arbeidsgiverForskutterer = true,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                ),
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user",
                    ),
                    SendtEvent(
                        ArbeidsgiverStatus(orgnummer),
                        listOf(
                            SporsmalOgSvarDTO("Be om ny nærmeste leder?", ShortNameDTO.NY_NARMESTE_LEDER, SvartypeDTO.JA_NEI, "JA"),
                        ),
                    ),
                ),
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo false
            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
    }
})
