package at.e

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.serialization.Serializable
import at.e.ui.theme.EdotatTheme

val PREFERENCES_FILE_KEY = "at.e.PREFERENCE_FILE_KEY"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            EdotatTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                ) { innerPadding ->
                    Main(innerPadding)
                }
            }
        }
    }

    @Serializable
    object ChooseMethod
    @Serializable
    object QrCode
    @Serializable
    object NearMe
    @Serializable
    object SearchRestaurant

    @Composable
    fun Main(innerPadding: PaddingValues) {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = ChooseMethod,
        ) {
            composable<ChooseMethod> { ChooseMethodScreen(navController, innerPadding) }
            composable<QrCode> { /* TODO */ }
            composable<NearMe> { /* TODO */ }
            composable<SearchRestaurant> { SearchRestaurantScreen(navController, innerPadding) }
        }
    }
}
