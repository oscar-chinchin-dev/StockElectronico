package com.example.stockelectronico.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockelectronico.R
import com.example.stockelectronico.domain.model.Producto
import com.example.stockelectronico.domain.model.Canal
import com.example.stockelectronico.ui.inventory.formatClp

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProductDetailScreen(viewModel: ProductDetailViewModel, onBack: () -> Unit, onEdit: (String) -> Unit, onDeleted: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val deleteState = viewModel.deleteState.collectAsStateWithLifecycle().value
    val showDeleteDialog = remember { mutableStateOf(false) }
    LaunchedEffect(deleteState.succeeded) { if (deleteState.succeeded) onDeleted() }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.product_detail_title)) },
                navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back)) } }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> DetailMessage(R.string.loading, padding)
            state.hasError -> DetailMessage(R.string.product_detail_error, padding)
            state.producto == null -> DetailMessage(R.string.product_detail_not_found, padding)
            else -> ProductDetail(state.producto, padding, onEdit = { onEdit(state.producto.id) }, onDelete = { showDeleteDialog.value = true }, deleteError = deleteState.hasError)
        }
    }
    if (showDeleteDialog.value) AlertDialog(
        onDismissRequest = { if (!deleteState.isDeleting) showDeleteDialog.value = false },
        title = { Text(stringResource(R.string.delete_product_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.delete_product_message))
                if (deleteState.hasError) Text(stringResource(R.string.delete_product_error), color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = { Button(onClick = viewModel::delete, enabled = !deleteState.isDeleting) { Text(stringResource(if (deleteState.isDeleting) R.string.deleting else R.string.delete)) } },
        dismissButton = { TextButton(onClick = { showDeleteDialog.value = false }, enabled = !deleteState.isDeleting) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun DetailMessage(message: Int, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) { Text(stringResource(message), style = MaterialTheme.typography.bodyLarge) }
}

@Composable
private fun ProductDetail(producto: Producto, padding: androidx.compose.foundation.layout.PaddingValues, onEdit: () -> Unit, onDelete: () -> Unit, deleteError: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(producto.nombre, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                DetailField(R.string.product_detail_code, producto.codigo)
                DetailField(R.string.product_detail_category, producto.categoria)
                DetailField(R.string.product_detail_brand, producto.marca)
                DetailField(R.string.product_detail_description, producto.descripcion)
                DetailField(R.string.product_detail_price, formatClp(producto.precio))
                DetailField(R.string.product_detail_stock, producto.stock.toString())
                DetailField(R.string.product_detail_channel, channelName(producto.canal))
                Button(onClick = onEdit, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.edit_product)) }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.delete)) }
                if (deleteError) Text(stringResource(R.string.delete_product_error), color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun channelName(canal: Canal): String = stringResource(
    when (canal) {
        Canal.SUCURSAL -> R.string.channel_branch
        Canal.ONLINE -> R.string.channel_online
        Canal.AMBOS -> R.string.channel_both
    }
)

@Composable
private fun DetailField(label: Int, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(stringResource(label), style = MaterialTheme.typography.labelLarge)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}
