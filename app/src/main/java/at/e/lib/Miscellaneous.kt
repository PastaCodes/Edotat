package at.e.lib

fun <E> MutableList<E>.replaceOne(element: E, replacement: E) {
    this[this.indexOf(element)] = replacement
}
