package dev.fnc314.tools.jobtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.google.firebase.FirebaseApp
import dev.fnc314.tools.jobtracker.app.core.App
import dev.fnc314.tools.jobtracker.core.data.ApplicationContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        ApplicationContextHolder.application = application
        if (FirebaseApp.getApps(this).isEmpty()) {
            FirebaseApp.initializeApp(this)
        }

        setContent {
          App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
  App()
}