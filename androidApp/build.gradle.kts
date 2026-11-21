import com.android.build.api.variant.ApplicationAndroidComponentsExtension

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

/**
 * Packages the Compose Multiplatform string bundles from :shared into this APK's assets.
 *
 * Compose Multiplatform only wires its own copy-to-assets task for the legacy
 * `com.android.library` + `androidTarget()` setup. Under AGP 9's
 * `com.android.kotlin.multiplatform.library` the resources are produced but never packaged, and
 * every stringResource() then fails at runtime with MissingResourceException. The task the plugin
 * registers for this is internal, so rather than reaching into it we re-stage the prepared
 * resources ourselves under the path the runtime reader expects.
 *
 * Delete this and the androidComponents block below once Compose Multiplatform supports the
 * new Android KMP DSL.
 */
abstract class StageComposeResources : DefaultTask() {

    @get:InputDirectory
    abstract val preparedResources: DirectoryProperty

    @get:Input
    abstract val resourcePackage: Property<String>

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystem: FileSystemOperations

    @TaskAction
    fun stage() {
        fileSystem.sync {
            from(preparedResources)
            into(outputDirectory.dir("composeResources/${resourcePackage.get()}"))
        }
    }
}

val stageComposeResources = tasks.register<StageComposeResources>("stageComposeResources") {
    dependsOn(":shared:prepareComposeResourcesTaskForCommonMain")
    preparedResources.set(
        project(":shared").layout.buildDirectory.dir(
            "generated/compose/resourceGenerator/preparedResources/commonMain/composeResources"
        )
    )
    resourcePackage.set("ch.bfh.anchor.resources")
    outputDirectory.set(layout.buildDirectory.dir("generated/composeResourcesAssets"))
}

extensions.configure<ApplicationAndroidComponentsExtension> {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            stageComposeResources,
            StageComposeResources::outputDirectory
        )
    }
}

android {
    namespace = "ch.bfh.anchor.android"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "ch.bfh.anchor"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)

    debugImplementation(compose.uiTooling)
}
