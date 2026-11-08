import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing: the four chili.* keys come from local.properties (git-ignored). Any missing -> release stays unsigned.
val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.isFile) f.inputStream().use { load(it) }
}
val releaseSigningKeys = listOf("chili.storeFile", "chili.storePassword", "chili.keyAlias", "chili.keyPassword")
    .associateWith { localProps.getProperty(it)?.takeIf { v -> v.isNotBlank() } }
val hasReleaseSigning = releaseSigningKeys.values.all { it != null } &&
    file(releaseSigningKeys.getValue("chili.storeFile")!!).isFile

android {
    namespace = "com.superduperlover.finance"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chili.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(releaseSigningKeys.getValue("chili.storeFile")!!)
                storePassword = releaseSigningKeys.getValue("chili.storePassword")
                keyAlias = releaseSigningKeys.getValue("chili.keyAlias")
                keyPassword = releaseSigningKeys.getValue("chili.keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core"))
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
