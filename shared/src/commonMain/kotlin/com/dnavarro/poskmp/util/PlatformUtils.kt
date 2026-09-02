package com.dnavarro.poskmp.util

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.ThreePaneScaffoldNavigator
import androidx.compose.runtime.Composable
import com.dnavarro.poskmp.db.Products

expect fun currentTimeMillis(): Long
expect fun generateUUID(): String
expect fun isAndroid(): Boolean
expect fun playSoundAlert(bytes: ByteArray)

@Composable
expect fun PlatformBackHandler(enabled: Boolean = true, onBack: () -> Unit)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
expect fun <T> AdaptiveScaffoldPredictiveBackHandler(
    navigator: ThreePaneScaffoldNavigator<T>,
    backBehavior: BackNavigationBehavior = BackNavigationBehavior.PopUntilScaffoldValueChange,
)

expect fun pickFile(
    allowedExtensions: List<String>,
    onFilePicked: (fileName: String, content: ByteArray) -> Unit,
    onError: (String) -> Unit
)

expect fun saveFile(
    defaultFileName: String,
    content: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
)

expect fun pickDirectory(
    initialPath: String,
    onDirectoryPicked: (path: String) -> Unit,
    onError: (String) -> Unit
)

expect fun parseImportFile(
    fileName: String,
    content: ByteArray
): List<Products>


