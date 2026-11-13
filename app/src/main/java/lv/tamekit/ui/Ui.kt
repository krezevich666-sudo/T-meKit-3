package lv.tamekit.ui

import android.widget.Toast
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import lv.tamekit.R
import lv.tamekit.data.Company
import lv.tamekit.data.Customer
import lv.tamekit.data.Material
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale

private enum class Tab(val route: String, val label: Int, val icon: ImageVector) {
    HOME("home", R.string.tab_home, Icons.Filled.Home),
    ESTIMATES("estimates", R.string.tab_estimates, Icons.Filled.Create),
    MATERIALS("materials", R.string.tab_materials, Icons.Filled.ShoppingCart),
    CUSTOMERS("customers", R.string.tab_customers, Icons.Filled.Person),
    SETTINGS("settings", R.string.tab_settings, Icons.Filled.Settings)
}

private fun go(nav: NavHostController, route: String) {
    nav.navigate(route) {
        popUpTo(nav.graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}


@Composable
fun TameKitApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val current = entry?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            Tab.entries.forEach { t ->
                NavigationBarItem(
                    selected = current == t.route,
                    onClick = { go(nav, t.route) },
                    icon = { Icon(t.icon, contentDescription = null) },
                    label = { Text(stringResource(t.label)) }
                )
            }
        }
    }) { pad ->
        NavHost(nav, startDestination = Tab.HOME.route, modifier = Modifier.padding(pad)) {
            composable(Tab.HOME.route) { HomeScreen(vm) { go(nav, it) } }
            composable(Tab.ESTIMATES.route) { Placeholder(R.string.estimates_soon) }
            composable(Tab.MATERIALS.route) { MaterialsScreen(vm) }
            composable(Tab.CUSTOMERS.route) { CustomersScreen(vm) }
            composable(Tab.SETTINGS.route) { SettingsScreen(vm) }
        }
    }
}

@Composable
private fun Placeholder(text: Int) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Text(stringResource(text)) }
}

@Composable
internal fun Field(value: String, onChange: (String) -> Unit, label: Int, decimal: Boolean = false, error: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(stringResource(label)) },
        singleLine = true,
        isError = error,
        supportingText = { if (error) Text(stringResource(R.string.invalid_value)) },
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Text)
    )
}

@Composable
private fun Stat(value: String, label: Int, modifier: Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(stringResource(label))
        }
    }
}

@Composable
private fun HomeScreen(vm: MainViewModel, onGo: (String) -> Unit) {
    val customers by vm.customers.collectAsStateWithLifecycle()
    val materials by vm.materials.collectAsStateWithLifecycle()
    val balances by vm.balances.collectAsStateWithLifecycle()
    val low = materials.count { it.minimumStock.signum() > 0 && (balances[it.id] ?: BigDecimal.ZERO) <= it.minimumStock }
    Column(
        Modifier.padding(16.dp).verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.home_greeting), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.home_subtitle))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat("0", R.string.stat_estimates, Modifier.weight(1f))
            Stat(customers.size.toString(), R.string.stat_customers, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Stat(materials.size.toString(), R.string.stat_materials, Modifier.weight(1f))
            Stat(low.toString(), R.string.stat_low, Modifier.weight(1f))
        }
        Text(stringResource(R.string.quick_actions), style = MaterialTheme.typography.titleMedium)
        Button({ onGo(Tab.ESTIMATES.route) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.new_estimate)) }
        OutlinedButton({ onGo(Tab.CUSTOMERS.route) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_customer)) }
        OutlinedButton({ onGo(Tab.MATERIALS.route) }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.add_material)) }
    }
}

@Composable
private fun CustomersScreen(vm: MainViewModel) {
    val list by vm.customers.collectAsStateWithLifecycle()
    var show by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize()) {
        if (list.isEmpty()) Text(stringResource(R.string.customers_empty), Modifier.align(Alignment.Center))
        LazyColumn(Modifier.fillMaxSize()) {
            items(list, key = { it.id }) { c ->
                ListItem(
                    headlineContent = { Text(c.name) },
                    supportingContent = { if (c.phone.isNotBlank()) Text(c.phone) },
                    trailingContent = {
                        IconButton({ vm.archiveCustomer(c) }) { Icon(Icons.Filled.Delete, contentDescription = null) }
                    }
                )
                HorizontalDivider()
            }
        }
        FloatingActionButton({ show = true }, Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
        }
    }
    if (show) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { show = false },
            title = { Text(stringResource(R.string.add_customer)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(name, { name = it }, R.string.customer_name)
                    Field(phone, { phone = it }, R.string.phone)
                }
            },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    vm.addCustomer(Customer(name = name.trim(), phone = phone.trim()))
                    show = false
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton({ show = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
