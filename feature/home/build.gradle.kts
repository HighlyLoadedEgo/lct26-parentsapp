plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ru.nksk.parentsapp.feature.home"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
}

dependencies {
    constraints {
        implementation(libs.androidx.core.ktx) {
            because("Use the app's Core version when compiling this feature independently.")
        }
        implementation(libs.androidx.activity.runtime) {
            because("Align Hilt's transitive Activity dependency with the app.")
        }
        implementation(libs.androidx.compose.runtime) {
            because("Pin the Compose runtime version aligned with the app.")
        }
    }
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
}
