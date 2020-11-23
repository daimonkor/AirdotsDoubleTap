package com.orik.airdotsdoubletap

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.content.*
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.icebergteam.timberjava.LineNumberDebugTree
import com.icebergteam.timberjava.Timber
import com.orik.airdotsdoubletap.AirDropBroadcastReceiver.Companion.EXTRA_BATTERY_LEVEL_VALUE
import com.orik.airdotsdoubletap.databinding.ActivityInfoBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch


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
        initLogger()
        binding = ActivityInfoBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        settings = Settings(this)
        binding.textView.text = (getString(
            R.string.info,
            getString(R.string.app_name)
        ))
    }

    private fun initLogger() {
        Timber.plant(object : LineNumberDebugTree() {
            override fun createStackElementTag(element: StackTraceElement): String? {
                var tag = element.className
                val m = ANONYMOUS_CLASS.matcher(tag)
                if (m.find()) {
                    tag = m.replaceAll("")
                }
                tag = tag.substring(tag.lastIndexOf('.') + 1)
                // Tag length limit was removed in API 24.
                if (tag.length <= MAX_TAG_LENGTH || Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    return String.format("%s (%s)", tag, element.lineNumber)
                }
                val className = tag.substring(0, MAX_TAG_LENGTH).split("$").toTypedArray()[0]
                return String.format(
                    "(%s.kt:%s#%s",
                    className,
                    element.lineNumber,
                    element.methodName
                )
            }

            override fun wtf(tag: String, message: String) {
                Log.wtf(tag, message)
            }

            override fun println(priority: Int, tag: String, message: String) {
                Log.println(priority, tag, message)
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (!isBluetoothOn) {
            Snackbar.make(binding.root, "Bluetooth is no enabled", LENGTH_LONG).show()
        }
        val filter = IntentFilter()
        filter.addAction(AirDropBroadcastReceiver.BLUETOOTH_DEVICE_CONNECTION_CHANGED)
        filter.addAction(AirDropBroadcastReceiver.BATTERY_LEVEL_CHANGED)
        filter.addAction(AirDropBroadcastReceiver.BLUETOOTH_STATE_CHANGED)
        this.registerReceiver(broadcastReceiver, filter)

        currentBluetoothDevice.observe(this) {
            if (it != null) {
                binding.batteryMeterView.chargeLevel =
                    it.batteryLevel()
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
            when (it) {
                is Success -> {
                    currentBluetoothDevice.postValue(it.bluetoothDevice)
                }
                Missing -> currentBluetoothDevice.postValue(null)
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

fun BluetoothDevice.batteryLevel() =
    this.javaClass.getMethod("getBatteryLevel").invoke(this) as Int?
