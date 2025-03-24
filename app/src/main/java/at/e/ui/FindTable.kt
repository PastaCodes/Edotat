package at.e.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import at.e.Navigation
import at.e.R
import at.e.UserPreferences
import at.e.backend.backendInterface

data object FindTable {
    object ChooseMethod {
        context(Context)
        @Composable
        fun Screen(navController: NavController, innerPadding: PaddingValues) {
            // State of the "set as preferred method" checkbox
            val (checkedState, onStateChange) = remember { mutableStateOf(false) }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Header(large = true)
                Spacer(modifier = Modifier.height(16.dp))
                MethodButtons(navController, checkedState)
                Spacer(modifier = Modifier.height(32.dp))
                SetPreferredMethod(checkedState, onStateChange)
            }
        }

        private data class MethodButton(
            val imageVector: ImageVector,
            @StringRes val textResId: Int,
            val method: Method,
        )

        private val methodButtons = listOf(
            MethodButton(Common.Icons.QrCodeScanner, R.string.find_table_scan_qr_code, Method.QrCode),
            MethodButton(Common.Icons.MyLocation, R.string.find_table_near_me, Method.NearMe),
            MethodButton(Common.Icons.Search, R.string.find_table_search, Method.Search)
        )

        context(Context)
        @Composable
        private fun MethodButtons(navController: NavController, checkedState: Boolean) {
            val coroutineScope = rememberCoroutineScope()
            Column(
                modifier = Modifier.width(300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                methodButtons.forEach { button ->
                    Button(
                        onClick = {
                            if (checkedState) {
                                with(coroutineScope) {
                                    UserPreferences.save(
                                        key = UserPreferences.Keys.FindTablePreferredMethod,
                                        value = button.method.toPreference(),
                                    )
                                }
                            }
                            navController.navigate(
                                route = button.method.route(/* isInitial = */ false),
                            )
                        },
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
        private fun SetPreferredMethod(checkedState: Boolean, onStateChange: (Boolean) -> Unit) {
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
    }

    @Composable
    private fun Header(large: Boolean) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(if (large) 32.dp else 16.dp),
        ) {
            Text(
                text = "Hungry?",
                fontSize = if (large) 60.sp else 40.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Text(
                text = "Let's find your table.",
                fontSize = 26.sp,
            )
        }
    }

    context(Context)
    @Composable
    private fun Back(navController: NavController) {
        TextButton(
            onClick = {
                navController.navigateUp()
            },
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
                    imageVector = Common.Icons.ChevronLeft,
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

    sealed class Method(val route: (Boolean) -> Any) {
        companion object {
            const val NO_PREFERENCE = 0

            fun fromPreference(value: Int) =
                when (value) {
                    1 -> QrCode
                    2 -> NearMe
                    3 -> Search
                    else -> null
                }

            fun Method?.toRoute(isInitial: Boolean = false) =
                when (this) {
                    null -> Navigation.Destination.FindTable.ChooseMethod
                    else -> this.route(isInitial)
                }
        }

        fun toPreference() =
            when (this) {
                QrCode -> 1
                NearMe -> 2
                Search -> 3
            }

        data object QrCode : Method(Navigation.Destination.FindTable.Method::QrCode)

        data object NearMe : Method(Navigation.Destination.FindTable.Method::NearMe)

        data object Search : Method(Navigation.Destination.FindTable.Method::Search) {
            context(Context)
            @Composable
            fun Screen(
                navController: NavController,
                innerPadding: PaddingValues,
                isInitial: Boolean = false,
            ) {
                val (query, onQueryChange) = remember { mutableStateOf("") }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(24.dp)
                        .consumeWindowInsets(innerPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Back(navController)
                    if (isInitial) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Header(large = false)
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    SearchBar(query, onQueryChange)
                    RestaurantResults(query, navController)
                }
            }

            context(Context)
            @Composable
            private fun SearchBar(query: String, setQuery: (String) -> Unit) {
                OutlinedTextField(
                    value = query,
                    onValueChange = setQuery,
                    modifier = Modifier.fillMaxWidth(),
                    shape = CircleShape,
                    leadingIcon = {
                        Icon(
                            imageVector = Common.Icons.Search,
                            contentDescription = null, // Icon is decorative
                            modifier = Modifier.padding(start = 16.dp),
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(
                                onClick = { setQuery("") },
                            ) {
                                Icon(
                                    imageVector = Common.Icons.Close,
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

            @Composable
            private fun RestaurantResults(query: String, navController: NavController) {
                LazyColumn (
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(4.dp),
                ) {
                    items(backendInterface.getRestaurants(query)) { result ->
                        OutlinedCard(
                            onClick = {
                                navController.navigate(route = TODO())
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier.height(86.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Common.Icons.Restaurant,
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
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = result.address.toString(),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Icon(
                                    imageVector = Common.Icons.ChevronRight,
                                    contentDescription = null, // Icon is decorative
                                    modifier = Modifier.padding(horizontal = 24.dp).size(24.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
