package no.nav.syfo.narmesteleder.db

import no.nav.syfo.application.db.DatabaseInterface
import no.nav.syfo.narmesteleder.model.NarmesteLeder
import java.sql.Connection
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.UUID

fun DatabaseInterface.getNarmestelederRelasjon(narmestelederId: UUID): NarmesteLeder? {
    return connection.use { connection ->
        connection.prepareStatement(
            """
            select * from narmeste_leder where narmeste_leder_id = ?;
            """
        ).use { ps ->
            ps.setObject(1, narmestelederId)
            ps.executeQuery().toSingleNarmesteLeder()
        }
    }
}

private fun ResultSet.toSingleNarmesteLeder(): NarmesteLeder? {
    return when (next()) {
        true -> toNarmesteLeder()
        false -> null
    }
}

fun DatabaseInterface.slettNarmesteLeder(narmesteLederId: UUID) {
    connection.use { connection ->
        connection.slettNarmesteLeder(narmesteLederId)
        connection.commit()
    }
}

fun DatabaseInterface.oppdaterNarmesteLeder(narmesteLeder: NarmesteLeder) {
    connection.use { connection ->
        connection.oppdaterNarmesteLeder(narmesteLeder)
        connection.commit()
    }
}

fun DatabaseInterface.lagreNarmesteLeder(narmesteLeder: NarmesteLeder) {
    connection.use { connection ->
        connection.lagreNarmesteleder(narmesteLeder)
        connection.commit()
    }
}

private fun Connection.lagreNarmesteleder(narmesteLeder: NarmesteLeder) {
    this.prepareStatement(
        """
                INSERT INTO narmeste_leder(
                    narmeste_leder_id,
                    orgnummer,
                    bruker_fnr,
                    narmeste_leder_fnr,
                    narmeste_leder_telefonnummer,
                    narmeste_leder_epost,
                    arbeidsgiver_forskutterer,
                    aktiv_fom,
                    timestamp)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);
                 """
    ).use {
        it.setObject(1, narmesteLeder.narmesteLederId)
        it.setString(2, narmesteLeder.orgnummer)
        it.setString(3, narmesteLeder.fnr)
        it.setString(4, narmesteLeder.narmesteLederFnr)
        it.setString(5, narmesteLeder.narmesteLederTelefonnummer)
        it.setString(6, narmesteLeder.narmesteLederEpost)
        it.setObject(7, narmesteLeder.arbeidsgiverForskutterer)
        it.setObject(8, narmesteLeder.aktivFom)
        it.setTimestamp(9, Timestamp.from(narmesteLeder.timestamp.toInstant()))
        it.execute()
    }
}

private fun Connection.slettNarmesteLeder(narmesteLederId: UUID) =
    this.prepareStatement(
        """
            DELETE FROM narmeste_leder 
                WHERE narmeste_leder_id = ?;
            """
    ).use {
        it.setObject(1, narmesteLederId)
        it.execute()
    }

private fun Connection.oppdaterNarmesteLeder(narmesteLeder: NarmesteLeder) =
    this.prepareStatement(
        """
            UPDATE narmeste_leder 
                SET narmeste_leder_telefonnummer = ?, narmeste_leder_epost = ?, arbeidsgiver_forskutterer = ?, timestamp = ?
                WHERE narmeste_leder_id = ?;
            """
    ).use {
        it.setString(1, narmesteLeder.narmesteLederTelefonnummer)
        it.setString(2, narmesteLeder.narmesteLederEpost)
        it.setObject(3, narmesteLeder.arbeidsgiverForskutterer)
        it.setTimestamp(4, Timestamp.from(narmesteLeder.timestamp.toInstant()))
        it.setObject(5, narmesteLeder.narmesteLederId)
        it.execute()
    }

private fun ResultSet.toNarmesteLeder(): NarmesteLeder =
    NarmesteLeder(
        narmesteLederId = getObject("narmeste_leder_id", UUID::class.java),
        fnr = getString("bruker_fnr"),
        orgnummer = getString("orgnummer"),
        narmesteLederFnr = getString("narmeste_leder_fnr"),
        narmesteLederTelefonnummer = getString("narmeste_leder_telefonnummer"),
        narmesteLederEpost = getString("narmeste_leder_epost"),
        aktivFom = getTimestamp("aktiv_fom").toInstant().atOffset(ZoneOffset.UTC).toLocalDate(),
        arbeidsgiverForskutterer = getObject("arbeidsgiver_forskutterer")?.toString()?.toBoolean(),
        timestamp = getTimestamp("timestamp").toInstant().atOffset(ZoneOffset.UTC)
    )
