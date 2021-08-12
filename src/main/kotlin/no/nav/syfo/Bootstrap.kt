package no.nav.syfo

import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.coroutine.Unbounded
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.db.Database
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafkautils.JacksonKafkaDeserializer
import no.nav.syfo.narmesteleder.OppdaterNarmesteLederService
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesahConsumerService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.StringDeserializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.narmesteleder-varsel")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val database = Database(env)

    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val oppdaterNarmesteLederService = OppdaterNarmesteLederService(database)
    val kafkaConsumer = KafkaConsumer(
        KafkaUtils.getAivenKafkaConfig().also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest" }.toConsumerConfig("narmesteleder-varsel", JacksonKafkaDeserializer::class),
        StringDeserializer(),
        JacksonKafkaDeserializer(NarmesteLederLeesah::class)
    )
    val narmesteLederLeesahConsumerService = NarmesteLederLeesahConsumerService(
        kafkaConsumer,
        applicationState,
        env.narmesteLederLeesahTopic,
        oppdaterNarmesteLederService
    )
    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true

    startBackgroundJob(applicationState) {
        log.info("Starting narmesteleder leesah consumer")
        narmesteLederLeesahConsumerService.startConsumer()
    }
}

fun startBackgroundJob(applicationState: ApplicationState, block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch(Dispatchers.Unbounded) {
        try {
            block()
        } catch (ex: Exception) {
            log.error("Error in background task, restarting application")
            applicationState.alive = false
            applicationState.ready = false
        }
    }
}
