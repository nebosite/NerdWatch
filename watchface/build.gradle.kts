plugins {
    id("com.android.application")
}

/**
 * The Watch Face Format dial. Resource-only by mandate: WFF bundles may contain
 * no executable code, which is why this is a separate module from :app.
 */
android {
    namespace = "com.nerdwatch.dial"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nerdwatch.dial"
        // WFF requires API 33+.
        minSdk = 33
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}
