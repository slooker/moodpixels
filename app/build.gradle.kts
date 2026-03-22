import java.util.Properties
import java.io.FileInputStream
import java.io.File

val versionPropsFile = rootProject.file("version.properties")
val versionProps = Properties()
if (versionPropsFile.exists()) {
    versionProps.load(FileInputStream(versionPropsFile))
}

val isReleaseBuild = gradle.startParameter.taskNames.any {
    (it.contains("bundle", ignoreCase = true) || it.contains("assemble", ignoreCase = true)) &&
            it.contains("release", ignoreCase = true)
}
if (isReleaseBuild) {
    val newCode = ((versionProps["VERSION_CODE"] as String?)?.toInt() ?: 0) + 1
    versionProps["VERSION_CODE"] = newCode.toString()
    versionProps.store(versionPropsFile.outputStream(), null)
}

val appVersionCode = (versionProps["VERSION_CODE"] as String?)?.toInt() ?: 1
val appPackageName = "us.slooker.moodpixels"

// Controls filename and output-metadata.json — must be before plugins {}
base.archivesName.set("moodpixels-v${appVersionCode}")

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

// Load signing credentials from keystore.properties (not committed to git)
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = appPackageName
    compileSdk = 35

    defaultConfig {
        applicationId = appPackageName
        minSdk = 24
        targetSdk = 35
        versionCode = appVersionCode
        versionName = "1.0.$appVersionCode"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = keystoreProperties["storeFile"]?.let { rootProject.file(it) }
            storePassword = keystoreProperties["storePassword"] as? String
            keyAlias = keystoreProperties["keyAlias"] as? String
            keyPassword = keystoreProperties["keyPassword"] as? String
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }

    applicationVariants.all {
        val variant = this
        variant.outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "moodpixels-v${variant.versionCode}-${variant.buildType.name}.apk"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.activity.ktx)
    implementation(libs.fragment.ktx)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.runtime.ktx)

    implementation(libs.coroutines.android)
    implementation(libs.gson)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
