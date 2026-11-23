package com.fareswoop.app

import android.net.Uri
import kotlin.math.roundToInt
import kotlin.random.Random

object AffiliateLinkBuilder {
    // Set to true and add your live partner marker once approved by an affiliate network
    private const val ENABLE_AFFILIATE_REDIRECT = false
    private const val PARTNER_MARKER = "YOUR_LIVE_MARKER"

    fun buildBookingUrl(directUrl: String, category: String): String {
        val cleanUrl = directUrl.trim()
        if (!ENABLE_AFFILIATE_REDIRECT || PARTNER_MARKER == "YOUR_LIVE_MARKER") {
            return cleanUrl
        }
        return Uri.parse("https://www.travelpayouts.com/")
            .buildUpon()
            .appendQueryParameter("marker", PARTNER_MARKER)
            .appendQueryParameter("sub_id", "fareswoop_${category.lowercase()}")
            .appendQueryParameter("redirect_to", cleanUrl)
            .build()
            .toString()
    }
}

data class TravelOffer(
    val id: String,
    val category: String,
    val primaryTitle: String,
    val badgeLabel: String,
    val providerName: String,
    val detailsSummary: String,
    val freeCancellation: Boolean,
    val totalPriceGbp: Double,
    val unitPriceLabel: String,
    val bookingUrl: String
)

object TravelSearchRepository {

    fun searchCars(
        pickupLocation: String,
        automaticOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val loc = pickupLocation.ifBlank { "Alicante Airport (ALC)" }
        val encoded = Uri.encode(loc)

        val offers = listOf(
            TravelOffer(
                id = "car_1",
                category = "CAR",
                primaryTitle = "Fiat 500 or similar ($loc)",
                badgeLabel = "Mini",
                providerName = "Kayak Car Hire",
                detailsSummary = "4 Seats • 1 Bag • Manual • Full-to-Full",
                freeCancellation = true,
                totalPriceGbp = 148.0,
                unitPriceLabel = "£21.14/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.kayak.co.uk/cars/$encoded",
                    "CAR"
                )
            ),
            TravelOffer(
                id = "car_2",
                category = "CAR",
                primaryTitle = "VW Golf or similar ($loc)",
                badgeLabel = "Compact",
                providerName = "Rentalcars",
                detailsSummary = "5 Seats • 2 Bags • Automatic • Full-to-Full",
                freeCancellation = true,
                totalPriceGbp = 214.0,
                unitPriceLabel = "£30.57/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.rentalcars.com/SearchResults.do?place=$encoded",
                    "CAR"
                )
            ),
            TravelOffer(
                id = "car_3",
                category = "CAR",
                primaryTitle = "Nissan Qashqai or similar ($loc)",
                badgeLabel = "Intermediate SUV",
                providerName = "Sixt UK",
                detailsSummary = "5 Seats • 3 Bags • Automatic • Unlimited Miles",
                freeCancellation = true,
                totalPriceGbp = 268.0,
                unitPriceLabel = "£38.28/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.sixt.co.uk/",
                    "CAR"
                )
            )
        )

        return offers.filter { offer ->
            val okAuto = !automaticOnly || offer.detailsSummary.contains("Automatic", ignoreCase = true)
            val okCancel = !freeCancelOnly || offer.freeCancellation
            okAuto && okCancel
        }
    }

    fun searchHotels(
        destination: String,
        breakfastIncludedOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val city = destination.ifBlank { "Alicante City Centre" }
        val encoded = Uri.encode(city)

        val offers = listOf(
            TravelOffer(
                id = "hotel_1",
                category = "HOTEL",
                primaryTitle = "Melia Alicante Marina • $city",
                badgeLabel = "4-Star Hotel",
                providerName = "Booking.com",
                detailsSummary = "Double Sea View • Breakfast Included • Near Beach",
                freeCancellation = true,
                totalPriceGbp = 742.0,
                unitPriceLabel = "£106.00/night • 7 nights",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.booking.com/searchresults.en-gb.html?ss=$encoded",
                    "HOTEL"
                )
            ),
            TravelOffer(
                id = "hotel_2",
                category = "HOTEL",
                primaryTitle = "Eurostars Lucentum • $city",
                badgeLabel = "4-Star Hotel",
                providerName = "Hotels.com",
                detailsSummary = "Superior Twin • Room Only • Central Location",
                freeCancellation = true,
                totalPriceGbp = 588.0,
                unitPriceLabel = "£84.00/night • 7 nights",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://uk.hotels.com/Hotel-Search?destination=$encoded",
                    "HOTEL"
                )
            )
        )

        return offers.filter { offer ->
            val okBreakfast = !breakfastIncludedOnly || offer.detailsSummary.contains("Breakfast", ignoreCase = true)
            val okCancel = !freeCancelOnly || offer.freeCancellation
            okBreakfast && okCancel
        }
    }

    fun searchFlights(
        origin: String,
        destination: String,
        directOnly: Boolean,
        cabinBagOnly: Boolean
    ): List<TravelOffer> {
        val from = origin.ifBlank { "Bristol (BRS)" }
        val to = destination.ifBlank { "Alicante (ALC)" }
        val encodedQuery = Uri.encode("$from to$to")

        val offers = listOf(
            TravelOffer(
                id = "flight_1",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • 2h 35m",
                providerName = "Skyscanner",
                detailsSummary = "Direct • Cabin Bag Included • Morning Outbound",
                freeCancellation = false,
                totalPriceGbp = 164.0,
                unitPriceLabel = "Return per person • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.skyscanner.net/transport/flights/?q=$encodedQuery",
                    "FLIGHT"
                )
            ),
            TravelOffer(
                id = "flight_2",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • 2h 40m",
                providerName = "Jet2",
                detailsSummary = "Direct • 10kg Cabin Bag Included • Flexible Change",
                freeCancellation = true,
                totalPriceGbp = 219.0,
                unitPriceLabel = "Return per person • 10kg bag included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(
                    "https://www.jet2.com/",
                    "FLIGHT"
                )
            )
        )

        return offers.filter { offer ->
            val okDirect = !directOnly || offer.badgeLabel.contains("Direct", ignoreCase = true)
            val okBag = !cabinBagOnly || offer.detailsSummary.contains("Cabin Bag", ignoreCase = true)
            okDirect && okBag
        }
    }

    fun fetchLiveQuote(booking: TrackedBooking): Double {
        val maxDrop = when (booking.category) {
            "CAR" -> -0.22
            "HOTEL" -> -0.18
            else -> -0.25
        }
        val fluctuation = Random.nextDouble(maxDrop, 0.08)
        val raw = (booking.latestPriceGbp * (1.0 + fluctuation)).coerceAtLeast(35.0)
        return (raw * 100).roundToInt() / 100.0
    }
}
