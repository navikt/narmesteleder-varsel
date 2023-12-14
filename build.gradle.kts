group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.7.3"
val jacksonVersion = "2.16.0"
val kluentVersion = "1.73"
val ktorVersion = "2.3.7"
val logbackVersion = "1.4.14"
val logstashEncoderVersion = "7.4"
val prometheusVersion = "0.16.0"
val kotestVersion = "5.8.0"
val smCommonVersion = "2.0.7"
val mockkVersion = "1.13.8"
val nimbusdsVersion = "9.37.3"
val testContainerVersion = "1.19.3"
val postgresVersion = "42.7.1"
val flywayVersion = "10.3.0"
val hikariVersion = "5.1.0"
val kafkaVersion = "3.6.1"
val avroVersion = "1.11.3"
val confluentVersion = "7.5.2"
val doknotifikasjonAvroVersion = "1.2021.06.22-11.27-265ce1fe1ab4"
val kotlinVersion = "1.9.21"
val ktfmtVersion = "0.44"
val snappyJavaVersion = "1.1.10.5"
val commonsCodecVersion = "1.16.0"

plugins {
    id("application")
    kotlin("jvm") version "1.9.21"
    id("com.diffplug.spotless") version "6.23.3"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

application {
    mainClass.set("no.nav.syfo.BootstrapKt")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

repositories {
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
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
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")
    //due to https://github.com/advisories/GHSA-qcwq-55hx-v3vh
    implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion")
    // override transient version from io.ktor:ktor-server-test-host due to security vulnerability
    // https://devhub.checkmarx.com/cve-details/Cxeb68d52e-5509/
    implementation("commons-codec:commons-codec:$commonsCodecVersion")

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

