package no.nav.syfo.sykmeldingvarsel.db

import no.nav.syfo.application.db.DatabaseInterface
import no.nav.syfo.sykmeldingvarsel.VarselType
import java.sql.Connection
import java.sql.Timestamp

fun DatabaseInterface.harSendtVarsel(sykmeldingId: String, varselType: VarselType): Boolean {
    return connection.use { connection ->
        connection.prepareStatement(
            """
            select * from sendt_varsel where sykmelding_id = ? and varseltype = ?;
            """
        ).use { ps ->
            ps.setString(1, sykmeldingId)
            ps.setString(2, varselType.name)
            ps.executeQuery().next()
        }
    }
}

fun DatabaseInterface.lagreSendtVarsel(sendtVarsel: SendtVarsel) {
    connection.use { connection ->
        connection.lagreSendtVarsel(sendtVarsel)
        connection.commit()
    }
}

private fun Connection.lagreSendtVarsel(sendtVarsel: SendtVarsel) {
    this.prepareStatement(
        """
                INSERT INTO sendt_varsel(
                    sykmelding_id,
                    narmeste_leder_id,
                    bestilling_id,
                    varseltype,
                    timestamp)
                VALUES (?, ?, ?, ?, ?);
                 """
    ).use {
        it.setString(1, sendtVarsel.sykmeldingId)
        it.setObject(2, sendtVarsel.narmesteLederId)
        it.setObject(3, sendtVarsel.bestillingId)
        it.setString(4, sendtVarsel.varselType.name)
        it.setTimestamp(5, Timestamp.from(sendtVarsel.timestamp.toInstant()))
        it.execute()
    }
}
