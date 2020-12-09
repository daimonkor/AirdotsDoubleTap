package com.orik.airdotsdoubletap.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.orik.airdotsdoubletap.MainActivity
import com.orik.airdotsdoubletap.R
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


class NotificationService : Service() {

    private var intent: Intent? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var isServiceStarted = false

    override fun onBind(intent: Intent): IBinder? {
        appendLog("Some component want to bind with the service")
        // We don't provide binding, so return null
        return null
    }

    private fun appendLog(text: String?) {
        val logFile = File("sdcard/log.file")
        if (!logFile.exists()) {
            try {
                logFile.createNewFile()
            } catch (e: IOException) {
                // TODO Auto-generated catch block
                e.printStackTrace()
            }
        }
        try {
            //BufferedWriter for performance, true to set append to file flag
            val buf = BufferedWriter(FileWriter(logFile, true))
            buf.append(text)
            buf.newLine()
            buf.close()
        } catch (e: IOException) {
            // TODO Auto-generated catch block
            e.printStackTrace()
        }
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        appendLog("onStartCommand executed with startId: $startId")
        this.intent = intent
        if (intent != null) {
            val action = intent.action
            appendLog("using an intent with action $action")
            when (action) {
                Actions.START.name -> startService()
                Actions.STOP.name -> stopService()
                else -> appendLog("This should never happen. No action in the received intent")
            }
        } else {
            appendLog(
                "with a null intent. It has been probably restarted by the system."
            )
        }
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        appendLog("The service has been created".toUpperCase())

    }

    override fun onDestroy() {
        super.onDestroy()
        appendLog("The service has been destroyed".toUpperCase())
        Toast.makeText(this, "Service destroyed", Toast.LENGTH_SHORT).show()
    }

    override fun onTaskRemoved(rootIntent: Intent) {
        val restartServiceIntent =
            Intent(applicationContext, NotificationService::class.java).also {
                it.setPackage(packageName)
            };
        appendLog(
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss"
            ).format(Calendar.getInstance().time) + " Try restart service"
        )

        val restartServicePendingIntent: PendingIntent = PendingIntent.getService(
            this,
            1,
            restartServiceIntent,
            PendingIntent.FLAG_ONE_SHOT
        );
        applicationContext.getSystemService(Context.ALARM_SERVICE);
        val alarmService: AlarmManager =
            applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager;
        alarmService.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000,
            restartServicePendingIntent
        );
    }

    private fun startService() {
        val notification = createNotification()
        startForeground(1, notification)
        if (isServiceStarted) return
        appendLog("Starting the foreground service task")

        appendLog(
            SimpleDateFormat(
                "yyyy-MM-dd HH:mm:ss"
            ).format(Calendar.getInstance().time) + " Starting the foreground service task"
        )
        Toast.makeText(this, "Service starting its task", Toast.LENGTH_SHORT).show()
        isServiceStarted = true
        setServiceState(this, ServiceState.STARTED)

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock =
            (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
                newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationService::lock").apply {
                    acquire()
                }
            }

        // we're starting a loop in a coroutine
        appendLog("End of the loop for the service")

    }

    private fun stopService() {
        appendLog("Stopping the foreground service")
        Toast.makeText(this, "Service stopping", Toast.LENGTH_SHORT).show()
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            appendLog("Service stopped without being started: ${e.message}")
        }
        isServiceStarted = false
        setServiceState(this, ServiceState.STOPPED)
    }

    private fun createNotification(): Notification {
        val notificationChannelId = this.getString(R.string.app_name)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                notificationChannelId,
                this.getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Show ${getString(R.string.app_name)} level battery"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).let { notificationIntent ->
                PendingIntent.getActivity(this, 0, notificationIntent, 0)
            }

        val batteryLevel =
            (intent?.extras?.getInt("android.bluetooth.device.extra.BATTERY_LEVEL")
                ?.takeIf { it in 0..100 } ?: -1)

        return NotificationCompat.Builder(this, notificationChannelId)
            .setContentText(
                if (batteryLevel == -1) "Connection" else "Battery level: $batteryLevel%"
            )
            .setContentIntent(pendingIntent)
            .setSmallIcon(
                if (batteryLevel >= 0) this.resources.getIdentifier(
                    "battery_level_$batteryLevel", "drawable",
                    this.packageName
                ) else R.mipmap.ic_launcher
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
    }
}
