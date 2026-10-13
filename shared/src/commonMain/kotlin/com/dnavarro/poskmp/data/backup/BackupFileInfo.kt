package com.dnavarro.poskmp.data.backup

data class BackupFileInfo(
    val fileName: String,
    val filePath: String,
    val sizeBytes: Long,
    val lastModifiedEpochMillis: Long
)
