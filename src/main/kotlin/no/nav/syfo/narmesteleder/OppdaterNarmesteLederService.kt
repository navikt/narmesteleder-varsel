package no.nav.syfo.narmesteleder

import no.nav.syfo.application.db.DatabaseInterface
import no.nav.syfo.log
import no.nav.syfo.narmesteleder.db.getNarmestelederRelasjon
import no.nav.syfo.narmesteleder.db.lagreNarmesteLeder
import no.nav.syfo.narmesteleder.db.oppdaterNarmesteLeder
import no.nav.syfo.narmesteleder.db.slettNarmesteLeder
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah
import no.nav.syfo.narmesteleder.model.toNarmesteLeder

class OppdaterNarmesteLederService(
    private val database: DatabaseInterface,
) {
    fun handterMottattNarmesteLederOppdatering(narmesteLederLeesah: NarmesteLederLeesah) {
        val narmesteLeder = database.getNarmestelederRelasjon(narmesteLederLeesah.narmesteLederId)

        if (narmesteLeder != null) {
            if (narmesteLederLeesah.aktivTom == null) {
                database.oppdaterNarmesteLeder(narmesteLederLeesah.toNarmesteLeder())
                log.info("Oppdatert narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            } else {
                database.slettNarmesteLeder(narmesteLeder.narmesteLederId)
                log.info("Slettet narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            }
        } else {
            if (narmesteLederLeesah.aktivTom == null) {
                database.lagreNarmesteLeder(narmesteLederLeesah.toNarmesteLeder())
                log.info("Lagret narmesteleder med id ${narmesteLederLeesah.narmesteLederId}")
            } else {
                log.info(
                    "Ignorerer ny inaktiv narmesteleder med id ${narmesteLederLeesah.narmesteLederId}"
                )
            }
        }
    }
}
