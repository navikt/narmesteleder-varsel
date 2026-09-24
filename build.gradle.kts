import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"

val javaVersion = JvmTarget.JVM_25

val jacksonVersion = "3.2.2"
val kluentVersion = "1.73"
val ktorVersion = "3.6.0"
val logbackVersion = "1.6.3"
val logstashEncoderVersion = "9.0"
val prometheusVersion = "0.16.0"
val mockkVersion = "1.14.11"
val nimbusdsVersion = "10.10"
val testcontainerVersion = "2.0.5"
val postgresVersion = "42.7.13"
val flywayVersion = "13.7.0"
val hikariVersion = "7.1.0"
val kafkaVersion = "4.3.1"
val avroVersion = "1.12.2"
val confluentVersion = "8.1.1"
val teamdokumenthandteringAvroSchema = "2.0.0"
val junitJupiterVersion = "6.1.3"
val ktfmtVersion = "0.56"


plugins {
    id("application")
    kotlin("jvm") version "2.4.20"
    id("com.diffplug.spotless") version "8.10.2"
    id("com.gradleup.shadow") version "9.6.1"
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

    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-call-id:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")


    implementation("org.apache.kafka:kafka-clients:$kafkaVersion")
    implementation("io.confluent:kafka-avro-serializer:$confluentVersion")
    implementation("org.apache.avro:avro:$avroVersion")
    implementation("no.nav.teamdokumenthandtering:teamdokumenthandtering-avro-schemas:$teamdokumenthandteringAvroSchema")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("tools.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("tools.jackson.dataformat:jackson-dataformat-xml:$jacksonVersion")
    implementation("tools.jackson.module:jackson-module-jaxb-annotations:$jacksonVersion")

    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    compileOnly("org.flywaydb:flyway-core:$flywayVersion")
    implementation("org.flywaydb:flyway-database-postgresql:$flywayVersion")

    testImplementation("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("com.nimbusds:nimbus-jose-jwt:$nimbusdsVersion")
    testImplementation("org.testcontainers:testcontainers-kafka:$testcontainerVersion")
    testImplementation("org.testcontainers:testcontainers-postgresql:$testcontainerVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
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

