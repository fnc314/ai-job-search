package dev.fnc314.aijobtracker.frontend.app.android

import android.app.Application
import com.google.firebase.FirebaseApp
import com.waqas028.kmpinspector.KmpInspector
import dev.fnc314.aijobtracker.frontend.core.data.ApplicationContextHolder

class JobTrackerApplication : Application() {

  override fun onCreate() {
    super.onCreate()
    ApplicationContextHolder.application = this
    KmpInspector.install(this)
    if (FirebaseApp.getApps(this).isEmpty()) {
      FirebaseApp.initializeApp(this)
    }
  }
}
