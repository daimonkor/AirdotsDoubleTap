package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothDevice

sealed class DataStoreBluetoothDevice
data class Success(val bluetoothDevice: BluetoothDevice) : DataStoreBluetoothDevice()
object Missing : DataStoreBluetoothDevice()
data class Failure(val exception: Throwable) : DataStoreBluetoothDevice()