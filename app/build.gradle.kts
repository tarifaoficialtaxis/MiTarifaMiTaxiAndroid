plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose.compiler)
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.mitarifamitaxi.taximetrousuario"
    compileSdk = 35

    buildFeatures {
        compose = true
        buildConfig = true
    }

    defaultConfig {
        applicationId = "com.mitarifamitaxi.taximetrousuario"
        minSdk = 28
        targetSdk = 35
        versionCode = 17
        versionName = "1.0.0"

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
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.material3.android)

    implementation(libs.androidx.material.icons.extended)

    implementation(libs.firebase.components)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics.ktx)

    implementation(libs.firebase.auth)
    implementation(libs.play.services.auth)
    implementation(libs.firebase.firestore.ktx)

    implementation(libs.gson)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.play.services.location)

    implementation(libs.maps.compose)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.firebase.database.ktx)

    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)
    implementation(libs.androidx.lifecycle.service)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}