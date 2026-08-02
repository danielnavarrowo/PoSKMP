package com.dnavarro.poskmp.util

import com.dnavarro.poskmp.db.Products

expect fun currentTimeMillis(): Long
expect fun generateUUID(): String
expect fun isAndroid(): Boolean
expect fun playSoundAlert(bytes: ByteArray)

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

expect fun parseImportFile(
    fileName: String,
    content: ByteArray
): List<Products>


