package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.preferencesKey
import androidx.datastore.preferences.createDataStore
import androidx.lifecycle.lifecycleScope
import eo.view.batterymeter.BatteryMeterView
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


class InfoActivity : AppCompatActivity() {
    private lateinit var dataStore: DataStore<Preferences>

    fun getPairedDevices(): MutableSet<BluetoothDevice>? {
        return  BluetoothAdapter.getDefaultAdapter().bondedDevices
    }

    fun getCachedBtDevice(): BluetoothDevice?{
        lifecycleScope.launch {
            dataStore.data.collect {

                Log.e("EEEEE", it.toString())
                Log.e("EEEE", it.get(preferencesKey<String>("ADDRESS")).toString())
            }
        }

        val bluetoothManager = getSystemService<Any>(Context.BLUETOOTH_SERVICE) as BluetoothManager
        mBluetoothAdapter =

        val device: BluetoothDevice = bluetoothManager.adapter.getRemoteDevice(address)

        lifecycleScope.launch {
            dataStore.edit {
                it[preferencesKey<String>("ADDRESS")] = btDevice.address
                it[preferencesKey<String>("NAME")] = btDevice.name
                it[preferencesKey<String>("ALIAS")] = (btDevice.alias ?: "") as String
            }
        }
    }

    fun cacheBtDevice(btDevice: BluetoothDevice){
        lifecycleScope.launch {
            dataStore.edit {
                it[preferencesKey<String>("ADDRESS")] = btDevice.address
                it[preferencesKey<String>("NAME")] = btDevice.name
                it[preferencesKey<String>("ALIAS")] = (btDevice.alias ?: "") as String
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        dataStore = this.createDataStore(
            name = "btDevice"
        )

        findViewById<TextView>(R.id.textView).text = (getString(
            R.string.info,
            getString(R.string.app_name)
        ))



            lifecycleScope.launch {
                dataStore.data.collect {
                    Log.e("EEEEE", it.toString())
Log.e("EEEE", it.get(preferencesKey<String>("ADDRESS")).toString())
                }
            }
            val invoke: Int? = bt.javaClass.getMethod("getBatteryLevel").invoke(bt) as Int?
            findViewById<BatteryMeterView>(R.id.batteryMeterView).chargeLevel = invoke

                Log.e("AAA", invoke.toString())


        }



    }
}