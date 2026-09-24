plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "ru.nksk.parentsapp.core.report"
    compileSdk = 37
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    constraints {
        implementation(libs.androidx.core.ktx) {
            because("Use the app's Core version when compiling this module independently.")
        }
        implementation(libs.androidx.activity.runtime) {
            because("Align Hilt's transitive Activity dependency with the app.")
        }
        implementation(libs.androidx.navigationevent) {
            because("Use the same NavigationEvent version as the app's Navigation 3 host.")
        }
        implementation(libs.kotlinx.serialization.core) {
            because("Align SavedState's transitive serialization with the app.")
        }
    }
    implementation(platform(libs.okhttp.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.okhttp)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.core)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
