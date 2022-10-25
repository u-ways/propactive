@file:Suppress("UnstableApiUsage")

include(
    ":propactive-plugin",
    ":propactive-jvm",
)

dependencyResolutionManagement {
    enableFeaturePreview("VERSION_CATALOGS")
    versionCatalogs {
        create("libs") {
            val kotlinVersion = "1.6.20"
            val serializationVersion = "1.4.1"
            val mockkVersion = "1.13.2"
            val kotestVersion = "5.2.2"
            val junitVersion = "5.8.2"
            val ktlintVersion = "11.0.0"
            val publishVersion = "0.21.0"
            val equalsVersion = "3.10"

            library("kotlin-reflect", "org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
            library("kotlin-stdlib", "org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
            library("kotlinx-serialization-json", "org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")
            library("mockk", "io.mockk:mockk:$mockkVersion")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:$kotestVersion")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:$kotestVersion")
            library("junit-jupiter", "org.junit.jupiter:junit-jupiter:$junitVersion")
            library("equalsverifier", "nl.jqno.equalsverifier:equalsverifier:$equalsVersion")

            plugin("jetbrains-kotlin-jvm", "org.jetbrains.kotlin.jvm").version(kotlinVersion)
            plugin("jetbrains-dokka", "org.jetbrains.dokka").version(kotlinVersion)
            plugin("gradle-ktlint", "org.jlleitschuh.gradle.ktlint").version(ktlintVersion)
            plugin("gradle-publish", "com.gradle.plugin-publish").version(publishVersion)
        }
    }
}
