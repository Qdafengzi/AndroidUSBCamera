plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    namespace = "jp.co.cyberagent.android.gpuimage"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        targetSdk = 34
        ndk {
            abiFilters.add("arm64-v8a")
            abiFilters.add("armeabi-v7a")
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
        }
    }

    namespace = "jp.co.cyberagent.android.gpuimage"

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core"))
    // implementation(project(":libnative"))
}
