package no.nav.syfo.testutils

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import no.nav.syfo.application.db.DatabaseInterface
import org.flywaydb.core.Flyway
import java.sql.Connection

class TestDB : DatabaseInterface {
    private var pg: EmbeddedPostgres? = null
    override val connection: Connection
        get() = pg!!.postgresDatabase.connection.apply { autoCommit = false }

    init {
        pg = EmbeddedPostgres.start()
        pg!!.postgresDatabase.connection.use {
            connection ->
            connection.prepareStatement("create role cloudsqliamuser;").executeUpdate()
        }
        Flyway.configure().run {
            dataSource(pg?.postgresDatabase).load().migrate()
        }
    }

    fun stop() {
        pg?.close()
    }
}

fun Connection.dropData() {
    use { connection ->
        connection.prepareStatement("DELETE FROM narmeste_leder").executeUpdate()
        connection.prepareStatement("DELETE FROM sendt_varsel").executeUpdate()
        connection.commit()
    }
}
