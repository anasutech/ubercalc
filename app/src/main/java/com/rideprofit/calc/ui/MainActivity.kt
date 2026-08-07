package com.rideprofit.calc.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.rideprofit.calc.DriverSettings
import com.rideprofit.calc.Prefs
import com.rideprofit.calc.R

class MainActivity : AppCompatActivity() {

    private lateinit var etGas: EditText
    private lateinit var etMpg: EditText
    private lateinit var etWear: EditText
    private lateinit var etTarget: EditText
    private lateinit var tvStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etGas = findViewById(R.id.etGas)
        etMpg = findViewById(R.id.etMpg)
        etWear = findViewById(R.id.etWear)
        etTarget = findViewById(R.id.etTarget)
        tvStatus = findViewById(R.id.tvStatus)

        loadSettingsIntoFields()

        findViewById<Button>(R.id.btnAccessibility).setOnClickListener {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }

        findViewById<Button>(R.id.btnOverlay).setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
            }
        }

        findViewById<Button>(R.id.btnSave).setOnClickListener {
            saveFieldsToSettings()
        }
    }

    private fun loadSettingsIntoFields() {
        val s = Prefs.load(this)
        etGas.setText(s.gasPricePerGallon.toString())
        etMpg.setText(s.mpg.toString())
        etWear.setText(s.wearCostPerMile.toString())
        etTarget.setText(s.targetHourlyRate.toString())
    }

    private fun saveFieldsToSettings() {
        val gas = etGas.text.toString().toDoubleOrNull() ?: 3.90
        val mpg = etMpg.text.toString().toDoubleOrNull() ?: 17.5
        val wear = etWear.text.toString().toDoubleOrNull() ?: 0.12
        val target = etTarget.text.toString().toDoubleOrNull() ?: 20.0

        Prefs.save(this, DriverSettings(gas, mpg, wear, target))
        tvStatus.text = "تم الحفظ ✓"
    }
}
