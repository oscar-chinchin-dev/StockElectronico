package com.example.stockelectronico.ui.external

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockelectronico.R
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.domain.model.ExternalCatalogProduct
import com.example.stockelectronico.ui.inventory.formatClp
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExternalCatalogScreen(viewModel: ExternalCatalogViewModel, onBack: () -> Unit) {
    val catalogState by viewModel.catalogState.collectAsStateWithLifecycle()
    val importState by viewModel.importState.collectAsStateWithLifecycle()
    Scaffold(topBar = { TopAppBar(title = { Text(stringResource(R.string.external_catalog_title)) }, navigationIcon = {
        TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back)) }
    }) }) { padding ->
        when (val state = catalogState) {
            ExternalCatalogUiState.Loading -> LoadingContent(Modifier.padding(padding))
            ExternalCatalogUiState.Error -> Column(Modifier.fillMaxSize().padding(padding), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.external_catalog_error))
                Button(onClick = viewModel::loadCatalog) { Text(stringResource(R.string.retry)) }
            }
            is ExternalCatalogUiState.Success -> LazyColumn(Modifier.fillMaxSize().padding(padding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.products, key = { it.externalId }) { ExternalProductCard(it, viewModel::beginImport) }
            }
        }
    }
    importState.product?.let { product -> ImportDialog(product, importState, viewModel, onBack) }
}

@Composable private fun CenterMessage(message: String, modifier: Modifier) = Column(modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text(message) }

@Composable private fun LoadingContent(modifier: Modifier) = Column(
    modifier.fillMaxSize(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center
) {
    CircularProgressIndicator()
    Text(stringResource(R.string.loading), modifier = Modifier.padding(top = 12.dp))
}

@Composable private fun ExternalProductCard(product: ExternalCatalogProduct, onImport: (ExternalCatalogProduct) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(product.name, style = MaterialTheme.typography.titleMedium)
        Text("${product.brand ?: stringResource(R.string.external_catalog_no_brand)} · ${product.category}")
        Text(formatUsd(product.priceUsd), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.external_catalog_stock, product.stock))
        product.code?.let { Text(stringResource(R.string.inventory_code, it)) }
        Button(onClick = { onImport(product) }) { Text(stringResource(R.string.import_action)) }
    } }
}

@Composable private fun ImportDialog(product: ExternalCatalogProduct, state: ImportUiState, viewModel: ExternalCatalogViewModel, onImported: () -> Unit) {
    AlertDialog(
        onDismissRequest = { if (!state.isImporting) viewModel.cancelImport() },
        title = { Text(stringResource(R.string.import_product_title)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(product.name, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.import_catalog_price, formatUsd(product.priceUsd)))
            Text(stringResource(R.string.import_inventory_price, formatClp(convertUsdToClp(product.priceUsd))))
            Text(stringResource(R.string.inventory_stock, product.stock))
            Text(stringResource(R.string.inventory_code, product.code.orEmpty()))
            OutlinedTextField(value = state.supplier, onValueChange = viewModel::onSupplierChanged, label = { Text(stringResource(R.string.form_supplier)) }, isError = state.supplierInvalid, supportingText = if (state.supplierInvalid) ({ Text(stringResource(R.string.form_error_supplier_required)) }) else null, modifier = Modifier.fillMaxWidth(), enabled = !state.isImporting)
            Text(stringResource(R.string.form_channel))
            Canal.entries.forEach { channel -> Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = state.channel == channel, onClick = { viewModel.onChannelChanged(channel) }, enabled = !state.isImporting)
                Text(channelLabel(channel))
            } }
            if (state.error) Text(stringResource(R.string.import_error), color = MaterialTheme.colorScheme.error)
            if (state.succeeded) Text(stringResource(R.string.import_success), color = MaterialTheme.colorScheme.primary)
        } },
        dismissButton = { if (!state.succeeded) TextButton(onClick = viewModel::cancelImport, enabled = !state.isImporting) { Text(stringResource(R.string.cancel)) } },
        confirmButton = { Button(onClick = {
            if (state.succeeded) { viewModel.cancelImport(); onImported() } else viewModel.confirmImport()
        }, enabled = !state.isImporting) { Text(stringResource(if (state.succeeded) R.string.import_back_to_inventory else if (state.isImporting) R.string.importing else R.string.import_action)) } }
    )
}

@Composable private fun channelLabel(channel: Canal): String = stringResource(when (channel) { Canal.SUCURSAL -> R.string.channel_branch; Canal.ONLINE -> R.string.channel_online; Canal.AMBOS -> R.string.channel_both })
private fun formatUsd(value: Double): String = String.format(Locale.US, "US$ %.2f", value)
