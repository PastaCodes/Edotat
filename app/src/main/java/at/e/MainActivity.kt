package at.e

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.fragment.app.FragmentActivity
import androidx.navigation.compose.rememberNavController
import at.e.ui.Common

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @SuppressLint("SourceLockedOrientationActivity")
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        enableEdgeToEdge()
        setContent {
            val nc = rememberNavController()
            val gvm: GlobalViewModel by viewModels { GlobalViewModel.Factory(application, nc) }
            Common.Container(gvm, nc) { innerPadding ->
                Navigation.Setup(nc, this, gvm, innerPadding)
            }
        }
    }
}
