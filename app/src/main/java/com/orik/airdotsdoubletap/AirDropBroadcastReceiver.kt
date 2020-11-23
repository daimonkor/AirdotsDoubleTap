package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AirDropBroadcastReceiver : BroadcastReceiver() {
    companion object {
        const val BATTERY_LEVEL_CHANGED =
            "com.orik.airdotsdoubletap.action.BATTERY_LEVEL_CHANGED"
        const val BLUETOOTH_STATE_CHANGED =
            "com.orik.airdotsdoubletap.action.BLUETOOTH_STATE_CHANGED"
        const val EXTRA_BATTERY_LEVEL_VALUE =
            "com.orik.airdotsdoubletap.extra.EXTRA_BATTERY_LEVEL_VALUE"
        const val EXTRA_BLUETOOTH_STATE = "com.orik.airdotsdoubletap.extra.BLUETOOTH_STATE"
        const val BLUETOOTH_DEVICE_CONNECTION_CHANGED =
            "com.orik.airdotsdoubletap.action.BLUETOOTH_DEVICE_CONNECTION_CHANGED"
        const val EXTRA_BLUETOOTH_DEVICE_CONNECTION_STATE =
            "com.orik.airdotsdoubletap.extra.BLUETOOTH_DEVICE_CONNECTION_STATE"
    }

    override fun onReceive(context: Context, intent: Intent) {
//        Timber.e(
//            "INTENT %s ,bundle: %s",
//            intent.toUri(Intent.URI_ALLOW_UNSAFE), intent.extras.toString()
//        )
        if (BluetoothAdapter.ACTION_STATE_CHANGED == intent.action) {
            if (intent.getIntExtra(
                    BluetoothAdapter.EXTRA_STATE,
                    -1
                ) == BluetoothAdapter.STATE_OFF
            ) {
                context.sendBroadcast(
                    Intent(BLUETOOTH_STATE_CHANGED).putExtra(
                        EXTRA_BLUETOOTH_STATE,
                        BluetoothAdapter.STATE_OFF
                    )
                )
            }
        } else {
            if (intent.action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)) {
                context.sendBroadcast(
                    Intent(BLUETOOTH_DEVICE_CONNECTION_CHANGED).putExtra(
                        EXTRA_BLUETOOTH_DEVICE_CONNECTION_STATE,
                        intent.extras?.getInt(BluetoothHeadset.EXTRA_STATE)
                    ).putExtras(intent.extras!!)
                )
            } else if (intent.action.equals("android.bluetooth.device.action.BATTERY_LEVEL_CHANGED")) {
                context.sendBroadcast(
                    Intent(BATTERY_LEVEL_CHANGED).putExtra(
                        EXTRA_BATTERY_LEVEL_VALUE,
                        hashMapOf(
                            "BATTERY_LEVEL" to intent.extras?.getInt("android.bluetooth.device.extra.BATTERY_LEVEL"),
                            "BLUETOOTH_DEVICE_ADDRESS" to intent.getParcelableExtra<BluetoothDevice>(
                                BluetoothDevice.EXTRA_DEVICE
                            )?.address
                        )
                    ).putExtras(intent.extras!!)
                )
            }
        }
    }
}


