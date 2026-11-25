package com.fareswoop.app

import android.net.Uri
import kotlin.math.abs
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
        // UK & Ireland
        "Aberdeen Airport (ABZ)",
        "Belfast City Airport (BHD)",
        "Belfast International Airport (BFS)",
        "Birmingham Airport (BHX)",
        "Bournemouth Airport (BOH)",
        "Bristol Airport (BRS)",
        "Cardiff Airport (CWL)",
        "Cork Airport (ORK)",
        "Dublin Airport (DUB)",
        "Dundee Airport (DND)",
        "East Midlands Airport (EMA)",
        "Edinburgh Airport (EDI)",
        "Exeter Airport (EXT)",
        "Glasgow Airport (GLA)",
        "Glasgow Prestwick Airport (PIK)",
        "Humberside Airport (HUY)",
        "Inverness Airport (INV)",
        "Isle of Man Airport (IOM)",
        "Jersey Airport (JER)",
        "Leeds Bradford Airport (LBA)",
        "Liverpool John Lennon Airport (LPL)",
        "London City Airport (LCY)",
        "London Gatwick (LGW)",
        "London Heathrow (LHR)",
        "London Luton (LTN)",
        "London Southend Airport (SEN)",
        "London Stansted (STN)",
        "Manchester Airport (MAN)",
        "Newcastle Airport (NCL)",
        "Newquay Cornwall Airport (NQY)",
        "Norwich Airport (NWI)",
        "Shannon Airport (SNN)",
        "Southampton Airport (SOU)",
        "Teesside International Airport (MME)",
        // Europe
        "Alicante Airport (ALC)",
        "Almeria Airport (LEI)",
        "Amsterdam Schiphol (AMS)",
        "Athens Airport (ATH)",
        "Barcelona El Prat (BCN)",
        "Basel Mulhouse Freiburg Airport (BSL)",
        "Bergen Airport Flesland (BGO)",
        "Berlin Brandenburg Airport (BER)",
        "Bilbao Airport (BIO)",
        "Bologna Guglielmo Marconi Airport (BLQ)",
        "Bordeaux Merignac Airport (BOD)",
        "Brussels Airport (BRU)",
        "Budapest Ferenc Liszt Airport (BUD)",
        "Copenhagen Airport (CPH)",
        "Corfu Airport (CFU)",
        "Dubrovnik Airport (DBV)",
        "Dusseldorf Airport (DUS)",
        "Faro Airport (FAO)",
        "Frankfurt Airport (FRA)",
        "Fuerteventura Airport (FUE)",
        "Geneva Airport (GVA)",
        "Gran Canaria Airport (LPA)",
        "Hamburg Airport (HAM)",
        "Heraklion Airport (HER)",
        "Ibiza Airport (IBZ)",
        "Istanbul Airport (IST)",
        "Krakow John Paul II Airport (KRK)",
        "Lanzarote Airport (ACE)",
        "Larnaca International Airport (LCA)",
        "Lisbon Airport (LIS)",
        "Lyon Saint-Exupery Airport (LYS)",
        "Madrid Barajas (MAD)",
        "Malaga Airport (AGP)",
        "Malta International Airport (MLA)",
        "Menorca Airport (MAH)",
        "Milan Bergamo Airport (BGY)",
        "Milan Malpensa (MXP)",
        "Munich Airport (MUC)",
        "Naples International Airport (NAP)",
        "Nice Cote d'Azur Airport (NCE)",
        "Oslo Gardermoen Airport (OSL)",
        "Palermo Airport (PMO)",
        "Palma de Mallorca Airport (PMI)",
        "Paphos International Airport (PFO)",
        "Paris Beauvais Airport (BVA)",
        "Paris Charles de Gaulle (CDG)",
        "Paris Orly Airport (ORY)",
        "Porto Airport (OPO)",
        "Prague Vaclav Havel Airport (PRG)",
        "Reykjavik Keflavik Airport (KEF)",
        "Rhodes Diagoras Airport (RHO)",
        "Rome Ciampino Airport (CIA)",
        "Rome Fiumicino (FCO)",
        "Salzburg Airport (SZG)",
        "Seville Airport (SVQ)",
        "Sofia Airport (SOF)",
        "Split Airport (SPU)",
        "Stockholm Arlanda Airport (ARN)",
        "Stuttgart Airport (STR)",
        "Tenerife North Airport (TFN)",
        "Tenerife South Airport (TFS)",
        "Thessaloniki Airport (SKG)",
        "Valencia Airport (VLC)",
        "Venice Marco Polo Airport (VCE)",
        "Vienna International Airport (VIE)",
        "Warsaw Chopin Airport (WAW)",
        "Zakynthos Airport (ZTH)",
        "Zurich Airport (ZRH)",
        // North America, Caribbean, Middle East, Africa & Asia-Pacific
        "Atlanta Hartsfield-Jackson (ATL)",
        "Austin-Bergstrom Airport (AUS)",
        "Boston Logan (BOS)",
        "Calgary International Airport (YYC)",
        "Cancun International Airport (CUN)",
        "Chicago O'Hare Airport (ORD)",
        "Dallas/Fort Worth Airport (DFW)",
        "Denver International Airport (DEN)",
        "Fort Lauderdale Airport (FLL)",
        "Honolulu Daniel K. Inouye Airport (HNL)",
        "Houston George Bush Airport (IAH)",
        "Las Vegas Harry Reid Airport (LAS)",
        "Los Angeles International Airport (LAX)",
        "Miami International Airport (MIA)",
        "Montreal Trudeau Airport (YUL)",
        "Montego Bay Airport (MBJ)",
        "New York JFK (JFK)",
        "Newark Liberty Airport (EWR)",
        "Orlando International (MCO)",
        "Philadelphia International Airport (PHL)",
        "Phoenix Sky Harbor Airport (PHX)",
        "Punta Cana International Airport (PUJ)",
        "San Diego International Airport (SAN)",
        "San Francisco International Airport (SFO)",
        "Seattle-Tacoma Airport (SEA)",
        "Tampa International Airport (TPA)",
        "Toronto Pearson Airport (YYZ)",
        "Vancouver International Airport (YVR)",
        "Washington Dulles Airport (IAD)",
        "Abu Dhabi Zayed International (AUH)",
        "Auckland Airport (AKL)",
        "Bangkok Suvarnabhumi Airport (BKK)",
        "Beijing Capital Airport (PEK)",
        "Cairo International Airport (CAI)",
        "Cape Town International Airport (CPT)",
        "Delhi Indira Gandhi Airport (DEL)",
        "Doha Hamad International (DOH)",
        "Dubai International (DXB)",
        "Hong Kong International Airport (HKG)",
        "Johannesburg O.R. Tambo Airport (JNB)",
        "Kuala Lumpur International Airport (KUL)",
        "Marrakech Menara Airport (RAK)",
        "Melbourne Airport (MEL)",
        "Mumbai Chhatrapati Shivaji Airport (BOM)",
        "Nairobi Jomo Kenyatta Airport (NBO)",
        "Osaka Kansai Airport (KIX)",
        "Perth Airport (PER)",
        "Phuket International Airport (HKT)",
        "Riyadh King Khalid Airport (RUH)",
        "Seoul Incheon Airport (ICN)",
        "Shanghai Pudong Airport (PVG)",
        "Singapore Changi Airport (SIN)",
        "Sydney Kingsford Smith Airport (SYD)",
        "Tokyo Haneda Airport (HND)",
        "Tokyo Narita Airport (NRT)"
    )

    val carHireLocations = listOf(
        "Alicante Airport (ALC)",
        "Alicante City Centre",
        "Alicante Train Station",
        "Benidorm Levante",
        "Torrevieja Marina",
        "Bristol Airport (BRS)",
        "Bristol Harbourside",
        "Bristol Temple Meads",
        "Plymouth City Centre",
        "Saltash",
        "Exeter Airport (EXT)",
        "Exeter City Centre",
        "Truro City Centre",
        "Newquay Cornwall Airport (NQY)",
        "Faro Airport (FAO)",
        "Faro Marina",
        "Albufeira Strip",
        "Malaga Airport (AGP)",
        "Malaga Coast",
        "Marbella Old Town",
        "Edinburgh Airport (EDI)",
        "Edinburgh Old Town",
        "Glasgow Airport (GLA)",
        "Glasgow City Centre",
        "Manchester Airport (MAN)",
        "Manchester City Centre",
        "London Heathrow (LHR)",
        "London Gatwick (LGW)",
        "Birmingham Airport (BHX)",
        "Barcelona Airport (BCN)",
        "Barcelona Eixample",
        "Madrid Airport (MAD)",
        "Lisbon Airport (LIS)",
        "Cardiff Airport (CWL)",
        "Cardiff Bay",
        "Orlando International (MCO)",
        "Los Angeles International Airport (LAX)"
    )

    val hotelDestinations = listOf(
        "Saltash",
        "Plymouth",
        "Plymouth Hoe",
        "Looe",
        "Tavistock",
        "Exeter",
        "Torquay",
        "St Ives",
        "Newquay",
        "Falmouth",
        "Bath",
        "Bristol",
        "Bristol Harbourside",
        "Cardiff",
        "London",
        "London South Bank",
        "Manchester",
        "Edinburgh",
        "York",
        "Alicante",
        "Benidorm",
        "Faro",
        "Albufeira",
        "Malaga",
        "Marbella",
        "Barcelona",
        "Madrid",
        "Lisbon",
        "Porto",
        "Paris",
        "Rome",
        "New York",
        "Dubai"
    )

    private fun extractIataCode(raw: String): String? {
        return Regex("\\(([A-Z]{3})\\)").find(raw.uppercase())?.groupValues?.get(1)
    }

    private fun extractSlug(raw: String, fallback: String): String {
        val iata = extractIataCode(raw)
        if (iata != null) return iata
        val slug = raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return slug.ifBlank { fallback }
    }

    private fun locationMultiplier(location: String): Double {
        val hash = abs(location.lowercase().trim().hashCode() % 25)
        return 0.88 + (hash / 100.0)
    }

    fun searchCars(
        pickupLocation: String,
        pickupDateIso: String,
        pickupTime: String,
        returnDateIso: String,
        returnTime: String,
        rentalDays: Int,
        automaticOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val loc = pickupLocation.ifBlank { "Alicante Airport (ALC)" }
        val encodedLoc = Uri.encode(loc)
        val slug = extractSlug(loc, "alicante")
        val days = rentalDays.coerceAtLeast(1)
        val mult = locationMultiplier(loc)
        val puHour = pickupTime.replace(":", "")
        val doHour = returnTime.replace(":", "")

        val kayakUrl = "https://www.kayak.co.uk/cars/$slug/$pickupDateIso-${puHour}h/$returnDateIso-${doHour}h?sort=price_a&pickup=$encodedLoc"
        val skyscannerCarsUrl = "https://www.skyscanner.net/carhire/results/$slug/$slug/${pickupDateIso}T$pickupTime/${returnDateIso}T$returnTime/30"
        val rentalcarsUrl = "https://www.rentalcars.com/SearchResults.do?place=$encodedLoc&puYear=${pickupDateIso.substring(0, 4)}&puMonth=${pickupDateIso.substring(5, 7)}&puDay=${pickupDateIso.substring(8, 10)}&puHour=${pickupTime.substring(0, 2)}&puMinute=${pickupTime.substring(3, 5)}&doYear=${returnDateIso.substring(0, 4)}&doMonth=${returnDateIso.substring(5, 7)}&doDay=${returnDateIso.substring(8, 10)}&doHour=${returnTime.substring(0, 2)}&doMinute=${returnTime.substring(3, 5)}"
        val expediaCarsUrl = "https://www.expedia.co.uk/carsearch?locn=$encodedLoc&date1=$pickupDateIso&time1=$puHour&date2=$returnDateIso&time2=$doHour"

        data class CarTemplate(
            val id: String,
            val title: String,
            val badge: String,
            val provider: String,
            val details: String,
            val freeCancel: Boolean,
            val auto: Boolean,
            val baseDailyGbp: Double,
            val url: String
        )

        val templates = listOf(
            CarTemplate(
                "car_1",
                "Fiat 500 or similar • $loc",
                "Mini • $days${if (days == 1) "day" else "days"}",
                "Kayak Car Hire",
                "4 Seats • 1 Bag • Manual • Pick-up $pickupTime / Return$returnTime",
                true,
                false,
                22.0,
                kayakUrl
            ),
            CarTemplate(
                "car_2",
                "VW Golf or similar • $loc",
                "Compact • $days${if (days == 1) "day" else "days"}",
                "Skyscanner Car Hire",
                "5 Seats • 2 Bags • Automatic • Pick-up $pickupTime / Return$returnTime",
                true,
                true,
                31.0,
                skyscannerCarsUrl
            ),
            CarTemplate(
                "car_3",
                "Toyota Yaris Hybrid • $loc",
                "Economy Auto • $days${if (days == 1) "day" else "days"}",
                "Rentalcars.com",
                "4 Seats • 2 Bags • Automatic • Pick-up $pickupTime / Return$returnTime",
                false,
                true,
                26.0,
                rentalcarsUrl
            ),
            CarTemplate(
                "car_4",
                "Nissan Qashqai • $loc",
                "Intermediate SUV • $days${if (days == 1) "day" else "days"}",
                "Expedia Car Hire",
                "5 Seats • 3 Bags • Automatic • Pick-up $pickupTime / Return$returnTime",
                true,
                true,
                39.0,
                expediaCarsUrl
            ),
            CarTemplate(
                "car_5",
                "Ford Focus Estate • $loc",
                "Estate • $days${if (days == 1) "day" else "days"}",
                "Kayak Car Hire",
                "5 Seats • 4 Bags • Manual • Pick-up $pickupTime / Return$returnTime",
                true,
                false,
                33.0,
                kayakUrl
            )
        )

        return templates.filter { t ->
            (!automaticOnly || t.auto) && (!freeCancelOnly || t.freeCancel)
        }.map { t ->
            val daily = ((t.baseDailyGbp * mult) * 100).roundToInt() / 100.0
            val total = ((daily * days) * 100).roundToInt() / 100.0
            TravelOffer(
                id = t.id,
                category = "CAR",
                primaryTitle = t.title,
                badgeLabel = t.badge,
                providerName = t.provider,
                detailsSummary = t.details,
                freeCancellation = t.freeCancel,
                hasAutomaticTransmission = t.auto,
                totalPriceGbp = total,
                unitPriceLabel = String.format("Est. £%.2f/day • Pre-filled dates & times", daily),
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(t.url, "CAR")
            )
        }
    }

    fun searchHotels(
        destination: String,
        checkInIso: String,
        checkOutIso: String,
        nightsCount: Int,
        adults: Int,
        children: Int,
        rooms: Int,
        breakfastIncludedOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val city = destination.ifBlank { "Saltash" }
        val encodedCity = Uri.encode(city)
        val nights = nightsCount.coerceAtLeast(1)
        val mult = locationMultiplier(city)
        val partyLabel = "${buildPartySummary(adults, children)} • $rooms${if (rooms == 1) "Room" else "Rooms"} • $nights${if (nights == 1) "Night" else "Nights"}"

        val bookingUrl = "https://www.booking.com/searchresults.en-gb.html?ss=$encodedCity&checkin=$checkInIso&checkout=$checkOutIso&group_adults=$adults&group_children=$children&no_rooms=$rooms&selected_currency=GBP"
        val hotelsComUrl = "https://uk.hotels.com/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=$adults&rooms=$rooms"
        val expediaUrl = "https://www.expedia.co.uk/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=$adults&rooms=$rooms"
        val agodaUrl = "https://www.agoda.com/search?textToSearch=$encodedCity&checkIn=$checkInIso&checkOut=$checkOutIso&adults=$adults&children=$children&rooms=$rooms&currencyCode=GBP"
        val kayakHotelsUrl = "https://www.kayak.co.uk/hotels/$encodedCity/$checkInIso/$checkOutIso/${adults}adults"

        data class HotelTemplate(
            val id: String,
            val title: String,
            val stars: String,
            val provider: String,
            val details: String,
            val freeCancel: Boolean,
            val breakfast: Boolean,
            val baseNightlyGbp: Double,
            val url: String
        )

        val templates = listOf(
            HotelTemplate(
                "hotel_1",
                "$city Central & Waterfront Stay",
                "4-Star",
                "Booking.com",
                "Breakfast Included • Exact dates & guests pre-filled",
                true,
                true,
                104.0,
                bookingUrl
            ),
            HotelTemplate(
                "hotel_2",
                "$city Boutique Townhouse & Suites",
                "4-Star",
                "Hotels.com",
                "Room Only • Free Wi-Fi • Exact dates & guests pre-filled",
                true,
                false,
                86.0,
                hotelsComUrl
            ),
            HotelTemplate(
                "hotel_3",
                "$city Grand Spa & Country Hotel",
                "5-Star",
                "Expedia UK",
                "Breakfast Included • Leisure Club • Exact dates & guests pre-filled",
                true,
                true,
                136.0,
                expediaUrl
            ),
            HotelTemplate(
                "hotel_4",
                "$city Inn & Lodge",
                "3-Star",
                "Agoda",
                "Room Only • Value Rate • Exact dates & guests pre-filled",
                false,
                false,
                64.0,
                agodaUrl
            ),
            HotelTemplate(
                "hotel_5",
                "$city Harbour & Station Hotel",
                "3-Star",
                "Kayak Hotels",
                "Breakfast Included • Flexible Rate • Exact dates & guests pre-filled",
                true,
                true,
                92.0,
                kayakHotelsUrl
            )
        )

        val roomMultiplier = rooms.coerceAtLeast(1) * (1.0 + (children * 0.12))

        return templates.filter { t ->
            (!breakfastIncludedOnly || t.breakfast) && (!freeCancelOnly || t.freeCancel)
        }.map { t ->
            val nightly = ((t.baseNightlyGbp * mult * roomMultiplier) * 100).roundToInt() / 100.0
            val total = ((nightly * nights) * 100).roundToInt() / 100.0
            TravelOffer(
                id = t.id,
                category = "HOTEL",
                primaryTitle = t.title,
                badgeLabel = "${t.stars} •$partyLabel",
                providerName = t.provider,
                detailsSummary = t.details,
                freeCancellation = t.freeCancel,
                hasBreakfastIncluded = t.breakfast,
                totalPriceGbp = total,
                unitPriceLabel = String.format("Est. £%.2f/night • Opens %s directly", nightly, t.provider),
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(t.url, "HOTEL")
            )
        }
    }

    fun searchFlights(
        origin: String,
        destination: String,
        outboundDateIso: String,
        inboundDateIso: String,
        adults: Int,
        children: Int,
        cabinClass: String,
        directOnly: Boolean,
        cabinBagOnly: Boolean
    ): List<TravelOffer> {
        val from = origin.ifBlank { "Bristol Airport (BRS)" }
        val to = destination.ifBlank { "Alicante Airport (ALC)" }
        val fromCode = extractSlug(from, "brs").lowercase()
        val toCode = extractSlug(to, "alc").lowercase()
        val skyDateOut = outboundDateIso.replace("-", "").takeLast(6)
        val skyDateIn = inboundDateIso.replace("-", "").takeLast(6)
        val partyLabel = buildPartySummary(adults, children)
        val totalSeats = (adults + children).coerceAtLeast(1)
        val cabinSlug = cabinClass.lowercase().replace(" ", "")
        val cabinMultiplier = when (cabinClass) {
            "Premium Economy" -> 1.65
            "Business" -> 2.85
            "First" -> 4.20
            else -> 1.0
        }
        val routeMult = locationMultiplier("$from-$to")

        val skyscannerUrl = "https://www.skyscanner.net/transport/flights/$fromCode/$toCode/$skyDateOut/$skyDateIn/?adultsv2=$adults&childrenv2=$children&cabinclass=$cabinSlug&rtn=1"
        val kayakUrl = "https://www.kayak.co.uk/flights/${fromCode.uppercase()}-${toCode.uppercase()}/$outboundDateIso/$inboundDateIso/${adults}adults/${children}children/$cabinSlug?sort=bestflight_a"
        val expediaFlightUrl = "https://www.expedia.co.uk/Flights-Search?trip=roundtrip&leg1=from:${fromCode.uppercase()},to:${toCode.uppercase()},departure:$outboundDateIso&leg2=from:${toCode.uppercase()},to:${fromCode.uppercase()},departure:$inboundDateIso&passengers=adults:$adults,children:$children&mode=search"
        val momondoUrl = "https://www.momondo.co.uk/flight-search/${fromCode.uppercase()}-${toCode.uppercase()}/$outboundDateIso/$inboundDateIso/${adults}adults?sort=bestflight_a"

        data class FlightTemplate(
            val id: String,
            val badge: String,
            val provider: String,
            val details: String,
            val freeCancel: Boolean,
            val direct: Boolean,
            val cabinBag: Boolean,
            val basePerPersonGbp: Double,
            val url: String
        )

        val templates = listOf(
            FlightTemplate(
                "flight_1",
                "Direct • $partyLabel • $cabinClass",
                "Skyscanner Flights",
                "Direct • Cabin Bag Included • Route & passengers pre-filled",
                false,
                true,
                true,
                158.0,
                skyscannerUrl
            ),
            FlightTemplate(
                "flight_2",
                "Direct • $partyLabel • $cabinClass",
                "Kayak Flights",
                "Direct • 10kg Cabin Bag Included • Route & passengers pre-filled",
                true,
                true,
                true,
                194.0,
                kayakUrl
            ),
            FlightTemplate(
                "flight_3",
                "Direct • $partyLabel • $cabinClass",
                "Momondo Flights",
                "Direct • Underseat Bag Only • Route & passengers pre-filled",
                false,
                true,
                false,
                114.0,
                momondoUrl
            ),
            FlightTemplate(
                "flight_4",
                "1 Stop • $partyLabel • $cabinClass",
                "Expedia Flights",
                "1 Connection • Cabin Bag Included • Route & passengers pre-filled",
                true,
                false,
                true,
                224.0,
                expediaFlightUrl
            )
        )

        return templates.filter { t ->
            (!directOnly || t.direct) && (!cabinBagOnly || t.cabinBag)
        }.map { t ->
            val perPerson = ((t.basePerPersonGbp * routeMult * cabinMultiplier) * 100).roundToInt() / 100.0
            val total = ((perPerson * totalSeats) * 100).roundToInt() / 100.0
            TravelOffer(
                id = t.id,
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = t.badge,
                providerName = t.provider,
                detailsSummary = t.details,
                freeCancellation = t.freeCancel,
                isDirectFlight = t.direct,
                hasCabinBagIncluded = t.cabinBag,
                totalPriceGbp = total,
                unitPriceLabel = String.format("Est. £%.2f/person • Opens %s directly", perPerson, t.provider),
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(t.url, "FLIGHT")
            )
        }
    }

    fun buildPartySummary(adults: Int, children: Int): String {
        val adultPart = "$adults${if (adults == 1) "Adult" else "Adults"}"
        return if (children > 0) {
            "$adultPart, $children${if (children == 1) "Child" else "Children"}"
        } else {
            adultPart
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
