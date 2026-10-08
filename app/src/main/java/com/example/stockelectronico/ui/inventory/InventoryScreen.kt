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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockelectronico.R
import com.example.stockelectronico.domain.model.Producto
import java.text.NumberFormat
import java.util.Locale

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun InventoryScreen(viewModel: InventoryViewModel, onProductSelected: (String) -> Unit, onCreateProduct: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    Scaffold(
        topBar = { TopAppBar(title = { Text(stringResource(R.string.inventory_title)) }) },
        floatingActionButton = { FloatingActionButton(onClick = onCreateProduct) { Text(stringResource(R.string.create_product_short)) } }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChanged,
                label = { Text(stringResource(R.string.inventory_search_label)) },
                placeholder = { Text(stringResource(R.string.inventory_search_placeholder)) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                singleLine = true
            )
            when {
                state.isLoading -> LoadingContent()
                state.hasError -> MessageContent(R.string.inventory_error)
                state.productos.isEmpty() -> MessageContent(
                    if (state.query.isBlank()) R.string.inventory_empty else R.string.inventory_no_results
                )
                else -> ProductList(state.productos, onProductSelected)
            }
        }
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
