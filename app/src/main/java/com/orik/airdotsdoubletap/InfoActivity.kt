package com.orik.airdotsdoubletap

import android.bluetooth.*
import android.content.*
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.Button
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.BaseTransientBottomBar.LENGTH_LONG
import com.google.android.material.snackbar.Snackbar
import com.icebergteam.timberjava.Timber
import com.orik.airdotsdoubletap.AirDropBroadcastReceiver.Companion.EXTRA_BATTERY_LEVEL_VALUE
import com.orik.airdotsdoubletap.batterylevel.BluetoothBatteryLevel
import com.orik.airdotsdoubletap.batterylevel.OnChangeCheckBatteryState
import com.orik.airdotsdoubletap.batterylevel.State
import com.orik.airdotsdoubletap.databinding.ActivityInfoBinding
import com.orik.airdotsdoubletap.service.NotificationService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.sendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import java.util.*


class InfoActivity : AppCompatActivity() {
    private lateinit var binding: ActivityInfoBinding
    private var currentBluetoothDevice = MutableLiveData<BluetoothDevice>()
    private lateinit var settings: Settings


    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            if (intent.action.equals(AirDropBroadcastReceiver.BLUETOOTH_STATE_CHANGED)) {
                if (intent.getIntExtra(
                        AirDropBroadcastReceiver.EXTRA_BLUETOOTH_STATE,
                        -1
                    ) == BluetoothAdapter.STATE_OFF
                ) {
                    binding.batteryMeterView.chargeLevel = null
                    Snackbar.make(binding.root, "Bluetooth is no enabled", LENGTH_LONG).show()
                }
            } else {
                GlobalScope.launch(Dispatchers.IO) {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    settings.getCachedBtDevice().collect {
                        if (it is Success && it.bluetoothDevice.address == device?.address) {
                            if (intent.action.equals(AirDropBroadcastReceiver.BLUETOOTH_DEVICE_CONNECTION_CHANGED)) {
                                when (intent.extras?.getInt(
                                    AirDropBroadcastReceiver.EXTRA_BLUETOOTH_DEVICE_CONNECTION_STATE,
                                    -1
                                )) {
                                    BluetoothProfile.STATE_CONNECTED -> {
                                        Snackbar.make(
                                            binding.root,
                                            "\"${device?.name}\" is connected",
                                            LENGTH_LONG
                                        ).show()
                                    }
                                    BluetoothProfile.STATE_DISCONNECTED -> {
                                        Snackbar.make(
                                            binding.root,
                                            "\"${device?.name}\" is disconnected",
                                            LENGTH_LONG
                                        ).show()
                                    }
                                    else -> {
                                    }

                                }
                            } else if (intent.action.equals(AirDropBroadcastReceiver.BATTERY_LEVEL_CHANGED)) {
                                binding.batteryMeterView.chargeLevel =
                                    (intent.extras?.getSerializable(EXTRA_BATTERY_LEVEL_VALUE) as HashMap<*, *>)["BATTERY_LEVEL"] as Int?
                            }
                        }
                    }
                }
            }
        }
    }

    private val pairedDevices: MutableSet<BluetoothDevice>?
        get() {
            return BluetoothAdapter.getDefaultAdapter().bondedDevices
        }

    private fun generatePairedDevicesDialog(callback: DialogInterface.OnClickListener): AlertDialog {
        val builder: AlertDialog.Builder = AlertDialog.Builder(this)
        builder.setTitle("Select your bluetooth headphone")

        if (pairedDevices != null && pairedDevices?.size!! > 0) {
            builder.setItems(
                pairedDevices!!.map {
                    it.name
                }.toTypedArray(), callback
            )

        } else {
            builder.setMessage("Please pair your headphone")
            builder.setNeutralButton(
                "Ok"
            ) { dialog, _ -> dialog.dismiss() }
        }
        return builder.create()
    }

    private val isBluetoothOn: Boolean
        get() {
            return BluetoothAdapter.getDefaultAdapter().isEnabled
        }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.right_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.action_home -> {
                val linLayout = LinearLayout(this)
                linLayout.orientation = LinearLayout.VERTICAL
                val linLayoutParam =
                    LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                val button: Button = Button(this).apply {
                    this.text = "Clear"
                    this.isEnabled = isBluetoothOn
                }
                linLayout.addView(button, linLayoutParam)
                val create =
                    AlertDialog.Builder(this).setTitle("Settings").setView(linLayout).create()

                button.setOnClickListener {
                    lifecycleScope.launch {
                        settings.clear()
                        create.dismiss()
                    }
                }
                create.show()


            }

        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = Settings(this)
        binding.textView.text = (getString(
            R.string.info,
            getString(R.string.app_name)
        ))
    }

    /**
     * Check if the service is Running
     * @param serviceClass the class of the Service
     *
     * @return true if the service is running otherwise false
     */
    fun checkServiceRunning(serviceClass: Class<*>): Boolean {
//        val manager = getSystemService(ACTIVITY_SERVICE) as ActivityManager
//        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
//            Timber.e("SSSS %s", service.clientCount)
//            if (serviceClass.name == service.service.className) {
//                return true
//            }
//        }
        return false
    }


    override fun onResume() {
        super.onResume()
        if (!isBluetoothOn) {
            Snackbar.make(binding.root, "Bluetooth is no enabled", LENGTH_LONG).show()
        }

        checkServiceRunning(NotificationService::class.java)

        val filter = IntentFilter()
        filter.addAction(AirDropBroadcastReceiver.BLUETOOTH_DEVICE_CONNECTION_CHANGED)
        filter.addAction(AirDropBroadcastReceiver.BATTERY_LEVEL_CHANGED)
        filter.addAction(AirDropBroadcastReceiver.BLUETOOTH_STATE_CHANGED)
        this.registerReceiver(broadcastReceiver, filter)

        currentBluetoothDevice.observe(this) {
            if (it != null) {
                Timber.e("DDDDDD")
                lifecycleScope.launch {
                    it.batteryLevel(this@InfoActivity).collect {
                        it?.let { binding.batteryMeterView.chargeLevel = it }
                    }
                }


                if (it.isConnected() != true) {
                    Snackbar.make(
                        binding.root,
                        "\"${it.name}\" is not connected",
                        LENGTH_LONG
                    ).show()
                }
            } else {
                binding.batteryMeterView.chargeLevel = null
                generatePairedDevicesDialog { dialog, which ->
                    lifecycleScope.launch {
                        settings.cacheBtDevice(pairedDevices!!.toList()[which])
                        dialog.dismiss()
                    }
                }.show()
            }
        }
        settings.getCachedBtDevice().asLiveData().observe(this) {
            Timber.e("DataStore: %s", it)
            sendBroadcast(
                Intent(
                    this@InfoActivity,
                    AirDropBroadcastReceiver::class.java
                ).setAction("com.orik.airdotsdoubletap.action.REFRESH")
            )
            when (it) {
                is Success -> {
                    currentBluetoothDevice.postValue(it.bluetoothDevice)
                }
                Missing -> {
                    currentBluetoothDevice.postValue(null)
                }
                is Failure -> {
                    when (it.exception) {
                        is BluetoothEnableError -> Snackbar.make(
                            binding.root,
                            "Bluetooth is no enabled",
                            LENGTH_LONG
                        ).show()
                        else -> Snackbar.make(
                            binding.root,
                            it.exception.message ?: "Error",
                            LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        this.unregisterReceiver(broadcastReceiver)
    }

}


fun BluetoothDevice.isConnected() = this.javaClass.getMethod("isConnected")
    .invoke(this) as Boolean?


@ExperimentalCoroutinesApi
fun BluetoothDevice.batteryLevel(context: Context): Flow<Int?> {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        return flow {
            emit(
                this@batteryLevel.javaClass.getMethod("getBatteryLevel")
                    .invoke(this@batteryLevel) as Int?
            )
        }
    }

    return callbackFlow {
        BluetoothBatteryLevel(object : OnChangeCheckBatteryState {
            override fun currentState(): State {
                return State.UNKNOWN
            }

            override fun changeState(state: State) {

            }

            override fun onBatteryLevel(level: Int) {
                if (level != -1) {
                    sendBlocking(level)
                    channel.close()
                }

            }

        }).get_headset_profile(this@batteryLevel, context)
        awaitClose { }
    }
}


