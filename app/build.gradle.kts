import java.util.Properties
import kotlin.apply

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    id("kotlin-parcelize")
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.ncert7.mathandsciencelab"
    compileSdk =36

    defaultConfig {
        applicationId = "com.ncert7.mathandsciencelab"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        val localProps = Properties().apply {
            val f = rootProject.file("local.properties")
            if (f.exists()) load(f.inputStream())
        }
        fun prop(name: String, default: String = ""): String = localProps.getProperty(name, default)
        buildConfigField("String", "AUTH_KEY", "\"${prop("AUTH_KEY")}\"")
        buildConfigField("String", "ADMOB_APP_ID", "\"${prop("ADMOB_APP_ID")}\"")
        buildConfigField("String", "BANNER_AD_UNIT_ID", "\"${prop("BANNER_AD_UNIT_ID")}\"")
        // Manifest placeholders for runtime value substitution
        manifestPlaceholders["ADMOB_APP_ID"] = prop("ADMOB_APP_ID")

    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.googleid)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.lifecycle.process)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // Firebase BoM
    implementation(platform(libs.firebase.bom))

    // Firebase Libraries
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics)
    implementation("androidx.credentials:credentials-play-services-auth:1.5.0")
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Material3 and Material Icons
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    // Image Loading
    implementation(libs.glide)
    implementation(libs.glide.compose)

    // Google Play In-App Update
    implementation("com.google.android.play:app-update:2.1.0")

    // Google Mobile Ads SDK
    implementation("com.google.android.gms:play-services-ads:22.6.0")
}