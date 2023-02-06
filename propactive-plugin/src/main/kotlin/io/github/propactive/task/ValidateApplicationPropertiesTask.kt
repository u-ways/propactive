package io.github.propactive.task

import io.github.propactive.environment.EnvironmentFactory
import io.github.propactive.plugin.Configuration
import io.github.propactive.plugin.Configuration.Companion.DEFAULT_ENVIRONMENTS
import io.github.propactive.plugin.Configuration.Companion.DEFAULT_IMPLEMENTATION_CLASS
import io.github.propactive.plugin.Propactive.Companion.PROPACTIVE_GROUP
import org.gradle.api.tasks.TaskAction

open class ValidateApplicationPropertiesTask : ApplicationPropertiesTask() {
    companion object {
        internal val TASK_NAME =
            ValidateApplicationPropertiesTask::class.simpleName!!
                .replaceFirstChar(Char::lowercaseChar)
                .removeSuffix("Task")

        internal val TASK_DESCRIPTION = """
            |Validates the application properties without generating any files.
            |
            |  Optional configurations:
            |    -P${Configuration::environments.name}
            |        Description: Comma separated list of environments to generate the properties for.
            |        Example: "test,stage,prod"
            |        Default: "$DEFAULT_ENVIRONMENTS" (All provided environments)
            |    -P${Configuration::implementationClass.name}
            |        Description: Sets the location of your properties object.
            |        Example: "com.package.path.to.your.ApplicationProperties"
            |        Default: "$DEFAULT_IMPLEMENTATION_CLASS" (At the root of your project)
            |
        """.trimMargin()
    }

    @TaskAction
    fun run() = compiledClasses
        .find(implementationClass)
        .run(EnvironmentFactory::create)

    init {
        group = PROPACTIVE_GROUP
        description = TASK_DESCRIPTION
    }
}
