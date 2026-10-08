package com.example.stockelectronico.ui.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.stockelectronico.domain.repository.ProductoRepository
import com.example.stockelectronico.ui.detail.ProductDetailScreen
import com.example.stockelectronico.ui.detail.ProductDetailViewModel
import com.example.stockelectronico.ui.detail.ProductDetailViewModelFactory
import com.example.stockelectronico.ui.inventory.InventoryScreen
import com.example.stockelectronico.ui.inventory.InventoryViewModel
import com.example.stockelectronico.ui.inventory.InventoryViewModelFactory
import com.example.stockelectronico.ui.form.ProductFormScreen
import com.example.stockelectronico.ui.form.ProductFormViewModel
import com.example.stockelectronico.ui.form.ProductFormViewModelFactory

private const val INVENTORY_ROUTE = "inventory"
private const val DETAIL_ROUTE = "product_detail"
private const val PRODUCT_ID_ARGUMENT = "productId"
private const val CREATE_ROUTE = "product_create"
private const val EDIT_ROUTE = "product_edit"

@Composable
fun StockElectronicoNavHost(productoRepository: ProductoRepository) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = INVENTORY_ROUTE
    ) {
        composable(INVENTORY_ROUTE) {
            val viewModel: InventoryViewModel = viewModel(
                factory = InventoryViewModelFactory(productoRepository)
            )
            InventoryScreen(viewModel, onProductSelected = { productId -> navController.navigate("$DETAIL_ROUTE/${Uri.encode(productId)}") }, onCreateProduct = { navController.navigate(CREATE_ROUTE) })
        }
        composable(
            route = "$DETAIL_ROUTE/{$PRODUCT_ID_ARGUMENT}",
            arguments = listOf(navArgument(PRODUCT_ID_ARGUMENT) { type = NavType.StringType })
        ) { entry ->
            val productId = entry.arguments
                ?.getString(PRODUCT_ID_ARGUMENT)
                ?.let { encodedId -> runCatching { Uri.decode(encodedId) }.getOrNull() }
                .orEmpty()
            val viewModel: ProductDetailViewModel = viewModel(
                key = productId,
                factory = ProductDetailViewModelFactory(productId, productoRepository)
            )
            ProductDetailScreen(viewModel, onBack = navController::popBackStack, onEdit = { id -> navController.navigate("$EDIT_ROUTE/${Uri.encode(id)}") }, onDeleted = { navController.popBackStack(INVENTORY_ROUTE, false) })
        }
        composable(CREATE_ROUTE) {
            val viewModel: ProductFormViewModel = viewModel(factory = ProductFormViewModelFactory(null, productoRepository))
            ProductFormScreen(viewModel, onBack = navController::popBackStack, onSaved = { navController.popBackStack(INVENTORY_ROUTE, false) })
        }
        composable(route = "$EDIT_ROUTE/{$PRODUCT_ID_ARGUMENT}", arguments = listOf(navArgument(PRODUCT_ID_ARGUMENT) { type = NavType.StringType })) { entry ->
            val productId = entry.arguments?.getString(PRODUCT_ID_ARGUMENT)?.let { runCatching { Uri.decode(it) }.getOrNull() }.orEmpty()
            val viewModel: ProductFormViewModel = viewModel(key = "edit-$productId", factory = ProductFormViewModelFactory(productId, productoRepository))
            ProductFormScreen(viewModel, onBack = navController::popBackStack, onSaved = { navController.popBackStack() })
        }
    }
}
