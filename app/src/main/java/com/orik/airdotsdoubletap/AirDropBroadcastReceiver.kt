package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.icebergteam.timberjava.Timber
import com.orik.airdotsdoubletap.service.Actions
import com.orik.airdotsdoubletap.service.NotificationService
import com.orik.airdotsdoubletap.service.ServiceState
import com.orik.airdotsdoubletap.service.getServiceState
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


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
        const val REFRESH = "com.orik.airdotsdoubletap.action.REFRESH"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Timber.e(
            "INTENT %s ,bundle: %s",
            intent.toUri(Intent.URI_ALLOW_UNSAFE), intent.extras.toString()
        )

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
//                actionOnService(context, Actions.STOP, intent.extras)
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
                val batteryLevel =
                    intent.extras?.getInt("android.bluetooth.device.extra.BATTERY_LEVEL")
                context.sendBroadcast(
                    Intent(BATTERY_LEVEL_CHANGED).putExtra(
                        EXTRA_BATTERY_LEVEL_VALUE,
                        hashMapOf(
                            "BATTERY_LEVEL" to batteryLevel,
                            "BLUETOOTH_DEVICE_ADDRESS" to intent.getParcelableExtra<BluetoothDevice>(
                                BluetoothDevice.EXTRA_DEVICE
                            )?.address
                        )
                    ).putExtras(intent.extras!!)
                )
            }
            callCheckDevice(context, intent)

        }

    }

    private fun callCheckDevice(context: Context, intent: Intent) {

        GlobalScope.launch {
            val settings = Settings(context)
            settings.getCachedBtDevice().collect {
                Timber.e("DEVICE %s", it)
                when (it) {
                    is Success -> {
                        if (it.bluetoothDevice.isConnected() == true) {
                            /*      it.bluetoothDevice.batteryLevel(context).collect {
                                      if (it !== -1) {
                                          actionOnService(context, Actions.START, Bundle().apply {
                                              putInt(
                                                  "android.bluetooth.device.extra.BATTERY_LEVEL",
                                                  it!!
                                              )
                                          })
                                      }
                                  }*/
                        } else {
                            actionOnService(context, Actions.STOP, intent.extras)
                        }
                    }
                    else -> {
                        actionOnService(context, Actions.STOP, intent.extras)
                    }
                }
            }

        }
    }

    private fun actionOnService(context: Context, action: Actions, bundle: Bundle?) {

        try {
            if (getServiceState(context) == ServiceState.STOPPED && action == Actions.STOP) return
            Timber.e("EEEEEEE %s", action)
            Intent(context, NotificationService::class.java).also {
                it.action = action.name
                bundle?.let { bundle ->
                    it.putExtras(bundle)
                }
//                ContextCompat.startForegroundService(context, it)
//                /* if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//
//                     context.startForegroundService(it)
//                     return
//                 }
                context.startService(it)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}


