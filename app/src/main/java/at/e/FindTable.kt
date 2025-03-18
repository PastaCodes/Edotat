package at.e

import android.content.Context
import android.graphics.drawable.shapes.RoundRectShape
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

context(Context)
@Composable
fun FindTable(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FindTableHeader()
        Spacer(Modifier.height(16.dp))
        FindTableButtons()
        Spacer(Modifier.height(32.dp))
        FindTableSetPreferredMethod()
    }
}

@Composable
fun FindTableHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        Text(
            text = "Hungry?",
            fontSize = 60.sp,
        )
        Text(
            text = "Let's find your table.",
            fontSize = 26.sp,
        )
    }
}

data class FindTableButton(
    val imageVector: ImageVector,
    @StringRes val textResId: Int,
)

context(Context)
@Composable
fun FindTableButtons() {
    Column(
        modifier = Modifier.width(300.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        listOf(
            FindTableButton(Icons.Filled.QrCodeScanner, R.string.find_table_scan_qr_code),
            FindTableButton(Icons.Filled.MyLocation, R.string.find_table_near_me),
            FindTableButton(Icons.Filled.Search, R.string.find_table_search)
        ).forEach { button ->
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(24.dp, 16.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = button.imageVector,
                        contentDescription = null, // Icons are decorative
                        modifier = Modifier.size(40.dp),
                    )
                    Text(
                        text = getString(button.textResId),
                        fontSize = 18.sp,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

context(Context)
@Composable
fun FindTableSetPreferredMethod() {
    val (checkedState, onStateChange) = remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.toggleable(
            value = checkedState,
            onValueChange = { onStateChange(!checkedState) },
            role = Role.Checkbox
        ),
    ) {
        Checkbox(
            checked = checkedState,
            onCheckedChange = null,
        )
        Text(
            text = getString(R.string.find_table_set_preferred_method),
            fontSize = 18.sp,
            modifier = Modifier. padding(start = 8.dp),
        )
    }
}

context(Context)
@Composable
fun SearchRestaurant(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(innerPadding).fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        SearchRestaurantBar()
    }
}

context(Context)
@Composable
fun SearchRestaurantBar() {
    val (query, onQueryChange) = remember { mutableStateOf("") }
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = Modifier.fillMaxWidth(),
        shape = CircleShape,
        leadingIcon = {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = null, // Icon is decorative
                modifier = Modifier.padding(start = 16.dp),
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(
                    onClick = { onQueryChange("") },
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = getString(R.string.icon_clear_search),
                        modifier = Modifier.padding(end = 12.dp),
                    )
                }
            }
        },
        placeholder = {
            Text(
                text = getString(R.string.search_restaurants),
            )
        },
        singleLine = true,
    )
}
