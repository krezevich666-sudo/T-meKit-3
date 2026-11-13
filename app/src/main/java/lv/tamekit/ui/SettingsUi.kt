package lv.tamekit.ui

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import lv.tamekit.R
import lv.tamekit.data.Company
import java.util.Locale

@Composable
internal fun SettingsScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val company by vm.company.collectAsStateWithLifecycle()
    val cur = AppCompatDelegate.getApplicationLocales().get(0)?.language ?: Locale.getDefault().language
    var name by remember(company) { mutableStateOf(company?.name ?: "") }
    var reg by remember(company) { mutableStateOf(company?.registrationNumber ?: "") }
    var phone by remember(company) { mutableStateOf(company?.phone ?: "") }
    var email by remember(company) { mutableStateOf(company?.email ?: "") }
    var address by remember(company) { mutableStateOf(company?.address ?: "") }
    var confirmRestore by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = vm.exportJson()
                withContext(Dispatchers.IO) {
                    ctx.contentResolver.openOutputStream(uri)!!.use { it.write(json.toByteArray(Charsets.UTF_8)) }
                }
            }.isSuccess
            Toast.makeText(ctx, if (ok) R.string.export_ok else R.string.export_fail, Toast.LENGTH_SHORT).show()
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            val ok = runCatching {
                val json = withContext(Dispatchers.IO) {
                    ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
                }
                vm.restoreJson(json)
            }.isSuccess
            Toast.makeText(ctx, if (ok) R.string.restore_ok else R.string.restore_fail, Toast.LENGTH_LONG).show()
        }
    }

    Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
        listOf("lv" to R.string.lang_lv, "en" to R.string.lang_en, "ru" to R.string.lang_ru).forEach { (tag, label) ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = cur == tag,
                    onClick = { AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag)) }
                )
                Text(stringResource(label))
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.company), style = MaterialTheme.typography.titleMedium)
        Field(name, { name = it }, R.string.company_name)
        Field(reg, { reg = it }, R.string.reg_number)
        Field(phone, { phone = it }, R.string.phone)
        Field(email, { email = it }, R.string.email)
        Field(address, { address = it }, R.string.address)
        Button(onClick = {
            vm.saveCompany(
                Company(
                    name = name.trim(), registrationNumber = reg.trim(), phone = phone.trim(),
                    email = email.trim(), address = address.trim(),
                    createdAt = company?.createdAt ?: System.currentTimeMillis()
                )
            )
            Toast.makeText(ctx, R.string.saved, Toast.LENGTH_SHORT).show()
        }) { Text(stringResource(R.string.save)) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.backup), style = MaterialTheme.typography.titleMedium)
        OutlinedButton({ exportLauncher.launch("tamekit-backup.json") }, Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.export_data))
        }
        OutlinedButton({ confirmRestore = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.restore_data)) }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        Text(stringResource(R.string.demo), style = MaterialTheme.typography.titleMedium)
        OutlinedButton({
            scope.launch {
                val loaded = runCatching { vm.loadDemo() }.getOrDefault(false)
                Toast.makeText(ctx, if (loaded) R.string.demo_loaded else R.string.demo_not_empty, Toast.LENGTH_LONG).show()
            }
        }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.demo_load)) }
        OutlinedButton({ confirmClear = true }, Modifier.fillMaxWidth()) { Text(stringResource(R.string.clear_all)) }
    }
    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(stringResource(R.string.clear_warn)) },
            confirmButton = {
                TextButton({ confirmClear = false; scope.launch { vm.clearAllData() } }) { Text(stringResource(R.string.clear_all)) }
            },
            dismissButton = { TextButton({ confirmClear = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    if (confirmRestore) {
        AlertDialog(
            onDismissRequest = { confirmRestore = false },
            text = { Text(stringResource(R.string.restore_warn)) },
            confirmButton = {
                TextButton({ confirmRestore = false; importLauncher.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.restore_data)) }
            },
            dismissButton = { TextButton({ confirmRestore = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
