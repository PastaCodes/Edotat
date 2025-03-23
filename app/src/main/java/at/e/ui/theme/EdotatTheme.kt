package at.e.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import at.e.R

object EdotatTheme {
    context(Context)
    @Composable
    fun Apply(content: @Composable () -> Unit) {
        MaterialTheme(
            colorScheme = darkColorScheme(
                primary = Color(getColor(R.color.primary)),
                onPrimary = Color(getColor(R.color.on_primary)),
                secondaryContainer = Color(getColor(R.color.secondary_container)),
            ),
            content = content,
        )
    }
}
