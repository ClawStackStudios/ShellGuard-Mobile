plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

android {
  namespace = "com.clawstack.shellguard"
  compileSdk = 36

  defaultConfig {
    applicationId = "com.clawstack.shellguard"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "0.0.0.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
    debug {
      isDebuggable = true
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  testOptions {
    unitTests {
      isIncludeAndroidResources = true
      all { test ->
        test.jvmArgs("-XX:-UsePerfData")
        val testTmpDir = File(layout.buildDirectory.get().asFile, "tmp")
        testTmpDir.mkdirs()
        test.systemProperty("java.io.tmpdir", testTmpDir.absolutePath)
      }
    }
  }

  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }

  packaging {
    jniLibs {
      // Store native libraries uncompressed & page-aligned inside APK/AAB
      // Mandated for Android 15+ 16 KB page-size device compatibility.
      useLegacyPackaging = false
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.navigation.compose)

  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)

  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  ksp(libs.androidx.room.compiler)

  implementation(libs.sqlcipher.android)
  implementation(libs.androidx.sqlite.ktx)

  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)

  implementation(libs.ktor.client.core)
  implementation(libs.ktor.client.okhttp)
  implementation(libs.ktor.client.content.negotiation)
  implementation(libs.ktor.serialization.kotlinx.json)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)

  implementation(libs.androidx.biometric)
  implementation(libs.androidx.biometric.ktx)
  implementation(libs.androidx.security.crypto)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)

  implementation(libs.androidx.camera.camera2)
  implementation(libs.androidx.camera.lifecycle)
  implementation(libs.androidx.camera.view)
  implementation(libs.androidx.camera.core)
  implementation(libs.google.mlkit.barcode.scanning)
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.work.runtime.ktx)

  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.runner)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)

  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
}
