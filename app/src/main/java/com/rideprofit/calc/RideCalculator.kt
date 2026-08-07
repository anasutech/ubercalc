package com.rideprofit.calc

import kotlin.math.roundToInt

/**
 * نتيجة الحساب الجاهزة لعرض صافي الربح والتقييم بالفقاعة.
 */
data class RideResult(
    val fare: Double,
    val miles: Double,
    val minutes: Double,
    val cost: Double,
    val net: Double,
    val hourlyRate: Double
)

/**
 * إعدادات السائق (تُقرأ من SharedPreferences).
 */
data class DriverSettings(
    val gasPricePerGallon: Double = 3.90,
    val mpg: Double = 17.5,
    val wearCostPerMile: Double = 0.12,
    val targetHourlyRate: Double = 20.0
)

object RideCalculator {

    /**
     * يحسب تكلفة الميل الواحد = بنزين + صيانة/تآكل.
     */
    private fun costPerMile(settings: DriverSettings): Double {
        val gasCostPerMile = settings.gasPricePerGallon / settings.mpg
        return gasCostPerMile + settings.wearCostPerMile
    }

    /**
     * حساب تفاصيل الرحلة (التكلفة، الصافي، ومعدل الساعة لتحديد اللون).
     */
    fun calculate(fare: Double, miles: Double, minutes: Double, settings: DriverSettings): RideResult {
        val cpm = costPerMile(settings)
        val cost = miles * cpm
        val net = fare - cost
        val hours = if (minutes <= 0) 0.01 else minutes / 60.0
        val hourlyRate = net / hours
        return RideResult(fare, miles, minutes, cost, net, hourlyRate)
    }

    /**
     * يرجع مستوى التقييم لتحديد لون الفقاعة (أخضر، أصفر، أحمر) بناءً على الهدف:
     * أحمر: أقل من 60% من الهدف | أصفر: بين 60% و 100% | أخضر: فوق الهدف
     */
    fun verdictLevel(result: RideResult, settings: DriverSettings): Verdict {
        val target = settings.targetHourlyRate
        return when {
            result.hourlyRate < target * 0.6 -> Verdict.RED
            result.hourlyRate < target -> Verdict.AMBER
            else -> Verdict.GREEN
        }
    }

    enum class Verdict { RED, AMBER, GREEN }
}

/**
 * يستخرج الأجرة والمسافة والوقت من شاشة أوبر بدقة.
 */
object FareParser {

    private val fareRegex = Regex("""\$\s?([\d,]+(?:\.\d{1,2})?)""")
    // النمط الشامل لقراءة أي سطر يحتوي على دقائق وم الأميال (سواء بالصيغة العادية أو المتعددة)
    private val segmentRegex = Regex("""(\d+(?:\.\d+)?)\s*(?:min|mins)\b[^)]*?([\d,]+(?:\.\d+)?)\s*mi\b""", RegexOption.IGNORE_CASE)

    fun parse(allText: List<String>): ParsedRide? {
        val joined = allText.joinToString(" | ")

        // استخراج الأجرة (مثال: $14.09 أو $8.11)
        val fareMatch = fareRegex.find(joined) ?: return null
        val fare = fareMatch.groupValues[1].replace(",", "").toDoubleOrNull() ?: return null

        var totalMinutes = 0.0
        var totalMiles = 0.0

        // البحث عن كل مقاطع الوقت والمسافة في الشاشة وجمعها
        val matches = segmentRegex.findAll(joined)
        for (match in matches) {
            val mins = match.groupValues[1].toDoubleOrNull() ?: 0.0
            val miles = match.groupValues[2].replace(",", "").toDoubleOrNull() ?: 0.0
            totalMinutes += mins
            totalMiles += miles
        }

        // طريقة احتياطية دقيقة جداً إذا كان النص مقسماً بطريقة أخرى
        if (totalMiles <= 0.0 || totalMinutes <= 0.0) {
            val allMiles = Regex("""([\d,]+(?:\.\d+)?)\s*mi\b""", RegexOption.IGNORE_CASE)
                .findAll(joined).mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }.toList()
            
            val allMins = Regex("""(\d+(?:\.\d+)?)\s*(?:min|mins)\b""", RegexOption.IGNORE_CASE)
                .findAll(joined).mapNotNull { it.groupValues[1].toDoubleOrNull() }.toList()

            totalMiles = allMiles.sum()
            totalMinutes = allMins.sum()
        }

        if (totalMiles <= 0.0 || totalMinutes <= 0.0) return null

        return ParsedRide(fare, totalMiles, totalMinutes)
    }
}

data class ParsedRide(val fare: Double, val miles: Double, val minutes: Double)

fun Double.round1(): Double = (this * 10.0).roundToInt() / 10.0
