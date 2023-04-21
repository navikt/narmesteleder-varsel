package no.nav.syfo.narmesteleder.model

import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah

fun NarmesteLederLeesah.toNarmesteLeder(): NarmesteLeder =
    NarmesteLeder(
        narmesteLederId = narmesteLederId,
        fnr = fnr,
        orgnummer = orgnummer,
        narmesteLederFnr = narmesteLederFnr,
        narmesteLederTelefonnummer = narmesteLederTelefonnummer,
        narmesteLederEpost = narmesteLederEpost,
        aktivFom = aktivFom,
        arbeidsgiverForskutterer = arbeidsgiverForskutterer,
        timestamp = timestamp,
    )
