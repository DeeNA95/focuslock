package com.focuslock.ui.apppicker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.focuslock.domain.model.InstalledApp
import com.focuslock.ui.components.AppIcon
import com.focuslock.ui.design.FocusCard
import com.focuslock.ui.design.FocusSpacing
import com.focuslock.ui.design.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerDialog(
    apps: List<InstalledApp>,
    selectedPackages: Set<String>,
    isProtected: (String) -> Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(apps, query) {
        if (query.isBlank()) apps
        else apps.filter {
            it.label.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(horizontal = FocusSpacing.L)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SectionHeader(title = "Block apps", modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Filled.Close, contentDescription = "Close")
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search apps...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
            )

            LazyColumn(modifier = Modifier.padding(vertical = FocusSpacing.M)) {
                items(filtered, key = { it.packageName }) { app ->
                    val unsafe = isProtected(app.packageName)
                    val checked = app.packageName in selectedPackages
                    AppRow(
                        app = app,
                        checked = checked,
                        unsafe = unsafe,
                        enabled = !unsafe,
                        onToggle = { onToggle(app.packageName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    checked: Boolean,
    unsafe: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    FocusCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = FocusSpacing.XS),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { onToggle() }
                .padding(horizontal = FocusSpacing.M, vertical = FocusSpacing.XS),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppIcon(app.packageName, size = 40.dp)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = FocusSpacing.M),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    app.label,
                    fontWeight = if (checked) FontWeight.SemiBold else FontWeight.Normal,
                )
                Text(
                    text = if (unsafe) "${app.packageName} · cannot be blocked" else app.packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (unsafe) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Checkbox(
                checked = checked,
                onCheckedChange = { onToggle() },
                enabled = enabled,
            )
        }
    }
}
