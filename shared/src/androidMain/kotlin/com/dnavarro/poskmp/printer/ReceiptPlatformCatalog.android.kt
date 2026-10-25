package com.dnavarro.poskmp.printer

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.printservice.PrintService
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.core.content.ContextCompat
import com.dnavarro.poskmp.db.DatabaseDriverFactory
import com.dnavarro.poskmp.domain.model.ReceiptPrinterOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual fun hasBluetoothPrinterPermission(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    val context = DatabaseDriverFactory.appContext ?: return false
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.BLUETOOTH_CONNECT
    ) == PackageManager.PERMISSION_GRANTED
}

actual fun openBluetoothSettings() {
    val context = DatabaseDriverFactory.appContext ?: return
    try {
        val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
actual fun rememberBluetoothPermissionRequester(onPermissionResult: (Boolean) -> Unit): () -> Unit {
    val currentCallback by rememberUpdatedState(onPermissionResult)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        currentCallback(granted)
    }
    return {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || hasBluetoothPrinterPermission()) {
            currentCallback(true)
        } else {
            launcher.launch(Manifest.permission.BLUETOOTH_CONNECT)
        }
    }
}

@SuppressLint("MissingPermission")
actual suspend fun getReceiptPrinterOptions(): List<ReceiptPrinterOption> = withContext(Dispatchers.IO) {
    val context = DatabaseDriverFactory.appContext ?: return@withContext emptyList()
    val options = mutableListOf<ReceiptPrinterOption>()

    // 1. Paired Bluetooth devices (SPP thermal printers)
    if (hasBluetoothPrinterPermission()) {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val bondedDevices = runCatching {
            bluetoothManager?.adapter?.bondedDevices?.toList()
        }.getOrNull().orEmpty()

        val sortedBluetoothDevices = bondedDevices.sortedWith(
            compareByDescending<android.bluetooth.BluetoothDevice> { device ->
                val majorClass = runCatching { device.bluetoothClass?.majorDeviceClass }.getOrNull()
                val name = runCatching { device.name }.getOrNull().orEmpty().lowercase()
                majorClass == BluetoothClass.Device.Major.IMAGING ||
                    majorClass == BluetoothClass.Device.Major.UNCATEGORIZED ||
                    name.contains("print") ||
                    name.contains("pos") ||
                    name.contains("mtp") ||
                    name.contains("mpt") ||
                    name.contains("rp") ||
                    name.contains("bt") ||
                    name.contains("thermal")
            }.thenBy { device ->
                runCatching { device.name }.getOrNull().orEmpty().lowercase()
            }
        )

        sortedBluetoothDevices.forEach { device ->
            val address = device.address ?: return@forEach
            val deviceName = runCatching { device.name }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Dispositivo BT"
            options += ReceiptPrinterOption(
                id = "bt://$address",
                name = "Bluetooth: $deviceName ($address)",
                isDefault = false
            )
        }
    }

    // 2. Android System Print Services
    val services = runCatching {
        context.packageManager.queryIntentServices(
            Intent(PrintService.SERVICE_INTERFACE),
            0
        )
    }.getOrDefault(emptyList())
    val systemOptions = services.map { resolveInfo ->
        val serviceInfo = resolveInfo.serviceInfo
        ReceiptPrinterOption(
            id = ComponentName(serviceInfo.packageName, serviceInfo.name).flattenToString(),
            name = resolveInfo.loadLabel(context.packageManager).toString()
        )
    }.distinctBy { it.id }.sortedBy { it.name.lowercase() }

    options += systemOptions
    options.distinctBy { it.id }
}
