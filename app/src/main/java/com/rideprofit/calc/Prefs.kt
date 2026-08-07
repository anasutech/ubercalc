package com.rideprofit.calc

import android.content.Context

object Prefs {
    private const val NAME = "ride_profit_prefs"

    private fun sp(context: Context) =
        context.getSharedPreferences(NAME, Context.MODE_PRIVATE)

    fun save(context: Context, settings: DriverSettings) {
        sp(context).edit()
            .putFloat("gas", settings.gasPricePerGallon.toFloat())
            .putFloat("mpg", settings.mpg.toFloat())
            .putFloat("wear", settings.wearCostPerMile.toFloat())
            .putFloat("target", settings.targetHourlyRate.toFloat())
            .apply()
    }

    fun load(context: Context): DriverSettings {
        val p = sp(context)
        return DriverSettings(
            gasPricePerGallon = p.getFloat("gas", 3.90f).toDouble(),
            mpg = p.getFloat("mpg", 17.5f).toDouble(),
            wearCostPerMile = p.getFloat("wear", 0.12f).toDouble(),
            targetHourlyRate = p.getFloat("target", 20.0f).toDouble()
        )
    }
}