import kotlinx.coroutines.flow.merge
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_21

val coroutinesVersion = "1.10.2"
val jacksonVersion = "2.20.1"
val kluentVersion = "1.73"
val ktorVersion = "3.3.3"
val logbackVersion = "1.5.21"
val logstashEncoderVersion = "9.0"
val prometheusVersion = "0.16.0"
val mockkVersion = "1.14.6"
val nimbusdsVersion = "10.6"
val testContainerVersion = "1.21.3"
val postgresVersion = "42.7.8"
val flywayVersion = "11.18.0"
val hikariVersion = "7.0.2"
val kafkaVersion = "3.9.1"
val avroVersion = "1.12.1"
val confluentVersion = "8.1.1"
val teamdokumenthandteringAvroSchema = "1.1.7"
val kotlinVersion = "2.2.21"
val junitJupiterVersion = "6.0.1"
val ktfmtVersion = "0.44"

//Due to vulnerabilities
val nettyCommonVersion = "4.2.7.Final"
val snappyJavaVersion = "1.1.10.8"
val commonsCodecVersion = "1.20.0"

plugins {
    id("application")
    kotlin("jvm") version "2.2.21"
    id("com.diffplug.spotless") version "8.1.0"
    id("com.gradleup.shadow") version "9.2.2"
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
    constraints {
        implementation("io.netty:netty-common:$nettyCommonVersion") {
            because("override transient from io.ktor:ktor-server-netty")
        }
    }
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")


    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$teamdokumenthandteringAvroSchema")

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
    constraints {
        implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion") {
            because("due to https://github.com/advisories/GHSA-qcwq-55hx-v3vh")
        }
    }

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("org.testcontainers:kafka:$testContainerVersion")
    testImplementation("org.testcontainers:postgresql:$testContainerVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
    constraints {
        testImplementation("commons-codec:commons-codec:$commonsCodecVersion") {
            because("override transient version from io.ktor:ktor-server-test-host due to security vulnerability\n" +
                "    // https://devhub.checkmarx.com/cve-details/Cxeb68d52e-5509/")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = javaVersion
    }
}


tasks {

    shadowJar {
        mergeServiceFiles {

        }
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
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }


    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }

}

