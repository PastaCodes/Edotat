package at.e.api

data class Address(
    val streetAddress: String,  // example: street name and building number
    val locality: String,       // city / town / village
    val division: String,       // state / province / region
    val country: String,        // full country name
) {
    override fun toString(): String {
        return "$streetAddress, $locality, $division, $country"
    }
}
