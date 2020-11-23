package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class Settings(private var context: Context) {
    private val dataStore: DataStore<Preferences> = context.createDataStore(
        name = "settings"
    )

    suspend fun cacheBtDevice(btDevice: BluetoothDevice) {
        dataStore.edit {
            it[preferencesKey<String>("ADDRESS")] = btDevice.address
            it[preferencesKey<String>("NAME")] = btDevice.name
            it[preferencesKey<String>("ALIAS")] = (btDevice.alias ?: "") as String
        }
    }

    suspend fun clear() {
        dataStore.updateData {
            emptyPreferences()
        }
    }

    private val isBluetoothOn: Boolean
        get() {
            return BluetoothAdapter.getDefaultAdapter().isEnabled
        }

    fun getCachedBtDevice(): Flow<DataStoreBluetoothDevice> =
        dataStore.data.map {
            if (it == emptyPreferences()) {
                return@map Missing
            }

            if (!isBluetoothOn) {
                throw BluetoothEnableError()
            }

            return@map Success(
                (context.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter.getRemoteDevice(
                    it[preferencesKey<String>(
                        "ADDRESS"
                    )]
                )
            )
        }.catch { exception ->
            emit(Failure(exception))
        }
}