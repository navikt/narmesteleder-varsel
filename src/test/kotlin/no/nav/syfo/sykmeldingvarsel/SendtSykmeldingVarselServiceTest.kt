package no.nav.syfo.sykmeldingvarsel

import io.mockk.clearMocks
import io.mockk.mockk
import io.mockk.verify
import no.nav.syfo.model.sykmeldingstatus.KafkaMetadataDTO
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
import no.nav.syfo.testutils.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class SendtSykmeldingVarselServiceTest : Spek({
    val testDb = TestDB()
    val doknotifikasjonProducer = mockk<DoknotifikasjonProducer>(relaxed = true)
    val sendtSykmeldingVarselService = SendtSykmeldingVarselService(testDb, doknotifikasjonProducer)

    val fnrAnsatt = "12345678910"
    val fnrLeder = "01987654321"
    val orgnummer = "999000"

    afterEachTest {
        testDb.connection.dropData()
        clearMocks(doknotifikasjonProducer)
    }
    afterGroup {
        testDb.stop()
    }
    describe("SendtSykmeldingVarselService") {
        it("Sender varsel til nl hvis nl finnes og varsel ikke er sendt før") {
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
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                )
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user"
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer))
                )
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo true
            verify { doknotifikasjonProducer.send(any(), any()) }
        }
        it("Sender ikke varsel hvis nl gjelder annet arbeidsforhold") {
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
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                )
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user"
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer = "999888"))
                )
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo false
            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
        it("Sender ikke varsel til nl hvis varsel er sendt tidligere") {
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
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                )
            )
            testDb.lagreSendtVarsel(
                SendtVarsel(
                    sykmeldingId = sykmeldingId,
                    narmesteLederId = narmesteLederId,
                    bestillingId = UUID.randomUUID(),
                    varselType = VarselType.SENDT_SYKMELDING,
                    timestamp = OffsetDateTime.now(ZoneOffset.UTC)
                )
            )

            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user"
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer))
                )
            )

            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
        it("Sender ikke varsel til nl hvis nl ikke finnes") {
            val sykmeldingId = UUID.randomUUID().toString()
            sendtSykmeldingVarselService.handterSendtSykmelding(
                SendtSykmelding(
                    KafkaMetadataDTO(
                        sykmeldingId = sykmeldingId,
                        timestamp = OffsetDateTime.now(ZoneOffset.UTC),
                        fnr = fnrAnsatt,
                        source = "user"
                    ),
                    SendtEvent(ArbeidsgiverStatus(orgnummer))
                )
            )

            testDb.harSendtVarsel(sykmeldingId, VarselType.SENDT_SYKMELDING) shouldBeEqualTo false
            verify(exactly = 0) { doknotifikasjonProducer.send(any(), any()) }
        }
    }
})
