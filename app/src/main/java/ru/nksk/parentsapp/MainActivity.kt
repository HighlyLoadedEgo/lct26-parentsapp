package ru.nksk.parentsapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import ru.nksk.parentsapp.feature.pin.access.ParentsAccessViewModel
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import ru.nksk.parentsapp.app.ParentsApp

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val access by lazy { ViewModelProvider(this)[ParentsAccessViewModel::class.java] }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        setContent {
            ParentsApp(access = access, onClose = { access.lock(); finish() })
        }
    }

    override fun onStart() {
        super.onStart()
        access.onForeground()
    }

    override fun onStop() {
        if (!isChangingConfigurations) access.lock()
        super.onStop()
    }
}
