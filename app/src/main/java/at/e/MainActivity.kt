package at.e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import at.e.ui.Common

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            Common.Container(navController) { innerPadding, setBottomBarVisible ->
                Navigation.Setup(navController, innerPadding, setBottomBarVisible)
            }
        }
    }
}
