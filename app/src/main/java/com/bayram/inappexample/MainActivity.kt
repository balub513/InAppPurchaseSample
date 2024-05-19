package com.bayram.inappexample

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private var btn_nonconsumeable: Button? = null
    private var btn_consumeable: Button? = null
    private var btn_subscription: Button? = null
    private var intent: Intent? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    private fun init() {
        btn_nonconsumeable = this.findViewById(R.id.btn_nonconsumeable)
        btn_consumeable = this.findViewById(R.id.btn_consumeable)
        btn_subscription = this.findViewById(R.id.btn_subscription)

        (btn_nonconsumeable as Button).setOnClickListener(View.OnClickListener {
            intent = Intent(this@MainActivity, NonConsumable::class.java)
            startActivity(intent)
        })

        (btn_consumeable as Button).setOnClickListener(View.OnClickListener {
            intent = Intent(this@MainActivity, Consumable::class.java)
            startActivity(intent)
        })

        (btn_subscription as Button).setOnClickListener(View.OnClickListener {
            intent = Intent(this@MainActivity, Subscription::class.java)
            startActivity(intent)
        })
    }
}