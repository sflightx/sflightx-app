plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.gms.google-services")
    alias(libs.plugins.google.firebase.crashlytics)
}

android {
    namespace = "com.sflightx.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.sflightx.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "0.01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            storeFile = file(findProperty("RELEASE_STORE_FILE") as? String ?: "")
            storePassword = findProperty("RELEASE_STORE_PASSWORD") as? String ?: ""
            keyAlias = findProperty("RELEASE_KEY_ALIAS") as? String ?: ""
            keyPassword = findProperty("RELEASE_KEY_PASSWORD") as? String ?: ""
        }
    }



    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")  // Referencing the release signing config
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
        viewBinding = true
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
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.preference)
    implementation(libs.ui)
    implementation(libs.androidx.foundation)
    implementation(libs.google.accompanist.systemuicontroller)
    implementation(libs.coil.compose)
    implementation(libs.google.accompanist.flowlayout)
    implementation(libs.androidx.browser)
    implementation(libs.gson)

    implementation(project(":ImageCrop"))
    implementation(project(":EnhancedFirebase"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.ui.auth)
    implementation(libs.firebase.crashlytics)
    implementation(libs.play.services.auth)
    implementation(libs.google.firebase.database.ktx)
    implementation(libs.firebase.storage.ktx)
    implementation(libs.androidx.runtime.android)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
