import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.local.android.application)
  alias(libs.plugins.local.dependencies.firebase)
  alias(libs.plugins.composeCompiler)
  alias(libs.plugins.google.play.services)
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

dependencies {
  coreLibraryDesugaring(libs.desugaring)
  implementation(projects.appsCore)
  implementation(projects.coreData)

  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)

  implementation(libs.bundles.compose.tooling)
  debugImplementation(libs.compose.uiTooling)
}

android {
  namespace = "dev.fnc314.tools.jobtracker.app.android"
  compileSdk = libs.versions.android.compile.sdk.get().toInt()

  defaultConfig {
    applicationId = "dev.fnc314.tools.jobtracker"
    minSdk = libs.versions.android.min.sdk.get().toInt()
    targetSdk = libs.versions.android.target.sdk.get().toInt()
    versionCode = 1
    versionName = "1.0"
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
  buildTypes {
    release {
      isMinifyEnabled = false
      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro",
      )
    }
  }
  compileOptions {
    isCoreLibraryDesugaringEnabled = true
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
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
