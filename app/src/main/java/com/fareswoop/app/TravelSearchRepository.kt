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
        // North America & Caribbean
        "Atlanta Hartsfield-Jackson (ATL)",
        "Austin-Bergstrom Airport (AUS)",
        "Baltimore/Washington Airport (BWI)",
        "Boston Logan (BOS)",
        "Calgary International Airport (YYC)",
        "Cancun International Airport (CUN)",
        "Charlotte Douglas Airport (CLT)",
        "Chicago O'Hare Airport (ORD)",
        "Dallas/Fort Worth Airport (DFW)",
        "Denver International Airport (DEN)",
        "Detroit Metropolitan Airport (DTW)",
        "Fort Lauderdale Airport (FLL)",
        "Honolulu Daniel K. Inouye Airport (HNL)",
        "Houston George Bush Airport (IAH)",
        "Las Vegas Harry Reid Airport (LAS)",
        "Los Angeles International Airport (LAX)",
        "Miami International Airport (MIA)",
        "Minneapolis-St. Paul Airport (MSP)",
        "Montreal Trudeau Airport (YUL)",
        "Montego Bay Airport (MBJ)",
        "Nashville International Airport (BNA)",
        "New York JFK (JFK)",
        "Newark Liberty Airport (EWR)",
        "Orlando International (MCO)",
        "Philadelphia International Airport (PHL)",
        "Phoenix Sky Harbor Airport (PHX)",
        "Punta Cana International Airport (PUJ)",
        "Salt Lake City Airport (SLC)",
        "San Diego International Airport (SAN)",
        "San Francisco International Airport (SFO)",
        "Seattle-Tacoma Airport (SEA)",
        "Tampa International Airport (TPA)",
        "Toronto Pearson Airport (YYZ)",
        "Vancouver International Airport (YVR)",
        "Washington Dulles Airport (IAD)",
        // Middle East, Africa & Asia-Pacific
        "Abu Dhabi Zayed International (AUH)",
        "Auckland Airport (AKL)",
        "Bangkok Suvarnabhumi Airport (BKK)",
        "Beijing Capital Airport (PEK)",
        "Cairo International Airport (CAI)",
        "Cape Town International Airport (CPT)",
        "Chennai International Airport (MAA)",
        "Christchurch Airport (CHC)",
        "Colombo Bandaranaike Airport (CMB)",
        "Delhi Indira Gandhi Airport (DEL)",
        "Doha Hamad International (DOH)",
        "Dubai International (DXB)",
        "Ho Chi Minh City Tan Son Nhat (SGN)",
        "Hong Kong International Airport (HKG)",
        "Johannesburg O.R. Tambo Airport (JNB)",
        "Kuala Lumpur International Airport (KUL)",
        "Manila Ninoy Aquino Airport (MNL)",
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
        "Tel Aviv Ben Gurion Airport (TLV)",
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
        "Faro Airport (FAO)",
        "Faro Marina",
        "Albufeira Strip",
        "Lagos Old Town",
        "Malaga Airport (AGP)",
        "Malaga Coast",
        "Marbella Old Town",
        "Nerja Balcon de Europa",
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
        "Madrid Atocha",
        "Lisbon Airport (LIS)",
        "Lisbon Marques de Pombal",
        "Porto Airport (OPO)",
        "Porto Boavista",
        "Cardiff Airport (CWL)",
        "Cardiff Bay",
        "Plymouth City Centre",
        "Exeter Airport (EXT)",
        "Exeter City Centre",
        "Orlando International (MCO)",
        "Los Angeles International Airport (LAX)",
        "Miami International Airport (MIA)"
    )

    val hotelDestinations = listOf(
        "Alicante Old Town",
        "Alicante Beachfront",
        "Alicante Postiguet",
        "Benidorm Poniente",
        "Bristol Clifton",
        "Bristol City Centre",
        "Bristol Harbourside",
        "Faro Harbour",
        "Vilamoura Marina",
        "Albufeira Old Town",
        "Malaga Promenade",
        "Malaga Soho",
        "Marbella Golden Mile",
        "Edinburgh Royal Mile",
        "Edinburgh New Town",
        "Bath City Centre",
        "Barcelona Gothic Quarter",
        "Barcelona Gracia",
        "Madrid Salamanca",
        "Lisbon Chiado",
        "Lisbon Alfama",
        "Porto Ribeira",
        "London South Bank",
        "London Kensington",
        "Manchester Northern Quarter",
        "York Minster Quarter",
        "Plymouth Hoe",
        "New York Manhattan",
        "Dubai Marina",
        "Paris Le Marais",
        "Rome Centro Storico"
    )

    private fun extractCodeOrSlug(raw: String, fallback: String): String {
        val iataMatch = Regex("\\(([A-Z]{3})\\)").find(raw.uppercase())
        if (iataMatch != null) {
            return iataMatch.groupValues[1]
        }
        val slug = raw.trim().lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return slug.ifBlank { fallback }
    }

    fun searchCars(
        pickupLocation: String,
        pickupDateIso: String,
        returnDateIso: String,
        adults: Int,
        children: Int,
        automaticOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val loc = pickupLocation.ifBlank { "Alicante Airport (ALC)" }
        val encodedLoc = Uri.encode(loc)
        val slug = extractCodeOrSlug(loc, "alicante")
        val travellersCount = (adults + children).coerceAtLeast(1)

        val kayakSearchUrl = "https://www.kayak.co.uk/cars/$slug/$pickupDateIso/$returnDateIso?sort=price_a&pickup=$encodedLoc"
        val rentalcarsSearchUrl = "https://www.rentalcars.com/SearchResults.do?place=$encodedLoc&puDay=${pickupDateIso.takeLast(2)}&doDay=${returnDateIso.takeLast(2)}"
        val skyscannerCarsUrl = "https://www.skyscanner.net/carhire/results/$slug/$slug/$pickupDateIso/$returnDateIso/30"
        val sixtUrl = "https://www.sixt.co.uk/"
        val enterpriseUrl = "https://www.enterprise.co.uk/"
        val avisUrl = "https://www.avis.co.uk/"
        val hertzUrl = "https://www.hertz.co.uk/"
        val europcarUrl = "https://www.europcar.co.uk/"
        val budgetUrl = "https://www.budget.co.uk/"
        val dollarUrl = "https://www.dollar.co.uk/"
        val thriftyUrl = "https://www.thrifty.co.uk/"

        val offers = listOf(
            TravelOffer(
                id = "car_1",
                category = "CAR",
                primaryTitle = "Fiat 500 or similar • $loc",
                badgeLabel = "Mini • $travellersCount pax",
                providerName = "Kayak Car Hire & Rentalcars",
                detailsSummary = "4 Seats • 1 Bag • Manual • Full-to-Full",
                freeCancellation = true,
                hasAutomaticTransmission = false,
                totalPriceGbp = 148.0,
                unitPriceLabel = "£21.14/day • Compared across major car agencies",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakSearchUrl, "CAR")
            ),
            TravelOffer(
                id = "car_2",
                category = "CAR",
                primaryTitle = "VW Golf or similar • $loc",
                badgeLabel = "Compact • $travellersCount pax",
                providerName = "Skyscanner Cars & Sixt",
                detailsSummary = "5 Seats • 2 Bags • Automatic • Full-to-Full",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 214.0,
                unitPriceLabel = "£30.57/day • Compared across major car agencies",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerCarsUrl, "CAR")
            ),
            TravelOffer(
                id = "car_3",
                category = "CAR",
                primaryTitle = "Toyota Yaris Hybrid • $loc",
                badgeLabel = "Economy Auto • $travellersCount pax",
                providerName = "Rentalcars & Europcar",
                detailsSummary = "4 Seats • 2 Bags • Automatic • Low Emissions",
                freeCancellation = false,
                hasAutomaticTransmission = true,
                totalPriceGbp = 179.0,
                unitPriceLabel = "£25.57/day • Saver rate across suppliers",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(rentalcarsSearchUrl, "CAR")
            ),
            TravelOffer(
                id = "car_4",
                category = "CAR",
                primaryTitle = "Nissan Qashqai • $loc",
                badgeLabel = "Intermediate SUV • $travellersCount pax",
                providerName = "Enterprise, Avis & Hertz",
                detailsSummary = "5 Seats • 3 Bags • Automatic • Unlimited Miles",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 268.0,
                unitPriceLabel = "£38.28/day • Family SUV comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(enterpriseUrl, "CAR")
            ),
            TravelOffer(
                id = "car_5",
                category = "CAR",
                primaryTitle = "Ford Focus Estate • $loc",
                badgeLabel = "Compact Wagon • $travellersCount pax",
                providerName = "Avis, Budget & Thrifty",
                detailsSummary = "5 Seats • 4 Bags • Manual • Free Amendments",
                freeCancellation = true,
                hasAutomaticTransmission = false,
                totalPriceGbp = 229.0,
                unitPriceLabel = "£32.71/day • Extra luggage space",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(avisUrl, "CAR")
            ),
            TravelOffer(
                id = "car_6",
                category = "CAR",
                primaryTitle = "Hyundai Tucson • $loc",
                badgeLabel = "Standard SUV • $travellersCount pax",
                providerName = "Hertz, Sixt & Dollar",
                detailsSummary = "5 Seats • 3 Bags • Automatic • Full-to-Full",
                freeCancellation = false,
                hasAutomaticTransmission = true,
                totalPriceGbp = 245.0,
                unitPriceLabel = "£35.00/day • Prepaid multi-agency deal",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(hertzUrl, "CAR")
            ),
            TravelOffer(
                id = "car_7",
                category = "CAR",
                primaryTitle = "Renault Clio or similar • $loc",
                badgeLabel = "Economy • $travellersCount pax",
                providerName = "Europcar, Budget & Thrifty",
                detailsSummary = "5 Seats • 2 Bags • Manual • Full-to-Full",
                freeCancellation = true,
                hasAutomaticTransmission = false,
                totalPriceGbp = 162.0,
                unitPriceLabel = "£23.14/day • Value city & airport pick-up",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(europcarUrl, "CAR")
            ),
            TravelOffer(
                id = "car_8",
                category = "CAR",
                primaryTitle = "Peugeot 3008 Auto • $loc",
                badgeLabel = "Family Crossover • $travellersCount pax",
                providerName = "Sixt, Enterprise & Dollar",
                detailsSummary = "5 Seats • 3 Bags • Automatic • Free Cancellation",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 284.0,
                unitPriceLabel = "£40.57/day • Comfortable touring option",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(sixtUrl, "CAR")
            ),
            TravelOffer(
                id = "car_9",
                category = "CAR",
                primaryTitle = "Budget Compact Saver • $loc",
                badgeLabel = "Compact • $travellersCount pax",
                providerName = "Budget & Thrifty",
                detailsSummary = "5 Seats • 2 Bags • Manual • Standard Terms",
                freeCancellation = false,
                hasAutomaticTransmission = false,
                totalPriceGbp = 154.0,
                unitPriceLabel = "£22.00/day • Low-cost comparison rate",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(budgetUrl, "CAR")
            ),
            TravelOffer(
                id = "car_10",
                category = "CAR",
                primaryTitle = "Dollar & Thrifty Auto Deal • $loc",
                badgeLabel = "Midsize Auto • $travellersCount pax",
                providerName = "Dollar & Thrifty",
                detailsSummary = "5 Seats • 2 Bags • Automatic • Flexible Pickup",
                freeCancellation = true,
                hasAutomaticTransmission = true,
                totalPriceGbp = 205.0,
                unitPriceLabel = "£29.29/day • Automatic comparison rate",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl("$dollarUrl?ref=$thriftyUrl", "CAR")
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
        adults: Int,
        children: Int,
        rooms: Int,
        breakfastIncludedOnly: Boolean,
        freeCancelOnly: Boolean
    ): List<TravelOffer> {
        val city = destination.ifBlank { "Alicante Old Town" }
        val encodedCity = Uri.encode(city)
        val partyLabel = buildPartySummary(adults, children) + " • ${rooms} ${if (rooms == 1) "Room" else "Rooms"}"

        val bookingUrl = "https://www.booking.com/searchresults.en-gb.html?ss=$encodedCity&checkin=$checkInIso&checkout=$checkOutIso&group_adults=$adults&group_children=$children&no_rooms=$rooms"
        val hotelsComUrl = "https://uk.hotels.com/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=$adults&children=$children&rooms=$rooms"
        val expediaUrl = "https://www.expedia.co.uk/Hotel-Search?destination=$encodedCity&startDate=$checkInIso&endDate=$checkOutIso&adults=$adults&children=$children&rooms=$rooms"
        val agodaUrl = "https://www.agoda.com/search?city=$encodedCity&checkIn=$checkInIso&checkOut=$checkOutIso&adults=$adults&children=$children&rooms=$rooms"
        val kayakHotelsUrl = "https://www.kayak.co.uk/hotels/$encodedCity/$checkInIso/$checkOutIso/${adults}adults"
        val trivagoUrl = "https://www.trivago.co.uk/"
        val premierInnUrl = "https://www.premierinn.com/"
        val travelodgeUrl = "https://www.travelodge.co.uk/"
        val marriottUrl = "https://www.marriott.com/"
        val hiltonUrl = "https://www.hilton.com/"
        val ihgUrl = "https://www.ihg.com/"
        val accorUrl = "https://all.accor.com/"

        val offers = listOf(
            TravelOffer(
                id = "hotel_1",
                category = "HOTEL",
                primaryTitle = "Harbourview Boutique Hotel • $city",
                badgeLabel = "4-Star • $partyLabel",
                providerName = "Booking.com, Expedia & Trivago",
                detailsSummary = "Family Room • Breakfast Included • Central",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 742.0,
                unitPriceLabel = "£106.00/night • Multi-OTA comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(bookingUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_2",
                category = "HOTEL",
                primaryTitle = "Central Market Suites • $city",
                badgeLabel = "4-Star • $partyLabel",
                providerName = "Hotels.com & Agoda",
                detailsSummary = "Suite • Room Only • Free Wi-Fi",
                freeCancellation = true,
                hasBreakfastIncluded = false,
                totalPriceGbp = 588.0,
                unitPriceLabel = "£84.00/night • Multi-OTA comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(hotelsComUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_3",
                category = "HOTEL",
                primaryTitle = "Grand Promenade Resort • $city",
                badgeLabel = "5-Star • $partyLabel",
                providerName = "Expedia UK, Marriott & Hilton",
                detailsSummary = "Deluxe Room • Breakfast Included • Pool & Spa",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 966.0,
                unitPriceLabel = "£138.00/night • Resort & chain comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(expediaUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_4",
                category = "HOTEL",
                primaryTitle = "Old Quarter Inn • $city",
                badgeLabel = "3-Star • $partyLabel",
                providerName = "Agoda & Kayak Hotels",
                detailsSummary = "Standard Room • Room Only • Value Stay",
                freeCancellation = false,
                hasBreakfastIncluded = false,
                totalPriceGbp = 413.0,
                unitPriceLabel = "£59.00/night • Budget OTA rate",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(agodaUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_5",
                category = "HOTEL",
                primaryTitle = "Premier Inn & Travelodge Select • $city",
                badgeLabel = "3-Star • $partyLabel",
                providerName = "Premier Inn & Travelodge",
                detailsSummary = "Family Room • Breakfast Option • Flexible",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 504.0,
                unitPriceLabel = "£72.00/night • Popular chain comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(premierInnUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_6",
                category = "HOTEL",
                primaryTitle = "Travelodge & IHG Collection • $city",
                badgeLabel = "3-Star • $partyLabel",
                providerName = "Travelodge & IHG",
                detailsSummary = "Standard Room • Room Only • City Access",
                freeCancellation = false,
                hasBreakfastIncluded = false,
                totalPriceGbp = 448.0,
                unitPriceLabel = "£64.00/night • Chain & IHG rate",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(travelodgeUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_7",
                category = "HOTEL",
                primaryTitle = "Marriott & Hilton Honors Stay • $city",
                badgeLabel = "4-Star • $partyLabel",
                providerName = "Marriott & Hilton",
                detailsSummary = "King / Twin Room • Breakfast Included • Gym",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 882.0,
                unitPriceLabel = "£126.00/night • Global hotel brands",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(marriottUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_8",
                category = "HOTEL",
                primaryTitle = "IHG & Accor ALL Partner Stay • $city",
                badgeLabel = "4-Star • $partyLabel",
                providerName = "IHG, Accor & Trivago",
                detailsSummary = "Double Room • Room Only • Central",
                freeCancellation = true,
                hasBreakfastIncluded = false,
                totalPriceGbp = 651.0,
                unitPriceLabel = "£93.00/night • IHG, Accor & Trivago",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl("$ihgUrl?alt=$accorUrl&cmp=$trivagoUrl", "HOTEL")
            ),
            TravelOffer(
                id = "hotel_9",
                category = "HOTEL",
                primaryTitle = "Kayak & Trivago Meta Deal • $city",
                badgeLabel = "4-Star • $partyLabel",
                providerName = "Kayak & Trivago",
                detailsSummary = "Family Suite • Breakfast Included • Free Cancel",
                freeCancellation = true,
                hasBreakfastIncluded = true,
                totalPriceGbp = 798.0,
                unitPriceLabel = "£114.00/night • Meta-search comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakHotelsUrl, "HOTEL")
            ),
            TravelOffer(
                id = "hotel_10",
                category = "HOTEL",
                primaryTitle = "Accor City Saver • $city",
                badgeLabel = "3-Star • $partyLabel",
                providerName = "Accor & Booking.com",
                detailsSummary = "Standard Room • Room Only • Advance Saver",
                freeCancellation = false,
                hasBreakfastIncluded = false,
                totalPriceGbp = 469.0,
                unitPriceLabel = "£67.00/night • Advance purchase rate",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(accorUrl, "HOTEL")
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
        adults: Int,
        children: Int,
        cabinClass: String,
        directOnly: Boolean,
        cabinBagOnly: Boolean
    ): List<TravelOffer> {
        val from = origin.ifBlank { "Bristol Airport (BRS)" }
        val to = destination.ifBlank { "Alicante Airport (ALC)" }
        val fromCode = extractCodeOrSlug(from, "brs").lowercase()
        val toCode = extractCodeOrSlug(to, "alc").lowercase()
        val skyDateOut = outboundDateIso.replace("-", "").takeLast(6)
        val skyDateIn = inboundDateIso.replace("-", "").takeLast(6)
        val partyLabel = buildPartySummary(adults, children)
        val totalSeats = (adults + children).coerceAtLeast(1)
        val cabinSlug = cabinClass.lowercase().replace(" ", "")

        val skyscannerUrl = "https://www.skyscanner.net/transport/flights/$fromCode/$toCode/$skyDateOut/$skyDateIn/?adultsv2=$adults&childrenv2=$children&cabinclass=$cabinSlug&rtn=1"
        val kayakUrl = "https://www.kayak.co.uk/flights/${fromCode.uppercase()}-${toCode.uppercase()}/$outboundDateIso/$inboundDateIso/${adults}adults/${children}children/$cabinSlug"
        val googleFlightsUrl = "https://www.google.com/travel/flights"
        val momondoUrl = "https://www.momondo.co.uk/"
        val expediaFlightsUrl = "https://www.expedia.co.uk/Flights"
        val easyJetUrl = "https://www.easyjet.com/"
        val jet2Url = "https://www.jet2.com/"
        val ryanairUrl = "https://www.ryanair.com/"
        val baUrl = "https://www.britishairways.com/"
        val klmUrl = "https://www.klm.co.uk/"
        val lufthansaUrl = "https://www.lufthansa.com/"
        val emiratesUrl = "https://www.emirates.com/"

        val offers = listOf(
            TravelOffer(
                id = "flight_1",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • $partyLabel • $cabinClass",
                providerName = "Skyscanner, Google Flights & Momondo",
                detailsSummary = "Direct • Cabin Bag Included • Multi-airline fare",
                freeCancellation = false,
                isDirectFlight = true,
                hasCabinBagIncluded = true,
                totalPriceGbp = 164.0 * totalSeats,
                unitPriceLabel = "£164.00/person • Aggregator comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(skyscannerUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_2",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • $partyLabel • $cabinClass",
                providerName = "Jet2 & British Airways",
                detailsSummary = "Direct • 10kg Cabin Bag Included • Flexible Change",
                freeCancellation = true,
                isDirectFlight = true,
                hasCabinBagIncluded = true,
                totalPriceGbp = 219.0 * totalSeats,
                unitPriceLabel = "£219.00/person • Direct airline & flex",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(jet2Url, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_3",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • $partyLabel • $cabinClass",
                providerName = "easyJet & Ryanair",
                detailsSummary = "Direct • Small Bag Only • Low-cost fare",
                freeCancellation = false,
                isDirectFlight = true,
                hasCabinBagIncluded = false,
                totalPriceGbp = 112.0 * totalSeats,
                unitPriceLabel = "£112.00/person • Low-cost carrier fare",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(easyJetUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_4",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "1 Stop • $partyLabel • $cabinClass",
                providerName = "Kayak, Expedia & Momondo",
                detailsSummary = "1 Connection • Cabin Bag Included • Flexible",
                freeCancellation = true,
                isDirectFlight = false,
                hasCabinBagIncluded = true,
                totalPriceGbp = 254.0 * totalSeats,
                unitPriceLabel = "£254.00/person • OTA & connecting fare",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(kayakUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_5",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • $partyLabel • $cabinClass",
                providerName = "British Airways & Emirates",
                detailsSummary = "Direct • Cabin Bag Included • Full Service",
                freeCancellation = true,
                isDirectFlight = true,
                hasCabinBagIncluded = true,
                totalPriceGbp = 242.0 * totalSeats,
                unitPriceLabel = "£242.00/person • Full-service carriers",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(baUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_6",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "1 Stop • $partyLabel • $cabinClass",
                providerName = "KLM & Lufthansa",
                detailsSummary = "1 Connection via Hub • Cabin Bag Included",
                freeCancellation = true,
                isDirectFlight = false,
                hasCabinBagIncluded = true,
                totalPriceGbp = 228.0 * totalSeats,
                unitPriceLabel = "£228.00/person • European hub carriers",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(klmUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_7",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "Direct • $partyLabel • $cabinClass",
                providerName = "Ryanair Saver",
                detailsSummary = "Direct • Small Bag Only • Lowest base fare",
                freeCancellation = false,
                isDirectFlight = true,
                hasCabinBagIncluded = false,
                totalPriceGbp = 94.0 * totalSeats,
                unitPriceLabel = "£94.00/person • Basic bag fare",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl(ryanairUrl, "FLIGHT")
            ),
            TravelOffer(
                id = "flight_8",
                category = "FLIGHT",
                primaryTitle = "$from → $to (Return)",
                badgeLabel = "1 Stop • $partyLabel • $cabinClass",
                providerName = "Lufthansa, Emirates & Google Flights",
                detailsSummary = "1 Connection • Cabin Bag Included • Global network",
                freeCancellation = false,
                isDirectFlight = false,
                hasCabinBagIncluded = true,
                totalPriceGbp = 236.0 * totalSeats,
                unitPriceLabel = "£236.00/person • Global carrier comparison",
                bookingUrl = AffiliateLinkBuilder.buildBookingUrl("$lufthansaUrl?ref=$emiratesUrl&gf=$googleFlightsUrl&mm=$momondoUrl&ex=$expediaFlightsUrl", "FLIGHT")
            )
        )

        return offers.filter { offer ->
            val okDirect = !directOnly || offer.isDirectFlight
            val okBag = !cabinBagOnly || offer.hasCabinBagIncluded
            okDirect && okBag
        }
    }

    fun buildPartySummary(adults: Int, children: Int): String {
        val adultPart = "$adults ${if (adults == 1) "Adult" else "Adults"}"
        return if (children > 0) {
            "$adultPart, $children ${if (children == 1) "Child" else "Children"}"
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
