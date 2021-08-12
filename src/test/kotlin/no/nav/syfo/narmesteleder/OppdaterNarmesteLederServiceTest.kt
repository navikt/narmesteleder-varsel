package no.nav.syfo.narmesteleder

import no.nav.syfo.narmesteleder.db.getNarmestelederRelasjon
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah
import no.nav.syfo.testutils.TestDB
import no.nav.syfo.testutils.dropData
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldNotBeEqualTo
import org.spekframework.spek2.Spek
import org.spekframework.spek2.style.specification.describe
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID

class OppdaterNarmesteLederServiceTest : Spek({
    val testDb = TestDB()
    val oppdaterNarmesteLederService = OppdaterNarmesteLederService(testDb)

    afterEachTest {
        testDb.connection.dropData()
    }
    afterGroup {
        testDb.stop()
    }
    describe("OppdaterNarmesteLederService") {
        it("Oppretter ny nærmeste leder hvis den ikke finnes fra før og er aktiv") {
            val narmesteLederId = UUID.randomUUID()
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId))

            val narmesteLeder = testDb.getNarmestelederRelasjon(narmesteLederId)
            narmesteLeder shouldNotBeEqualTo null
        }
        it("Ignorerer melding om ny nærmeste leder hvis den ikke finnes fra før og er inaktiv") {
            val narmesteLederId = UUID.randomUUID()
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId, aktivTom = LocalDate.now()))

            val narmesteLeder = testDb.getNarmestelederRelasjon(narmesteLederId)
            narmesteLeder shouldBeEqualTo null
        }
        it("Oppdaterer nærmeste leder hvis den finnes fra før og er aktiv") {
            val narmesteLederId = UUID.randomUUID()
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId))
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId, telefonnummer = "98989898", epost = "mail@banken.no"))

            val narmesteLeder = testDb.getNarmestelederRelasjon(narmesteLederId)
            narmesteLeder shouldNotBeEqualTo null
            narmesteLeder?.narmesteLederTelefonnummer shouldBeEqualTo "98989898"
            narmesteLeder?.narmesteLederEpost shouldBeEqualTo "mail@banken.no"
        }
        it("Sletter nærmeste leder hvis den finnes fra før og er inaktiv") {
            val narmesteLederId = UUID.randomUUID()
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId))
            oppdaterNarmesteLederService.handterMottattNarmesteLederOppdatering(getNarmesteLederLeesah(narmesteLederId, aktivTom = LocalDate.now()))

            val narmesteLeder = testDb.getNarmestelederRelasjon(narmesteLederId)
            narmesteLeder shouldBeEqualTo null
        }
    }
})

fun getNarmesteLederLeesah(
    narmesteLederId: UUID,
    telefonnummer: String = "90909090",
    epost: String = "test@nav.no",
    aktivTom: LocalDate? = null
): NarmesteLederLeesah =
    NarmesteLederLeesah(
        narmesteLederId = narmesteLederId,
        fnr = "12345678910",
        orgnummer = "999999",
        narmesteLederFnr = "01987654321",
        narmesteLederTelefonnummer = telefonnummer,
        narmesteLederEpost = epost,
        aktivFom = LocalDate.now(),
        aktivTom = aktivTom,
        arbeidsgiverForskutterer = true,
        timestamp = OffsetDateTime.now(ZoneOffset.UTC)
    )
