package no.nav.syfo.sykmeldingvarsel.doknotifikasjon

import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo
import no.nav.doknotifikasjon.schemas.PrefererteKanal
import no.nav.syfo.narmesteleder.model.NarmesteLeder

const val SMS_TEKST = """
Hei!
Du har fått tilgang til sykmeldingen til en av dine ansatte.
Logg inn på DineSykmeldte for å se sykmeldingen. 
Vennlig hilsen NAV
"""
const val EPOST_TEKST = """
<!DOCTYPE html>
<html>
<head>
<title>syfoNaermesteLederNySykmelding</title>
</head>
<body>
<p>Hei.</p>
<p>Du har fått tilgang til sykmeldingen til en av dine ansatte fordi du er meldt inn som nærmeste leder med personalansvar.</p>
<p>Logg inn på DineSykmeldte for å se sykmeldingen.</p>
<p>Du må logge inn med BankID eller tilsvarende for at vi skal være sikre på at sykmeldingen kommer fram til rett person.</p>
<p>Vennlig hilsen</p>
<p> NAV</p>
</body>
</html>
"""

fun tilNotifikasjonMedkontaktInfo(bestillingsId: String, narmesteLeder: NarmesteLeder): NotifikasjonMedkontaktInfo {
    return NotifikasjonMedkontaktInfo(
        bestillingsId,
        "narmesteleder-varsel",
        narmesteLeder.narmesteLederFnr,
        narmesteLeder.narmesteLederTelefonnummer,
        narmesteLeder.narmesteLederEpost,
        0,
        0,
        "Sykmeldt arbeidstaker",
        EPOST_TEKST,
        SMS_TEKST,
        listOf(PrefererteKanal.EPOST)
    )
}
