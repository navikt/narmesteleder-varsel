group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.7.3"
val jacksonVersion = "2.15.2"
val kluentVersion = "1.73"
val ktorVersion = "2.3.4"
val logbackVersion = "1.4.11"
val logstashEncoderVersion = "7.4"
val prometheusVersion = "0.16.0"
val kotestVersion = "5.7.2"
val smCommonVersion = "1.0.10"
val mockkVersion = "1.13.7"
val nimbusdsVersion = "9.31"
val testContainerVersion = "1.19.0"
val postgresVersion = "42.6.0"
val flywayVersion = "9.22.0"
val hikariVersion = "5.0.1"
val kafkaVersion = "3.3.1"
val avroVersion = "1.11.2"
val confluentVersion = "7.5.0"
val doknotifikasjonAvroVersion = "1.2021.06.22-11.27-265ce1fe1ab4"
val kotlinVersion = "1.9.10"
val ktfmtVersion = "0.44"


plugins {
    id("application")
    kotlin("jvm") version "1.9.10"
    id("com.diffplug.spotless") version "6.21.0"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("no.nav.syfo.BootstrapKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven(url = "https://jitpack.io")
}


dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-slf4j:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")

    implementation("no.nav.helse:syfosm-common-kafka:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-models:$smCommonVersion")

    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("com.github.navikt:doknotifikasjon-schemas:$doknotifikasjonAvroVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    implementation("org.flywaydb:flyway-core:$flywayVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
}


tasks {

    shadowJar {
        archiveBaseName.set("app")
        archiveClassifier.set("")
        isZip64 = true
        manifest {
            attributes(
                mapOf(
                    "Main-Class" to "no.nav.syfo.BootstrapKt",
                ),
            )
        }
    }

    test {
        useJUnitPlatform {}
        testLogging.showStandardStreams = true
    }


    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

}

