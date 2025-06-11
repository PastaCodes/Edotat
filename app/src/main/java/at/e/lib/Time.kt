package at.e.lib

import android.content.Context
import android.text.format.DateFormat
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.FormatStringsInDatetimeFormats
import kotlinx.datetime.format.byUnicodePattern

fun minuteOfDay(hour24: Int, minute: Int) = hour24 * 60 + minute

fun splitMinuteOfDay(minuteOfDay: Int) = minuteOfDay / 60 to minuteOfDay % 60

fun formatLocalMinuteOfDay(minuteOfDay: Int, is24Hour: Boolean): String {
    var res = ""
    val (hour24, minute) = splitMinuteOfDay(minuteOfDay)
    res += if (is24Hour) hour24 else hour24 % 12
    res += ":"
    res += if (minute >= 10) minute else "0$minute"
    if (!is24Hour) {
        res += if (hour24 < 12) " AM" else " PM"
    }
    return res
}

fun formatLocalMinuteOfDayRange(startMinute: Int, endMinute: Int, is24Hour: Boolean) =
    formatLocalMinuteOfDay(startMinute, is24Hour) + " – " + formatLocalMinuteOfDay(endMinute, is24Hour)

fun LocalTime.toMinuteOfDay() = minuteOfDay(this.hour, this.minute)

fun LocalTime.inMinuteOfDayRange(startMinute: Int, endMinute: Int): Boolean {
    return this.toMinuteOfDay() in startMinute..endMinute
}

@OptIn(FormatStringsInDatetimeFormats::class)
fun formatLocalDate(date: LocalDate, context: Context): String {
    val pattern = DateFormat.getBestDateTimePattern(context.resources.configuration.locales[0], "ddMMyyyy")
    return date.format(LocalDate.Format { byUnicodePattern(pattern) })
}
