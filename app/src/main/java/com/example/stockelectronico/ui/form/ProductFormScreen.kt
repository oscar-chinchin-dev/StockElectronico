package com.example.stockelectronico.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.RadioButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockelectronico.R
import com.example.stockelectronico.domain.model.Canal

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun ProductFormScreen(viewModel: ProductFormViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    LaunchedEffect(state.saveSucceeded) {
        if (state.saveSucceeded) {
            viewModel.consumeSaveSucceeded()
            onSaved()
        }
    }
    Scaffold(topBar = {
        TopAppBar(
            title = { Text(stringResource(if (state.isEditMode) R.string.edit_product else R.string.create_product)) },
            navigationIcon = { TextButton(onClick = onBack) { Text(stringResource(R.string.navigation_back)) } }
        )
    }) { padding ->
        when {
            state.isLoading -> FormMessage(R.string.loading, padding)
            state.productNotFound -> FormMessage(R.string.product_edit_not_found, padding)
            else -> ProductForm(state, viewModel, padding)
        }
    }
}

@Composable
private fun ProductForm(state: ProductFormUiState, viewModel: ProductFormViewModel, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(
        modifier = Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        FormField(state.nombre, viewModel::onNombreChanged, R.string.form_name, state.nombreError)
        FormField(state.codigo, viewModel::onCodigoChanged, R.string.form_code, state.codigoError)
        FormField(state.categoria, viewModel::onCategoriaChanged, R.string.form_category, state.categoriaError)
        FormField(state.marca, viewModel::onMarcaChanged, R.string.form_brand, state.marcaError)
        FormField(state.descripcion, viewModel::onDescripcionChanged, R.string.form_description, null, singleLine = false)
        FormField(state.precio, viewModel::onPrecioChanged, R.string.form_price, state.precioError, KeyboardType.Number)
        FormField(state.stock, viewModel::onStockChanged, R.string.form_stock, state.stockError, KeyboardType.Number)
        Text(stringResource(R.string.form_channel), style = MaterialTheme.typography.labelLarge)
        Canal.entries.forEach { canal ->
            androidx.compose.foundation.layout.Row {
                RadioButton(selected = state.canal == canal, onClick = { viewModel.onCanalChanged(canal) })
                TextButton(onClick = { viewModel.onCanalChanged(canal) }) { Text(channelName(canal)) }
            }
        }
        if (state.persistenceError) Text(stringResource(R.string.form_save_error), color = MaterialTheme.colorScheme.error)
        Button(onClick = viewModel::save, enabled = !state.isSaving, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(if (state.isSaving) R.string.saving else R.string.save))
        }
    }
}

@Composable
private fun FormField(value: String, onValueChange: (String) -> Unit, label: Int, error: Int?, keyboardType: KeyboardType = KeyboardType.Text, singleLine: Boolean = true) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text(stringResource(label)) },
        isError = error != null, supportingText = error?.let { { Text(stringResource(it)) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType), singleLine = singleLine,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun channelName(canal: Canal): String = stringResource(when (canal) {
    Canal.SUCURSAL -> R.string.channel_branch
    Canal.ONLINE -> R.string.channel_online
    Canal.AMBOS -> R.string.channel_both
})

@Composable
private fun FormMessage(message: Int, padding: androidx.compose.foundation.layout.PaddingValues) {
    Column(modifier = Modifier.fillMaxSize().padding(padding), verticalArrangement = Arrangement.Center) {
        Text(stringResource(message), modifier = Modifier.padding(16.dp))
    }
}
