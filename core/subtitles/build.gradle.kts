plugins { alias(libs.plugins.kotlin.jvm) }

kotlin { jvmToolchain(17) }

dependencies {
    api(project(":core:domain"))
    testImplementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit)
}
