package com.orik.airdotsdoubletap.batterylevel

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import android.content.Context
import com.icebergteam.timberjava.Timber
import java.io.IOException
import java.util.*

class BluetoothBatteryLevel(private val handler: OnChangeCheckBatteryState) :
    OnChangeCheckBatteryState {
    private val HFS_UUID = UUID.fromString("0000111e-0000-1000-8000-00805f9b34fb")
    private var mm_state: State = State.UNKNOWN
    private lateinit var mConnectedThread: ConnectedThread

    private fun reconnectDevice(
        bluetoothDevice: BluetoothDevice?,
        bluetoothProfile: BluetoothProfile?
    ): BluetoothSocket? {
        var bluetoothSocket: BluetoothSocket?
        var connectStatus: Boolean = false
        if (bluetoothDevice == null || bluetoothProfile == null) {
            return null
        }
        this.mm_state = State.CONNECTING
        try {
            Timber.i(
                "disconnect():" + bluetoothProfile.javaClass.getMethod(
                    "disconnect",
                    BluetoothDevice::class.java
                ).invoke(bluetoothProfile, bluetoothDevice) as Boolean
            )
        } catch (e3: java.lang.Exception) {
            Timber.e("disconnect() was failed:" + e3.message)
        }
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(HFS_UUID)
            try {

                bluetoothSocket.connect()
                this.mm_state = State.CONNECTED
                Timber.e("ret of reconnect():" + bluetoothSocket.isConnected)

            } catch (e7: IOException) {

            } catch (e8: Exception) {

                Timber.e("" + e8.message)
                try {
                    connectStatus = (bluetoothProfile.javaClass.getMethod(
                        "connect",
                        BluetoothDevice::class.java
                    ).invoke(bluetoothProfile, bluetoothDevice) as Boolean)
                } catch (unused: java.lang.Exception) {
                    Timber.e("connect() was failed:" + e8.message)
                }
                Timber.e("connect():$connectStatus")
                return bluetoothSocket
            }
        } catch (e9: IOException) {
            bluetoothSocket = null
            Timber.e("" + e9.message)
            try {
                connectStatus =
                    (bluetoothProfile.javaClass.getMethod("connect", BluetoothDevice::class.java)
                        .invoke(bluetoothProfile, bluetoothDevice) as Boolean)
            } catch (unused4: java.lang.Exception) {
                Timber.e("connect() was failed:" + e9.message)
            }
            Timber.e("connect():$connectStatus")
            return bluetoothSocket
        } catch (e10: Exception) {
            bluetoothSocket = null
            Timber.e(e10.message)
            connectStatus =
                (bluetoothProfile.javaClass.getMethod("connect", BluetoothDevice::class.java)
                    .invoke(bluetoothProfile, bluetoothDevice) as Boolean)
            Timber.e("connect():$connectStatus")
            return bluetoothSocket
        }
        return bluetoothSocket
    }


    fun get_headset_profile(bluetoothDevice: BluetoothDevice, context: Context) {
        val defaultAdapter = BluetoothAdapter.getDefaultAdapter()
        Timber.e("DDDDDDD")
        if (defaultAdapter == null) {
            handler.onBatteryLevel(-1)
        } else if (defaultAdapter.getProfileConnectionState(BluetoothProfile.HEADSET) != BluetoothProfile.STATE_CONNECTED && defaultAdapter.getProfileConnectionState(
                BluetoothProfile.A2DP
            ) != BluetoothProfile.STATE_CONNECTED
        ) {
            Timber.e("They are nor connected HEADSET nor A2DP")
        } else if (!defaultAdapter.getProfileProxy(context, object :
                BluetoothProfile.ServiceListener {

                override fun onServiceConnected(i: Int, bluetoothProfile: BluetoothProfile) {
                    Timber.e(
                        "%s --- onServiceConnected() in get_bluetooth_profile()", i
                    )
                    when (i) {
                        BluetoothProfile.HEADSET, BluetoothProfile.A2DP -> {
                            Timber.e("Got a headset OR A2DP successfully")
                            getBatteryInfoUsingAT(bluetoothDevice, bluetoothProfile)
                        }
                        else -> {
                            Timber.e("Failed to get a headset: profile:$i")
                        }
                    }
                }

                override fun onServiceDisconnected(i: Int) {
                    Timber.e(
                        "onServiceDisconnected() in get_bluetooth_profile() profile:$i"
                    )
                }
            }, BluetoothProfile.HEADSET)) {
            Timber.e("getProfileProxy() was failed")
        }
    }

    private fun getBatteryInfoUsingAT(
        bluetoothDevice: BluetoothDevice?,
        bluetoothProfile: BluetoothProfile?
    ) {

        if (bluetoothProfile == null) {
            Timber.e("Bluetooth profile is null in getBatteryInfoUsingAT()")
        } else if (bluetoothDevice == null) {
            Timber.e("Bluetooth device is null in getBatteryInfoUsingAT()")
        } else {
            var index = 0
            var bluetoothSocket: BluetoothSocket? = null
            while (bluetoothSocket == null && index < 5) {
                bluetoothSocket = reconnectDevice(bluetoothDevice, bluetoothProfile)
                index++
            }
            if (bluetoothSocket == null) {
                Timber.e("socket connection was failed")
                return
            }
            this.mConnectedThread =
                ConnectedThread(bluetoothSocket, bluetoothProfile, bluetoothDevice, this).also {
                    it.start()
                }
        }
    }

    override fun currentState(): State = this.mm_state

    override fun changeState(state: State) {
        this.mm_state = state
    }

    override fun onBatteryLevel(level: Int) {
        handler.onBatteryLevel(level)
    }


}

enum class State(status: Int) {
    DISCONNECT(0),
    CONNECTING(1),
    CONNECTED(2),
    UNKNOWN(3)
}

interface OnChangeCheckBatteryState {
    fun currentState(): State
    fun changeState(state: State)
    fun onBatteryLevel(level: Int)
}