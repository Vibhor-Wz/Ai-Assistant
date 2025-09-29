plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("io.objectbox")
    id("com.google.gms.google-services")
    kotlin("plugin.serialization") version "1.9.10"
}

android {
    namespace = "com.bigcash.ai.vectordb"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bigcash.ai.vectordb"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE"
            excludes += "META-INF/LICENSE.txt"
            excludes += "META-INF/license.txt"
            excludes += "META-INF/NOTICE"
            excludes += "META-INF/NOTICE.txt"
            excludes += "META-INF/notice.txt"
            excludes += "META-INF/ASL2.0"
            excludes += "META-INF/*.kotlin_module"
        }
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // ObjectBox dependencies
    implementation(libs.objectbox.android)
    
    // Firebase BOM for version management
    implementation(platform(libs.firebase.bom))
    
    // Firebase dependencies
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ai)
    
    // ViewModel and Coroutines
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.generativeai)
    
    // Ktor dependencies for Gemini AI client
    implementation(libs.ktor.client.android)
    implementation(libs.ktor.client.cio)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.json)
    implementation(libs.ktor.client.serialization)
    implementation(libs.ktor.client.logging)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    // Kotlinx serialization for JSON
    implementation(libs.kotlinx.serialization.json)

    // Markwon for text rendering
    implementation(libs.core)
    implementation(libs.html)
    implementation(libs.image)
    implementation(libs.linkify)
    implementation(libs.ext.tables)
    implementation(libs.ext.strikethrough)
    implementation(libs.ext.tasklist)


    // YouTube Transcript Kotlin module
    implementation("com.github.vibhor-kulshrestha:youtube-transcript-kotlin:v1.0.5")

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.5.0")

}