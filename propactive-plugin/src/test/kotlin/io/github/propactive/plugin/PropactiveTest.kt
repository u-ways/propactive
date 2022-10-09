package io.github.propactive.plugin

import io.github.propactive.plugin.Configuration.Companion.DEFAULT_BUILD_DESTINATION
import io.github.propactive.plugin.Configuration.Companion.DEFAULT_ENVIRONMENTS
import io.github.propactive.plugin.Configuration.Companion.DEFAULT_IMPLEMENTATION_CLASS
import io.github.propactive.plugin.Propactive.Companion.PROPACTIVE_GROUP
import io.github.propactive.plugin.PropactiveTest.PropactiveTasks.GENERATE_TASK
import io.github.propactive.plugin.PropactiveTest.PropactiveTasks.VALIDATE_TASK
import io.github.propactive.task.GenerateApplicationProperties
import io.github.propactive.task.GenerateApplicationPropertiesTask
import io.github.propactive.task.ValidateApplicationProperties
import io.github.propactive.task.ValidateApplicationPropertiesTask
import io.kotest.assertions.throwables.shouldNotThrow
import io.kotest.extensions.system.OverrideMode.SetOrOverride
import io.kotest.extensions.system.withSystemProperties
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.UnexpectedBuildFailure
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import java.io.File
import java.nio.file.Files
import java.util.UUID
import java.util.Properties
import kotlin.text.RegexOption.DOT_MATCHES_ALL

class PropactiveTest {
    @Nested
    @TestInstance(PER_CLASS)
    inner class Unit {
        private lateinit var underTest: Propactive
        private lateinit var target: Project
        private lateinit var configuration: Configuration
        private lateinit var extensionContainer: ExtensionContainer

        @BeforeEach
        internal fun setUp() {
            underTest = Propactive()
            configuration = mockk(relaxed = true)

            extensionContainer = mockk(relaxed = true) {
                every { findByType(Configuration::class.java) } returns configuration
            }

            target = mockk(relaxed = true) { every { extensions } returns extensionContainer }
            underTest.apply(target)
        }

        @Test
        fun `should register Configuration extension`() {
            verify {
                extensionContainer.create(PROPACTIVE_GROUP, Configuration::class.java)
            }
        }

        @ParameterizedTest
        @EnumSource(PropactiveTasks::class)
        fun `should register GenerateApplicationPropertiesTask`(task: PropactiveTasks) {
            verify {
                target.tasks.register(task.taskName, task.taskReference)
            }
        }
    }

    @Nested
    inner class Acceptance {
        private lateinit var project: Project

        @BeforeEach
        internal fun setUp() {
            project = ProjectBuilder.builder()
                .withName("temporary-project-${UUID.randomUUID()}")
                .build()
                .also { p -> p.plugins.apply(Propactive::class.java) }
        }

        @Test
        fun `should register task to generate application properties`() {
            project
                .getTasksByName(GenerateApplicationPropertiesTask.TASK_NAME, false)
                .shouldNotBeEmpty()
                .first()
                .shouldBeInstanceOf<GenerateApplicationPropertiesTask>()
                .apply {
                    group shouldBe PROPACTIVE_GROUP
                    description shouldBe GenerateApplicationPropertiesTask.TASK_DESCRIPTION
                }
        }

        @Test
        fun `should register task to validate application properties`() {
            project
                .getTasksByName(ValidateApplicationPropertiesTask.TASK_NAME, false)
                .shouldNotBeEmpty()
                .first()
                .shouldBeInstanceOf<ValidateApplicationPropertiesTask>()
                .apply {
                    group shouldBe PROPACTIVE_GROUP
                    description shouldBe ValidateApplicationPropertiesTask.TASK_DESCRIPTION
                }
        }

        @Test
        fun `should register task extension to configure propactive`() {
            project
                .extensions
                .findByType(Configuration::class.java)
                .shouldNotBeNull()
        }

        @Test
        fun `should provide sane propactive configuration defaults`() {
            project
                .extensions
                .findByType(Configuration::class.java)!!
                .apply {
                    destination shouldBe DEFAULT_BUILD_DESTINATION
                    environments shouldBe DEFAULT_ENVIRONMENTS
                    implementationClass shouldBe DEFAULT_IMPLEMENTATION_CLASS
                }
        }

        @Test
        fun `should allow modifying propactive configurations`() {
            val customDestination = "custom/path"
            val customEnvironments = "test"
            val customImplementationClass = "io.github.propactive.Test"

            project
                .extensions
                .findByType(Configuration::class.java)!!
                .apply {
                    destination = customDestination
                    environments = customEnvironments
                    implementationClass = customImplementationClass
                }

            project
                .extensions
                .findByType(Configuration::class.java)!!
                .apply {
                    destination shouldBe customDestination
                    environments shouldBe customEnvironments
                    implementationClass shouldBe customImplementationClass
                }
        }

        @Test
        fun `should allow setting propactive configurations through system properties`() {
            val customDestination = "custom/path"
            val customEnvironments = "test"
            val customImplementationClass = "io.github.propactive.Test"
            val customFilename = "customFilename"

            val properties = Properties().apply {
                put(Configuration::destination.name, customDestination)
                put(Configuration::environments.name, customEnvironments)
                put(Configuration::implementationClass.name, customImplementationClass)
                put(Configuration::filenameOverride.name, customFilename)
            }

            withSystemProperties(properties, SetOrOverride) {
                project
                    .extensions
                    .findByType(Configuration::class.java)!!
                    .apply {
                        destination = customDestination
                        environments = customEnvironments
                        implementationClass = customImplementationClass
                        filenameOverride = customFilename
                    }
            }
        }

        @Test
        fun `should allow overriding propactive custom configurations with system properties`() {
            val customConfigDestination = "custom/path/config"
            val customConfigEnvironments = "testConfig"
            val customConfigImplementationClass = "io.github.propactive.TestConfig"

            val customPropertyDestination = "custom/path/Property"
            val customPropertyEnvironments = "testProperty"
            val customPropertyImplementationClass = "io.github.propactive.TestProperty"

            project
                .extensions
                .findByType(Configuration::class.java)!!
                .apply {
                    destination = customConfigDestination
                    environments = customConfigEnvironments
                    implementationClass = customConfigImplementationClass
                }

            project
                .extensions
                .findByType(Configuration::class.java)!!
                .apply {
                    destination shouldBe customConfigDestination
                    environments shouldBe customConfigEnvironments
                    implementationClass shouldBe customConfigImplementationClass
                }

            val properties = Properties().apply {
                put(Configuration::destination.name, customPropertyDestination)
                put(Configuration::environments.name, customPropertyEnvironments)
                put(Configuration::implementationClass.name, customPropertyImplementationClass)
            }

            withSystemProperties(properties, SetOrOverride) {
                project
                    .extensions
                    .findByType(Configuration::class.java)!!
                    .apply {
                        destination = customPropertyDestination
                        environments = customPropertyEnvironments
                        implementationClass = customPropertyImplementationClass
                    }
            }
        }
    }

    @Nested
    @TestInstance(PER_CLASS)
    inner class Integration {
        private lateinit var projectDir: File

        @BeforeEach
        fun setUp() {
            projectDir = Files
                .createTempDirectory("projectDir")
                .toFile()
        }

        @AfterEach
        fun tearDown() {
            projectDir.delete()
        }

        @Test
        fun `should display propactive tasks description`() {
            projectDir.also { parent ->
                File(parent, "build.gradle.kts")
                    .apply {
                        writeText(
                            """
                            | plugins {
                            |     id("io.github.propactive") version "DEV-SNAPSHOT"
                            | }
                            """.trimMargin(),
                        )
                    }
            }

            GradleRunner
                .create()
                .withProjectDir(projectDir)
                .withArguments("tasks")
                .withPluginClasspath()
                .build()
                .apply {
                    output shouldContain """
                    Propactive tasks
                    ----------------
                    ${GENERATE_TASK.taskName} - .*?

                    ${VALIDATE_TASK.taskName} - .*?
                    """.trimIndent().toRegex(DOT_MATCHES_ALL)
                }
        }

        @Test
        fun `should register configuration extension for propactive plugin`() {
            projectDir.also { parent ->
                File(parent, "build.gradle.kts")
                    .apply {
                        writeText(
                            """
                            | plugins {
                            |     id("io.github.propactive") version "DEV-SNAPSHOT"
                            | }
                            |
                            | $PROPACTIVE_GROUP {
                            |     ${Configuration::destination.name} = "$DEFAULT_BUILD_DESTINATION"
                            |     ${Configuration::implementationClass.name} = "$DEFAULT_IMPLEMENTATION_CLASS"
                            |     ${Configuration::environments.name} = "$DEFAULT_ENVIRONMENTS"
                            | }
                            """.trimMargin(),
                        )
                    }
            }

            shouldNotThrow<UnexpectedBuildFailure> {
                GradleRunner
                    .create()
                    .withProjectDir(projectDir)
                    .withPluginClasspath()
                    .build()
            }
        }
    }

    @Suppress("unused")
    enum class PropactiveTasks(val taskName: String, val taskReference: Class<out DefaultTask>) {
        GENERATE_TASK(
            GenerateApplicationProperties::class.simpleName!!.replaceFirstChar(Char::lowercaseChar),
            GenerateApplicationPropertiesTask::class.java,
        ),
        VALIDATE_TASK(
            ValidateApplicationProperties::class.simpleName!!.replaceFirstChar(Char::lowercaseChar),
            ValidateApplicationPropertiesTask::class.java,
        ),
    }
}
