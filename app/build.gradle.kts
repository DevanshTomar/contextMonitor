plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.contextmonitor"
    compileSdk = 34

    viewBinding {
        enable = true
    }
    dataBinding {
        enable = true
    }


    defaultConfig {
        applicationId = "com.example.contextmonitor"
        minSdk = 29
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.ui.graphics.android)
    implementation(libs.camera.video)
    implementation(libs.camera.view)
    implementation(libs.camera.core)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.camera.lifecycle)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.extensions)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.support.annotations)
    implementation(libs.androidx.annotation)
    annotationProcessor(libs.room.compiler)
    kapt(libs.room.compiler)
}