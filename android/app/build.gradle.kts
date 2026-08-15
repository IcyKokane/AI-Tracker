plugins { id("com.android.application") }

android {
    namespace = "com.thefoolish.activityai"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.thefoolish.activityai"
        minSdk = 26
        targetSdk = 36
        versionCode = 4
        versionName = "0.1.2-m1-idempotencyfix"
    }
}
