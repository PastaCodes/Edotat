package at.e.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath

object EdotatIcons {
    val Meal = Icons.Filled.Restaurant
    val Recent = Icons.Filled.History
    val Account = Icons.Filled.AccountCircle
    val QrCodeScanner = Icons.Filled.QrCodeScanner
    val MyLocation = Icons.Filled.MyLocation
    val Search = Icons.Filled.Search
    val Back = materialIcon(name = "AutoMirrored.Filled.ChevronBackward", autoMirror = true) {
        materialPath {
            moveTo(15.41f, 7.41f)
            lineTo(14.0f, 6.0f)
            lineToRelative(-6.0f, 6.0f)
            lineToRelative(6.0f, 6.0f)
            lineToRelative(1.41f, -1.41f)
            lineTo(10.83f, 12.0f)
            close()
        }
    }
    val Forward = materialIcon(name = "AutoMirrored.Filled.ChevronForward", autoMirror = true) {
        materialPath {
            moveTo(10.0f, 6.0f)
            lineTo(8.59f, 7.41f)
            lineTo(13.17f, 12.0f)
            lineToRelative(-4.58f, 4.59f)
            lineTo(10.0f, 18.0f)
            lineToRelative(6.0f, -6.0f)
            close()
        }
    }
    val Close = Icons.Filled.Close
    val Restaurant = Icons.Filled.Restaurant
    val Visible = Icons.Filled.Visibility
    val Invisible = Icons.Filled.VisibilityOff
}
