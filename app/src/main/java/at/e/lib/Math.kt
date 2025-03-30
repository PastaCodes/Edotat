package at.e.lib

fun Boolean.toInt() = if (this) 1 else 0

operator fun Int.times(boolean: Boolean) = this * boolean.toInt()

operator fun Boolean.times(int: Int) = this.toInt() * int
