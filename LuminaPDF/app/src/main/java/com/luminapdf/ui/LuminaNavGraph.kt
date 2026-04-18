package com.luminapdf.ui

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.luminapdf.ui.screens.library.LibraryScreen
import com.luminapdf.ui.screens.reader.ReaderScreen
import java.net.URLDecoder
import java.net.URLEncoder

object Routes {
    const val LIBRARY = "library"
    const val READER  = "reader/{uri}"

    fun readerRoute(uri: String): String =
        "reader/${URLEncoder.encode(uri, "UTF-8")}"
}

@Composable
fun LuminaNavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.LIBRARY) {
        composable(Routes.LIBRARY) {
            LibraryScreen(
                onOpenPdf = { uri ->
                    navController.navigate(Routes.readerRoute(uri))
                }
            )
        }
        composable(
            route = Routes.READER,
            arguments = listOf(navArgument("uri") { type = NavType.StringType })
        ) { backStack ->
            val encodedUri = backStack.arguments?.getString("uri") ?: return@composable
            val uri = URLDecoder.decode(encodedUri, "UTF-8")
            ReaderScreen(
                uriString = uri,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
