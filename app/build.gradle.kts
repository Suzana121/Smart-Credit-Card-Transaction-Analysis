plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    // שימוש ב-Compose Compiler החדש שמותאם לגרסת ה-Kotlin שלך
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
    // חובה להוסיף את ה-plugin של Google Services כדי ש-Firebase יעבוד
    id("com.google.gms.google-services")
}

android {
    namespace = "com.cardify.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cardify.app"
        minSdk = 24
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
        compose = true
    }

    // הערה: הסרתי את composeOptions כי בגרסת Kotlin 2.0+
    // הקומפיילר של Compose מנוהל דרך ה-Plugins למעלה.

    buildToolsVersion = "36.1.0"
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.10.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Retrofit for API calls
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // SharedPreferences encryption
    implementation("androidx.security:security-crypto:1.1.0")

    // ===== Jetpack Compose =====
    implementation(platform("androidx.compose:compose-bom:2025.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.12.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.navigation:navigation-compose:2.7.5")

    // Compose Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // ===== Firebase & Voice Recording Support (חדש!) 🎤 =====
    // BoM של Firebase לניהול גרסאות אוטומטי
    implementation(platform("com.google.firebase:firebase-bom:33.1.1"))

    // ספריות Firebase לשימוש בהקלטות ואחסון
    implementation("com.google.firebase:firebase-storage-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

    // תמיכה ב-Coroutines עבור משימות Firebase
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    // Graph libraries
    implementation("com.patrykandpatrick.vico:compose:1.13.1")
    implementation("com.patrykandpatrick.vico:compose-m3:1.13.1")
    implementation("com.patrykandpatrick.vico:core:1.13.1")

    // Images & Animation
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.airbnb.android:lottie-compose:6.4.0")
}