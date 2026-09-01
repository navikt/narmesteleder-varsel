package no.nav.syfo.kafkautils

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JacksonKafkaDeserializerTest {
    @Test
    internal fun `deserializes Java time values and ignores unknown properties`() {
        val id = UUID.randomUUID()
        val data =
            """
            {
              "narmesteLederId": "$id",
              "fnr": "12345678910",
              "orgnummer": "123456789",
              "narmesteLederFnr": "10987654321",
              "narmesteLederTelefonnummer": "12345678",
              "narmesteLederEpost": "leder@example.com",
              "aktivFom": "2026-01-01",
              "aktivTom": "",
              "arbeidsgiverForskutterer": true,
              "timestamp": "2026-01-01T12:00:00Z",
              "unknown": "ignored"
            }
            """.trimIndent().encodeToByteArray()

        val actual = JacksonKafkaDeserializer(NarmesteLederLeesah::class).deserialize("topic", data)

        assertEquals(id, actual.narmesteLederId)
        assertEquals(LocalDate.parse("2026-01-01"), actual.aktivFom)
        assertEquals(null, actual.aktivTom)
        assertEquals(OffsetDateTime.parse("2026-01-01T12:00:00Z"), actual.timestamp)
    }
}
