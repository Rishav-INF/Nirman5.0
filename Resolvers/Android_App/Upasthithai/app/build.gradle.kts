plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.android.libraries.mapsplatform.secrets.gradle.plugin)
    id("com.google.devtools.ksp") version "1.9.0-1.0.13"
}


android {
    namespace = "com.example.upasthithai"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.upasthithai"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
//        buildConfigField("String", "API_KEY", "\"${secretProperties["API_KEY"]}\"")
//        buildConfigField("String", "GEMINI_API_KEY", "\"${secretProperties["GEMINI_API_KEY"]}\"")



        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
        viewBinding = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force(
            "androidx.camera:camera-core:1.3.3",
            "androidx.camera:camera-camera2:1.3.3",
            "androidx.camera:camera-lifecycle:1.3.3",
            "androidx.camera:camera-view:1.3.3",
            "androidx.camera:camera-extensions:1.3.3",
            "androidx.camera:camera-video:1.3.3",
            "androidx.camera:camera-mlkit-vision:1.3.3",
            "androidx.camera:camera-encoding:1.3.3",
            "androidx.camera.featurecombinationquery:featurecombinationquery:1.3.3"
        )
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation("com.airbnb.android:lottie:6.5.2")
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.material:material:1.8.0")
    implementation(libs.androidx.appcompat)
    implementation(libs.firebase.database)
    implementation(libs.play.services.maps)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx")
    implementation("com.google.ai.client.generativeai:generativeai:0.7.0")
    implementation(libs.androidx.espresso.core)
    //implementation(libs.androidx.camera.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation(kotlin("script-runtime"))

    // Add these to override camera version
    implementation("androidx.camera:camera-core:1.3.3")
    implementation("androidx.camera:camera-camera2:1.3.3")
    implementation("androidx.camera:camera-lifecycle:1.3.3")
    implementation("androidx.camera:camera-view:1.3.3")
    implementation("androidx.camera:camera-extensions:1.3.3")


    // ML Kit Face Detection
    implementation("com.google.mlkit:face-detection:16.1.6")

// Room Database
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")

// Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("com.google.code.gson:gson:2.10.1")


}