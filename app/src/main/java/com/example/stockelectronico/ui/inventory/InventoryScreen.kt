package com.example.stockelectronico.ui.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockelectronico.R
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.model.Canal
import java.text.NumberFormat
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryScreen(viewModel: InventoryViewModel, onProductSelected: (String) -> Unit, onCreateProduct: () -> Unit, onExternalCatalog: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }, actions = {
            TextButton(onClick = onExternalCatalog) { Text(stringResource(R.string.external_catalog_title)) }
            TextButton(onClick = viewModel::synchronize) { Text(stringResource(R.string.sync_action)) }
        }) },
        floatingActionButton = { FloatingActionButton(onClick = onCreateProduct) { Text(stringResource(R.string.create_product_short)) } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            when {
                state.isSyncing -> Text(stringResource(R.string.sync_in_progress))
                state.pendingCount > 0 -> Text(stringResource(R.string.sync_pending, state.pendingCount))
                state.syncFailed -> Text(stringResource(R.string.sync_error))
                else -> Text(stringResource(R.string.sync_complete))
            }
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text(stringResource(R.string.inventory_search_label)) },
                placeholder = { Text(stringResource(R.string.inventory_search_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                singleLine = true
            )
            InventoryFilters(state, viewModel)
            when {
                state.isLoading -> LoadingContent()
                state.hasError -> MessageContent(R.string.inventory_error)
                state.productos.isEmpty() -> MessageContent(
                    if (state.totalProductos == 0) R.string.inventory_empty else R.string.inventory_no_results
                )
                else -> ProductList(state.productos, onProductSelected)
            }
        }
    }
}

@Composable
private fun InventoryFilters(state: InventoryUiState, viewModel: InventoryViewModel) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
        Text(stringResource(R.string.inventory_filter_category), style = MaterialTheme.typography.labelLarge)
        FilterMenu(
            selected = state.categoriaSeleccionada ?: stringResource(R.string.inventory_filter_all),
            options = state.categoriasDisponibles,
            onSelected = viewModel::onCategoriaChanged
        )
        Text(stringResource(R.string.inventory_filter_channel), style = MaterialTheme.typography.labelLarge)
        FilterMenu(
            selected = state.canalSeleccionado?.let { channelName(it) } ?: stringResource(R.string.inventory_filter_all),
            options = Canal.entries,
            label = ::channelName,
            onSelected = viewModel::onCanalChanged
        )
        Text(stringResource(R.string.inventory_filter_supplier), style = MaterialTheme.typography.labelLarge)
        FilterMenu(
            selected = state.proveedorSeleccionado ?: stringResource(R.string.inventory_filter_all),
            options = state.proveedoresDisponibles,
            onSelected = viewModel::onProveedorChanged
        )
    }
}

@Composable
private fun FilterMenu(selected: String, options: List<String>, onSelected: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) { Text(selected) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.inventory_filter_all)) }, onClick = { onSelected(null); expanded = false })
        options.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { onSelected(option); expanded = false }) }
    }
}

@Composable
private fun FilterMenu(selected: String, options: List<Canal>, label: @Composable (Canal) -> String, onSelected: (Canal?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    TextButton(onClick = { expanded = true }) { Text(selected) }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        DropdownMenuItem(text = { Text(stringResource(R.string.inventory_filter_all)) }, onClick = { onSelected(null); expanded = false })
        options.forEach { option -> DropdownMenuItem(text = { Text(label(option)) }, onClick = { onSelected(option); expanded = false }) }
    }
}

@Composable
private fun ProductList(productos: List<Producto>, onProductSelected: (String) -> Unit) {
    LazyColumn(
        contentPadding = PaddingValues(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(productos, key = { it.id }) { producto ->
            Card(modifier = Modifier.fillMaxWidth().clickable { onProductSelected(producto.id) }) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(producto.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.inventory_code, producto.codigo), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.product_detail_category) + ": " + producto.categoria, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.product_detail_brand) + ": " + producto.marca, style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.inventory_supplier, producto.proveedor.ifBlank { stringResource(R.string.inventory_supplier_missing) }), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.product_detail_channel) + ": " + channelName(producto.canal), style = MaterialTheme.typography.bodyMedium)
                    Text(formatClp(producto.precio), style = MaterialTheme.typography.bodyLarge)
                    Text(stringResource(R.string.inventory_stock, producto.stock), style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun LoadingContent() = MessageContent(R.string.loading)

@Composable
private fun MessageContent(message: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(stringResource(message), style = MaterialTheme.typography.bodyLarge)
    }
}

fun formatClp(value: Long): String = NumberFormat.getCurrencyInstance(Locale("es", "CL")).format(value)

@Composable
private fun channelName(canal: Canal): String = stringResource(when (canal) {
    Canal.SUCURSAL -> R.string.channel_branch
    Canal.ONLINE -> R.string.channel_online
    Canal.AMBOS -> R.string.channel_both
})
