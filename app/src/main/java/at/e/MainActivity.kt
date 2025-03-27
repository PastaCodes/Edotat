package at.e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.navigation.compose.rememberNavController
import at.e.ui.Common

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val gvm: GlobalViewModel by viewModels { GlobalViewModel.Factory(application) }
            val nc = rememberNavController()
            Common.Container(gvm, nc) { innerPadding, setBottomBarVisible ->
                Navigation.Setup(nc, gvm, innerPadding, setBottomBarVisible)
            }
        }
    }
}
