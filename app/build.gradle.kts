plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.tvplayer"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.tvplayer"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
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
        viewBinding = false
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.annotation:annotation-experimental:1.3.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
        force("androidx.lifecycle:lifecycle-livedata-core:2.6.1")
        force("androidx.savedstate:savedstate:1.2.1")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Android TV / Leanback
    implementation("androidx.leanback:leanback:1.2.0")

    // Media3 ExoPlayer
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-ui:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")

    // Lifecycle & coroutines
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Image loading for channel logos
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // Pin transitive dependencies to cached versions
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.0")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.0")
    implementation("androidx.annotation:annotation-experimental:1.3.0")
    implementation("androidx.lifecycle:lifecycle-livedata-core:2.6.1")
}
