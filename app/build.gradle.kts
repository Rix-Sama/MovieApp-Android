plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.lumio.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.lumio.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 3
        versionName = "3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures { viewBinding = true }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.volley)
    implementation(libs.glide)
    implementation(libs.play.services.maps)
    annotationProcessor(libs.glide.compiler)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.swiperefreshlayout)
    implementation(libs.viewpager2)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.palette)
    implementation(libs.mlkit.image.labeling)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
