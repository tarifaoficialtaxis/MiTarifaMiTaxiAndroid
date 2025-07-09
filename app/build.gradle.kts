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
        versionCode = 22
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {

        debug {
            buildConfigField("boolean", "IS_DEV", "true")
            buildConfigField(
                "String",
                "OPEN_APP_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9257395921\""
            )
            buildConfigField(
                "String",
                "HOME_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
            buildConfigField(
                "String",
                "SOS_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )
            buildConfigField(
                "String",
                "MY_TRIPS_AD_UNIT_ID",
                "\"ca-app-pub-3940256099942544/9214589741\""
            )

        }

        release {
            buildConfigField("boolean", "IS_DEV", "false")
            buildConfigField(
                "String",
                "OPEN_APP_AD_UNIT_ID",
                "\"ca-app-pub-3864915489725459/1852355259\""
            )
            buildConfigField(
                "String",
                "HOME_AD_UNIT_ID",
                "\"ca-app-pub-3864915489725459/1408985203\""
            )
            buildConfigField(
                "String",
                "SOS_AD_UNIT_ID",
                "\"ca-app-pub-3864915489725459/3565726328\""
            )
            buildConfigField(
                "String",
                "MY_TRIPS_AD_UNIT_ID",
                "\"ca-app-pub-3864915489725459/1425297076\""
            )

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

    implementation(kotlin("reflect"))

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

    implementation(libs.play.services.ads)

    implementation(libs.androidx.lifecycle.process)


    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.tooling.preview)
}