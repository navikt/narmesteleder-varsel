package no.nav.syfo.testutils

import io.mockk.every
import io.mockk.mockk
import no.nav.syfo.Environment
import no.nav.syfo.application.db.Database
import no.nav.syfo.application.db.DatabaseInterface
import org.testcontainers.containers.PostgreSQLContainer

class PsqlContainer : PostgreSQLContainer<PsqlContainer>("postgres:12")

class TestDB private constructor() {

    companion object {
        val database: DatabaseInterface
        val env = mockk<Environment>()
        val psqlContainer: PsqlContainer = PsqlContainer()
            .withExposedPorts(5432)
            .withUsername("username")
            .withPassword("password")
            .withDatabaseName("database")
            .withInitScript("db/testdb-init.sql")

        init {
            psqlContainer.start()
            every { env.databasePassword } returns "password"
            every { env.databaseUsername } returns "username"
            every { env.jdbcUrl() } returns psqlContainer.jdbcUrl
            database = Database(env)
        }

        fun dropData() {
            database.connection.use { connection ->
                connection.prepareStatement("DELETE FROM narmeste_leder").executeUpdate()
                connection.prepareStatement("DELETE FROM sendt_varsel").executeUpdate()
                connection.commit()
            }
        }
    }

//        Flyway.configure().run {
//            dataSource(pg?.postgresDatabase).load().migrate()
//        }
}
