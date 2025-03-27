package at.e.ui.theme

import android.content.Context
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import at.e.R

object EdotatTheme {
    val RoundedCornerShape
        @Composable get() = MaterialTheme.shapes.medium

    object Alpha {
        const val NORMAL = 1.0f
        const val MEDIUM = 0.74f
        const val LOW = 0.38f
    }

    fun Modifier.mediumAlpha() = this.alpha(Alpha.MEDIUM)
    fun Modifier.lowAlpha() = this.alpha(Alpha.LOW)

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
