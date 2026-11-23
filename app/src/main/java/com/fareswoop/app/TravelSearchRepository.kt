package com.fareswoop.app

import android.net.Uri
import kotlin.math.roundToInt
import kotlin.random.Random

object AffiliateLinkBuilder {
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
    val hasAutomaticTransmission: Boolean = false,
    val hasBreakfastIncluded: Boolean = false,
    val isDirectFlight: Boolean = false,
    val hasCabinBagIncluded: Boolean = false,
    val totalPriceGbp: Double,
    val unitPriceLabel: String,
    val bookingUrl: String
)

object TravelSearchRepository {

    val flightAirports = listOf(
        "Bristol Airport (BRS)",
        "Alicante Airport (ALC)",
        "London Gatwick (LGW)",
        "London Heathrow (LHR)",
        "Manchester Airport (MAN)",
        "Faro Airport (FAO)",
        "Malaga Airport (AGP)",
        "Edinburgh Airport (EDI)",
        "Birmingham Airport (BHX)",
        "Dublin Airport (DUB)"
    )

    val carHireLocations = listOf(
        "Alicante Airport (ALC)",
        "Alicante City Centre",
        "Bristol Airport (BRS)",
        "Bristol Harbourside",
        "Faro Airport (FAO)",
        "Faro Marina",
        "Malaga Airport (AGP)",
        "Malaga Coast",
        "Edinburgh Airport (EDI)",
        "Edinburgh Old Town",
        "Barcelona Airport (BCN)",
        "Barcelona Eixample",
        "Lisbon Airport (LIS)",
        "Lisbon Marques de Pombal",
        "Cardiff Airport (CWL)",
        "Cardiff Bay"
    )

    val hotelDestinations = listOf(
        "Alicante Old Town",
        "Alicante Beachfront",
        "Bristol Clifton",
        "Bristol City Centre",
        "Faro Harbour",
        "Malaga Promenade",
        "Edinburgh Royal Mile",
        "Bath City Centre",
        "Barcelona Gothic Quarter",
        "Lisbon Chiado"
    )

    private fun extractIataOrSlug(raw: String, fallbackSlug: String): String {
        val iataMatch = Regex("\\(([A-Z]{3})\\)").find(raw.uppercase())
        if (iataMatch != null) {
            return iataMatch.groupValues[1]
        }
        val cleaned = raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return cleaned.ifBlank { fallbackSlug }
    }

    fun searchCars(
        pickupLocation: String,
        pickupDateIso: String,
        returnDateIso: String,
        automaticOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val loc = pickupLocation.ifBlank { "Alicante Airport (ALC)" }
        val encodedLoc = Uri.encode(loc)
        val slugOrIata = extractIataOrSlug(loc, "alicante")

        val kayakUrl = "https://www.kayak.co.uk/cars/$slugOrIata/$pickupDateIso/$returnDateIso?sort=price_a"
        val skyscannerCarsUrl = "https://www.skyscanner.net/carhire/results/$slugOrIata/$slugOrIata/$pickupDateIso/$returnDateIso/30"
        val rentalcarsUrl = "https://www.rentalcars.com/SearchResults.do?place=$encodedLoc&puDay=${pickupDateIso.takeLast(2)}&doDay=${returnDateIso.takeLast(2)}"

        val offers = listOf(
            TravelOffer(
                id = "car_1",
                category = "CAR",
                primaryTitle = "Fiat 500 • $loc",
                badgeLabel = "Mini",
                providerName = "Kayak Car Hire",
                detailsSummary = "4 Seats • 1 Bag • Manual • Full-to-Full",
                freeCancellation = true,
                hasAutomaticTransmission = false,
                totalPriceGbp = 148.0,
                unitPriceLabel = "£21.14/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakUrl, "CAR")
            ),
            TravelOffer(
                id = "car_2",
                category = "CAR",
                primaryTitle = "VW Golf • $loc",
                badgeLabel = "Compact",
                providerName = "Skyscanner Cars",
                detailsSummary = "5 Seats • 2 Bags • Automatic • Full-to-Full",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 214.0,
                unitPriceLabel = "£30.57/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerCarsUrl, "CAR")
            ),
            TravelOffer(
                id = "car_3",
                category = "CAR",
                primaryTitle = "Toyota Yaris Hybrid • $loc",
                badgeLabel = "Economy Auto",
                providerName = "Rentalcars",
                detailsSummary = "4 Seats • 2 Bags • Automatic • Low Emissions",
                freeCancellation = false,
                hasAutomaticTransmission = true,
                totalPriceGbp = 179.0,
                unitPriceLabel = "£25.57/day • Non-refundable saver",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(rentalcarsUrl, "CAR")
            ),
            TravelOffer(
                id = "car_4",
                category = "CAR",
                primaryTitle = "Nissan Qashqai • $loc",
                badgeLabel = "Intermediate SUV",
                providerName = "Kayak Car Hire",
                detailsSummary = "5 Seats • 3 Bags • Automatic • Unlimited Miles",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 268.0,
                unitPriceLabel = "£38.28/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakUrl, "CAR")
            ),
            TravelOffer(
                id = "car_5",
                category = "CAR",
                primaryTitle = "Ford Focus Estate • $loc",
                badgeLabel = "Compact Wagon",
                providerName = "Skyscanner Cars",
                detailsSummary = "5 Seats • 4 Bags • Manual • Free Amendments",
                freeCancellation = true,
                hasAutomaticTransmission = false,
                totalPriceGbp = 229.0,
                unitPriceLabel = "£32.71/day • Taxes included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerCarsUrl, "CAR")
            )
        )

        return offers.filter { offer ->
            val okAuto = !automaticOnly || offer.hasAutomaticTransmission
            val okCancel = !freeCancelOnly || offer.freeCancellation
            okAuto && okCancel
        }
    }

    fun searchHotels(
        destination: String,
        checkInIso: String,
        checkOutIso: String,
        breakfastIncludedOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val city = destination.ifBlank { "Alicante Old Town" }
        val encodedCity = Uri.encode(city)

        val bookingComUrl = "https://www.booking.com/searchresults.en-gb.html?ss=$encodedCity&checkin=$checkInIso&checkout=$checkOutIso&group_adults=2&no_rooms=1"
        val hotelsComUrl = "https://uk.hotels.com/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=2"
        val expediaUrl = "https://www.expedia.co.uk/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=2"

        val offers = listOf(
            TravelOffer(
                id = "hotel_1",
                category = "HOTEL",
                primaryTitle = "Harbourview Boutique Hotel • $city",
                badgeLabel = "4-Star Hotel",
                providerName = "Booking.com",
                detailsSummary = "Double Room • Breakfast Included • Central",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 742.0,
                unitPriceLabel = "£106.00/night • Live dates applied",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(bookingComUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_2",
                category = "HOTEL",
                primaryTitle = "Central Market Suites • $city",
                badgeLabel = "4-Star Hotel",
                providerName = "Hotels.com",
                detailsSummary = "Superior Twin • Room Only • Free Wi-Fi",
                freeCancellation = true,
                hasBreakfastIncluded = false,
                totalPriceGbp = 588.0,
                unitPriceLabel = "£84.00/night • Live dates applied",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(hotelsComUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_3",
                category = "HOTEL",
                primaryTitle = "Grand Promenade Resort • $city",
                badgeLabel = "5-Star Hotel",
                providerName = "Expedia UK",
                detailsSummary = "Deluxe Balcony • Breakfast Included • Spa Access",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 966.0,
                unitPriceLabel = "£138.00/night • Live dates applied",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(expediaUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_4",
                category = "HOTEL",
                primaryTitle = "Old Quarter Inn • $city",
                badgeLabel = "3-Star Hotel",
                providerName = "Booking.com",
                detailsSummary = "Standard Double • Room Only • Budget Pick",
                freeCancellation = false,
                hasBreakfastIncluded = false,
                totalPriceGbp = 413.0,
                unitPriceLabel = "£59.00/night • Live dates applied",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(bookingComUrl, "HOTEL")
            )
        )

        return offers.filter { offer ->
            val okBreakfast = !breakfastIncludedOnly || offer.hasBreakfastIncluded
            val okCancel = !freeCancelOnly || offer.freeCancellation
            okBreakfast && okCancel
        }
    }

    fun searchFlights(
        origin: String,
        destination: String,
        outboundDateIso: String,
        inboundDateIso: String,
        directOnly: Boolean,
        cabinBagOnly: Boolean
    ): List<TravelOffer> {
        val from = origin.ifBlank { "Bristol Airport (BRS)" }
        val to = destination.ifBlank { "Alicante Airport (ALC)" }
        val fromCode = extractIataOrSlug(from, "brs").lowercase()
        val toCode = extractIataOrSlug(to, "alc").lowercase()
        val skyDateOut = outboundDateIso.replace("-", "").takeLast(6)
        val skyDateIn = inboundDateIso.replace("-", "").takeLast(6)

        val skyscannerFlightUrl = "https://www.skyscanner.net/transport/flights/$fromCode/$toCode/$skyDateOut/$skyDateIn/?adultsv2=1&cabinclass=economy&rtn=1"
        val kayakFlightUrl = "https://www.kayak.co.uk/flights/${fromCode.uppercase()}-${toCode.uppercase()}/$outboundDateIso/$inboundDateIso?sort=bestflight_a"

        val offers = listOf(
            TravelOffer(
                id = "flight_1",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • 2h 35m",
                providerName = "Skyscanner Flights",
                detailsSummary = "Direct • Cabin Bag Included • Morning Outbound",
                freeCancellation = false,
                isDirectFlight = true,
                hasCabinBagIncluded = true,
                totalPriceGbp = 164.0,
                unitPriceLabel = "Return per person • Dates pre-filled",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerFlightUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_2",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • 2h 40m",
                providerName = "Kayak Flights",
                detailsSummary = "Direct • 10kg Cabin Bag Included • Flexible Change",
                freeCancellation = true,
                isDirectFlight = true,
                hasCabinBagIncluded = true,
                totalPriceGbp = 219.0,
                unitPriceLabel = "Return per person • 10kg bag included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakFlightUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_3",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • 2h 30m",
                providerName = "Skyscanner Flights",
                detailsSummary = "Direct • Small Underseat Bag Only • Low Fare",
                freeCancellation = false,
                isDirectFlight = true,
                hasCabinBagIncluded = false,
                totalPriceGbp = 112.0,
                unitPriceLabel = "Return per person • Underseat bag only",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerFlightUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_4",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "1 Stop • 4h 50m",
                providerName = "Kayak Flights",
                detailsSummary = "1 Connection • Cabin Bag Included • Flexible",
                freeCancellation = true,
                isDirectFlight = false,
                hasCabinBagIncluded = true,
                totalPriceGbp = 254.0,
                unitPriceLabel = "Return per person • Cabin bag included",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakFlightUrl, "FLIGHT")
            )
        )

        return offers.filter { offer ->
            val okDirect = !directOnly || offer.isDirectFlight
            val okBag = !cabinBagOnly || offer.hasCabinBagIncluded
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
