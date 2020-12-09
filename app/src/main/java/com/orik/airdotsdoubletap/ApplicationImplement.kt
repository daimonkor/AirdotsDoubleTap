package com.orik.airdotsdoubletap

import android.app.Application
import android.os.Build
import android.util.Log
import com.icebergteam.timberjava.LineNumberDebugTree
import com.icebergteam.timberjava.Timber

class ApplicationImplement : Application() {
    override fun onCreate() {
        super.onCreate()
        initLogger()
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
}