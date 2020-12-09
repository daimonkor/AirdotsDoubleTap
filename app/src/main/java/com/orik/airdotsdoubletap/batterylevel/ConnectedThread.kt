package com.orik.airdotsdoubletap.batterylevel

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothSocket
import com.icebergteam.timberjava.Timber
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Method

class ConnectedThread(
    private val bluetoothSocket: BluetoothSocket,
    private val bluetoothProfile: BluetoothProfile,
    private val bluetoothDevice: BluetoothDevice,
    private val stateHandler: OnChangeCheckBatteryState
) :
    Thread() {

    private var mmInStream: InputStream? = null
    private var mmOutStream: OutputStream? = null
    override fun run() {
        if (mmInStream == null || mmOutStream == null) {
            Timber.e("mmInStream or mmOutStream is null")
            cancel()
            return
        }
        Timber.e("BEGIN mConnectedThread")
        val bArr = ByteArray(64)
        while (true) {
            if (this.stateHandler.currentState() !== com.orik.airdotsdoubletap.batterylevel.State.CONNECTED) {
                break
            }
            try {
                bArr[mmInStream!!.read(bArr)] = 10
                val str = String(bArr)
                Timber.e("recvMsg:$str")
                if (str.contains("BRSF")) {
                    write("+CIND: (\"service\",(0,1)),(\"call\",(0,1))")
                } else if (str.contains("CIND=?")) {
                    write("+CIND: 1,0")
                } else if (!str.contains("CMER?")) {
                    if (str.contains("AT+CMER=3,0,0,1")) {
                        write("+XAPL= iPhone,1")
                    } else if (!str.contains("BAC")) {
                        if (!str.contains("NREC")) {
                            if (!str.contains("VGS")) {
                                write("+BRSF: 0")
                            }
                        }
                    }
                }
                write("OK")
                if (str.contains("IPHONEACCEV")) {
                    parseBattInfo(str)
                    cancel()
                    break
                }
            } catch (e: IOException) {
                Timber.e("" + e.message)
                cancel()
            }
        }
        if (this.stateHandler.currentState() === com.orik.airdotsdoubletap.batterylevel.State.CONNECTED) {
            cancel()
        }
    }

    private fun parseBattInfo(str: String) {
        var i: Int = -1
        Timber.e("come in parseBatt:$str")
        val split = str.split(",").toTypedArray()
        if (split.size >= 2) {
            try {
                var str2 = split[2]
                if (str2.length > 1) {
                    str2 = str2.substring(0, 1)
                }
                i = (str2.toInt() + 1) * 10
            } catch (e: NumberFormatException) {
                Timber.e(
                    "Error is occured in parseBattInfo():" + e.message
                )
            }
            Timber.e("received msg:$i")
            this.stateHandler.onBatteryLevel(i)
        }
        i = -1
        Timber.e("received msg:$i")
        this.stateHandler.onBatteryLevel(i)
    }

    private fun write(str: String?) {
        try {
            val sb = StringBuilder()
            sb.append("\r\n")
            sb.append(str)
            sb.append("\r\n")
            mmOutStream?.write(sb.toString().toByteArray())
            Timber.e("write():$sb")
        } catch (e: IOException) {
            Timber.e("Exception during write", e)
        }
    }

    private fun cancel() {
        this.stateHandler.changeState(com.orik.airdotsdoubletap.batterylevel.State.DISCONNECT)
        try {
            bluetoothSocket.close()
        } catch (e: IOException) {
            Timber.e("socket close() was failed:" + e.message)
        }
        try {
            val method: Method =
                bluetoothProfile.javaClass.getMethod("connect", BluetoothDevice::class.java)
            Timber.e(
                "connect():" + method.invoke(bluetoothProfile, bluetoothDevice) as Boolean
            )
        } catch (e2: java.lang.Exception) {
            Timber.e("profile connect() was failed:" + e2.message)
        }
        Timber.e("socket was closed, released bluetooth profile")
    }

    init {
        try {
            mmInStream = bluetoothSocket.inputStream
            try {
                mmOutStream = bluetoothSocket.outputStream
            } catch (e2: IOException) {

            }
        } catch (e3: IOException) {
            Timber.e("tmp sockets not created", e3)
            this.stateHandler.changeState(com.orik.airdotsdoubletap.batterylevel.State.CONNECTED)
        }
        this.stateHandler.changeState(com.orik.airdotsdoubletap.batterylevel.State.CONNECTED)
    }
}