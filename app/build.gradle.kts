plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.dan.terminal"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.dan.terminal"
        minSdk = 26
        targetSdk = 36
        versionCode = providers.environmentVariable("DAN_VERSION_CODE").orNull?.toIntOrNull() ?: 1
        versionName = providers.environmentVariable("DAN_VERSION_NAME").orNull ?: "0.1.0-M1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val updateStore = providers.environmentVariable("DAN_UPDATE_KEYSTORE").orNull
    val updateAlias = providers.environmentVariable("DAN_UPDATE_KEY_ALIAS").orNull
    val updateStorePassword = providers.environmentVariable("DAN_UPDATE_STORE_PASSWORD").orNull
    val updateKeyPassword = providers.environmentVariable("DAN_UPDATE_KEY_PASSWORD").orNull
    val updateSigningEnabled = listOf(updateStore, updateAlias, updateStorePassword, updateKeyPassword)
        .all { !it.isNullOrBlank() }

    signingConfigs {
        if (updateSigningEnabled) {
            create("obtainium") {
                storeFile = file(updateStore!!)
                storePassword = updateStorePassword
                keyAlias = updateAlias
                keyPassword = updateKeyPassword
            }
        }
    }
    buildTypes {
        getByName("debug") {
            if (updateSigningEnabled) signingConfig = signingConfigs.getByName("obtainium")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:security"))
    implementation(project(":core:shizuku"))
    implementation(project(":core:bluetooth"))
    implementation(project(":feature:termux"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.mockito.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
