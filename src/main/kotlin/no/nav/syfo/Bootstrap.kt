package no.nav.syfo

import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.ktor.util.KtorExperimentalAPI
import io.prometheus.client.hotspot.DefaultExports
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import no.nav.doknotifikasjon.schemas.NotifikasjonMedkontaktInfo
import no.nav.syfo.application.ApplicationServer
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.coroutine.Unbounded
import no.nav.syfo.application.createApplicationEngine
import no.nav.syfo.application.db.Database
import no.nav.syfo.kafka.aiven.KafkaUtils
import no.nav.syfo.kafka.loadBaseConfig
import no.nav.syfo.kafka.toConsumerConfig
import no.nav.syfo.kafka.toProducerConfig
import no.nav.syfo.kafkautils.JacksonKafkaDeserializer
import no.nav.syfo.narmesteleder.OppdaterNarmesteLederService
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesah
import no.nav.syfo.narmesteleder.kafka.NarmesteLederLeesahConsumerService
import no.nav.syfo.sykmeldingvarsel.SendtSykmeldingVarselService
import no.nav.syfo.sykmeldingvarsel.doknotifikasjon.DoknotifikasjonProducer
import no.nav.syfo.sykmeldingvarsel.kafka.SendtSykmelding
import no.nav.syfo.sykmeldingvarsel.kafka.SendtSykmeldingConsumerService
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.slf4j.Logger
import org.slf4j.LoggerFactory

val log: Logger = LoggerFactory.getLogger("no.nav.syfo.narmesteleder-varsel")

@KtorExperimentalAPI
fun main() {
    val env = Environment()
    val vaultSecrets = VaultSecrets()
    DefaultExports.initialize()
    val applicationState = ApplicationState()
    val database = Database(env)

    val applicationEngine = createApplicationEngine(
        env,
        applicationState
    )
    val oppdaterNarmesteLederService = OppdaterNarmesteLederService(database)
    val kafkaConsumer = KafkaConsumer(
        KafkaUtils.getAivenKafkaConfig().also { it[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "none" }.toConsumerConfig("narmesteleder-varsel", JacksonKafkaDeserializer::class),
        StringDeserializer(),
        JacksonKafkaDeserializer(NarmesteLederLeesah::class)
    )
    val narmesteLederLeesahConsumerService = NarmesteLederLeesahConsumerService(
        kafkaConsumer,
        applicationState,
        env.narmesteLederLeesahTopic,
        oppdaterNarmesteLederService
    )

    val onPremConsumerProperties = loadBaseConfig(env, vaultSecrets).toConsumerConfig(env.applicationName + "-consumer", JacksonKafkaDeserializer::class).apply {
        setProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest")
    }
    val onPremKafkaConsumer = KafkaConsumer(
        onPremConsumerProperties,
        StringDeserializer(),
        JacksonKafkaDeserializer(SendtSykmelding::class)
    )
    val kafkaProducerDoknotifikasjon = KafkaProducer<String, NotifikasjonMedkontaktInfo>(
        KafkaUtils
            .getAivenKafkaConfig().apply {
                setProperty("schema.registry.url", env.schemaRegistryUrl)
            }.toProducerConfig("${env.applicationName}-producer", valueSerializer = KafkaAvroSerializer::class, keySerializer = StringSerializer::class)
    )
    val doknotifikasjonProducer = DoknotifikasjonProducer(kafkaProducerDoknotifikasjon, env.doknotifikasjonTopic)
    val sendtSykmeldingVarselService = SendtSykmeldingVarselService(database, doknotifikasjonProducer)
    val sendtSykmeldingConsumerService = SendtSykmeldingConsumerService(
        onPremKafkaConsumer,
        sendtSykmeldingVarselService,
        env.sendtSykmeldingKafkaTopic,
        applicationState
    )

    val applicationServer = ApplicationServer(applicationEngine, applicationState)
    applicationServer.start()
    applicationState.ready = true

    startBackgroundJob(applicationState) {
        log.info("Starting narmesteleder leesah consumer")
        narmesteLederLeesahConsumerService.startConsumer()
    }
    startBackgroundJob(applicationState) {
        log.info("Starting sendt sykmelding consumer")
        sendtSykmeldingConsumerService.startConsumer()
    }
}

fun startBackgroundJob(applicationState: ApplicationState, block: suspend CoroutineScope.() -> Unit) {
    GlobalScope.launch(Dispatchers.Unbounded) {
        try {
            block()
        } catch (ex: Exception) {
            log.error("Error in background task, restarting application", ex)
            applicationState.alive = false
            applicationState.ready = false
        }
    }
}
