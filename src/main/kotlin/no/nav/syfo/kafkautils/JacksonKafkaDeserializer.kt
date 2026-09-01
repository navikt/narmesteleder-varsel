package no.nav.syfo.kafkautils

import kotlin.reflect.KClass
import org.apache.kafka.common.serialization.Deserializer
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.datatype.jsr310.JavaTimeModule
import tools.jackson.module.kotlin.KotlinModule

class JacksonKafkaDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {
    private val objectMapper =
        JsonMapper.builder()
            .addModule(KotlinModule.Builder().build())
            .addModule(JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT)
            .build()

    override fun configure(configs: MutableMap<String, *>, isKey: Boolean) {}

    override fun deserialize(topic: String?, data: ByteArray): T {
        return objectMapper.readValue(data, type.java)
    }

    override fun close() {}
}
