package at.e

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
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
fun FindTableBack() {
    TextButton(
        onClick = { },
        shape = MaterialTheme.shapes.medium,
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = 8.dp,
            end = 16.dp,
        ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.ChevronLeft,
                contentDescription = null, // Icon is decorative
                modifier = Modifier.size(32.dp),
            )
            Text(
                text = getString(R.string.find_table_back),
                fontSize = 16.sp,
            )
        }
    }
}

context(Context)
@Composable
fun SearchRestaurant(innerPadding: PaddingValues) {
    Column(
        modifier = Modifier.padding(innerPadding).padding(24.dp).fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FindTableBack()
        SearchRestaurantBar()
        SearchRestaurantResults(TMP_RESULTS)
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

data class RestaurantResult(
    val name: String,
    val address: String,
)

@Composable
fun SearchRestaurantResults(results: List<RestaurantResult>) {
    LazyColumn (
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(4.dp),
    ) {
        items(results) { result ->
            OutlinedCard(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.height(86.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.Restaurant,
                        contentDescription = null, // Icon is decorative
                        modifier = Modifier.padding(horizontal = 24.dp).size(32.dp),
                    )
                    Column(
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = result.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = result.address,
                        )
                    }
                    Icon(
                        imageVector = Icons.Filled.ChevronRight,
                        contentDescription = null, // Icon is decorative
                        modifier = Modifier.padding(horizontal = 24.dp).size(24.dp),
                    )
                }
            }
        }
    }
}

val TMP_RESULTS = listOf(
    RestaurantResult("Da Gino", "Via Marinara, 72"),
    RestaurantResult("Cool Burgers", "Via Pomodoro, 64"),
    RestaurantResult("Hot Kebab", "Via Salina, 41"),
    RestaurantResult("Da Gino", "Via Marinara, 72"),
    RestaurantResult("Cool Burgers", "Via Pomodoro, 64"),
    RestaurantResult("Hot Kebab", "Via Salina, 41"),
    RestaurantResult("Da Gino", "Via Marinara, 72"),
    RestaurantResult("Cool Burgers", "Via Pomodoro, 64"),
    RestaurantResult("Hot Kebab", "Via Salina, 41"),
    RestaurantResult("Da Gino", "Via Marinara, 72"),
    RestaurantResult("Cool Burgers", "Via Pomodoro, 64"),
    RestaurantResult("Hot Kebab", "Via Salina, 41"),
)
