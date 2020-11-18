package com.orik.airdotsdoubletap

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        findViewById<TextView>(R.id.textView).text = (getString(R.string.info, getString(R.string.app_name)))
    }
}