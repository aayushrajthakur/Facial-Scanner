plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.facialscanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.facialscanner"
        minSdk = 23
        targetSdk = 35
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.crashlytics.buildtools)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // CameraX core libraries
    implementation(libs.camera.core.v131)
    implementation(libs.camera.camera2.v131)
    implementation(libs.camera.lifecycle.v131)
    implementation(libs.camera.view.v131)

    // ML Kit Face Detection
    implementation(libs.face.detection.v1615)

    // Firebase Firestore
    implementation(libs.firebase.firestore.ktx.v2500)

    // ✅ Needed for ListenableFuture
    implementation(libs.concurrent.futures)

    // Optional: Guava (if you want full API)
    implementation(libs.guava)

    implementation (libs.face.detection)
    implementation (libs.tensorflow.lite)
    implementation (libs.tensorflow.lite.support)
    implementation (libs.retrofit)
    implementation (libs.converter.gson)
    implementation(libs.google.firebase.firestore.ktx)





}