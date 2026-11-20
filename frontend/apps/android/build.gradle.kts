plugins {
  alias(libs.plugins.local.android.application)
  alias(libs.plugins.local.dependencies.firebase)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.google.play.services)
}

dependencies {
  implementation(projects.appsCore)
  implementation(projects.coreData)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.firebase.messaging)
  implementation(libs.koin.core)

  implementation(libs.bundles.compose.tooling)
  debugImplementation(libs.compose.uiTooling)
}

android {
  namespace = "dev.fnc314.aijobtracker.app.android"
  compileSdk = libs.versions.android.compile.sdk.asProvider().get().toInt()

  defaultConfig {
    applicationId = "dev.fnc314.aijobtracker"
    minSdk = libs.versions.android.min.sdk.get().toInt()
    targetSdk = libs.versions.android.target.sdk.get().toInt()
    versionCode = 20702
    versionName = local.versions.version.get()
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
    jniLibs {
      keepDebugSymbols += listOf(
        "**/libandroidx.graphics.path.so",
        "**/libdatastore_shared_counter.so",
        "**/libsqliteJni.so",
      )
    }
  }
  signingConfigs {
    getByName("debug") {
      storeFile = file("debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  buildTypes {
    debug {
      signingConfig = signingConfigs.getByName("debug")
    }
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  buildFeatures {
    compose = true
  }
  testOptions {
    unitTests {
      isIncludeAndroidResources = true
    }
  }
}

androidComponents {
  beforeVariants(selector().withBuildType("release")) { variant ->
    (variant as com.android.build.api.variant.HasUnitTestBuilder).enableUnitTest = true
  }
}
