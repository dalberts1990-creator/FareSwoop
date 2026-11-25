package com.fareswoop.app

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.fareswoop.app.databinding.ActivityMainBinding
import com.fareswoop.app.databinding.BottomSheetArmWatcherBinding
import com.fareswoop.app.databinding.ItemCarOfferBinding
import com.fareswoop.app.databinding.ItemWatchlistBinding
import com.google.android.gms.ads.AdRequest
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class MainActivity : AppCompatActivity() {

    private enum class ScreenMode { HOME, SEARCH, WATCHLIST }

    private lateinit var binding: ActivityMainBinding
    private lateinit var offerAdapter: TravelOfferAdapter
    private lateinit var watchlistAdapter: WatchlistAdapter
    private var pendingOfferAfterBrowser: TravelOffer? = null
    private var selectedCategory = "CAR"
    private var currentMode = ScreenMode.HOME

    private var adultsCount = 2
    private var childrenCount = 0
    private var roomsCount = 1
    private var cabinIndex = 0
    private val cabinClasses = listOf("Economy", "Premium Economy", "Business", "First")

    private val mainHandler = Handler(Looper.getMainLooper())

    private val collectCalendar: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 14)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
    }

    private val returnCalendar: Calendar = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, 21)
        set(Calendar.HOUR_OF_DAY, 10)
        set(Calendar.MINUTE, 0)
    }

    private val dateTimeFormatter = SimpleDateFormat("dd MMM yyyy • HH:mm", Locale.UK)
    private val dateOnlyFormatter = SimpleDateFormat("dd MMM yyyy", Locale.UK)
    private val compactDateFormatter = SimpleDateFormat("dd MMM", Locale.UK)
    private val isoDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.UK)
    private val timeOnlyFormatter = SimpleDateFormat("HH:mm", Locale.UK)

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    private val swipeDetector by lazy {
        GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                if (dx > 140f && abs(dx) > abs(dy) * 1.4f && abs(velocityX) > 250f) {
                    if (currentMode != ScreenMode.HOME) {
                        switchScreenMode(ScreenMode.HOME)
                        return true
                    }
                }
                return false
            }
        })
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppPreferences.hasAcceptedTerms(this)) {
            startActivity(Intent(this, TermsActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applySafeInsets()
        ensureNotificationPermission()

        val defaultHours = if (AppPreferences.isPro(this)) {
            AppPreferences.PRO_FAST_INTERVAL_HOURS
        } else {
            AppPreferences.FREE_CHECK_INTERVAL_HOURS
        }
        PriceCheckWorker.schedulePeriodicChecks(this, defaultHours)

        refreshProAndAdsState()
        setupDateTimePickers()
        setupPartyAndTicketControls()
        setupNavigationAndHomepage()
        setupFilterListeners()
        setupLists()
        setupSearchControls()
        applyCategoryState("CAR", runSearch = true)
        switchScreenMode(ScreenMode.HOME)
        observeWatchlist()

        binding.scrollMainRoot.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }
        binding.layoutScrollContent.setOnTouchListener { _, event ->
            swipeDetector.onTouchEvent(event)
            false
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        swipeDetector.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun updateTabHighlights() {
        val activePurple = Color.parseColor("#6750A4")
        val whiteText = Color.WHITE
        val inactiveBg = Color.parseColor("#F8FAFC")
        val inactiveText = Color.parseColor("#334155")
        val borderColor = Color.parseColor("#CBD5E1")

        val tabs = listOf(
            ScreenMode.HOME to binding.btnModeHome,
            ScreenMode.SEARCH to binding.btnModeSearch,
            ScreenMode.WATCHLIST to binding.btnModeWatchlist
        )

        for ((mode, btn) in tabs) {
            if (currentMode == mode) {
                btn.backgroundTintList = ColorStateList.valueOf(activePurple)
                btn.setTextColor(whiteText)
                btn.strokeWidth = 0
            } else {
                btn.backgroundTintList = ColorStateList.valueOf(inactiveBg)
                btn.setTextColor(inactiveText)
                btn.strokeColor = ColorStateList.valueOf(borderColor)
                btn.strokeWidth = (1 * resources.displayMetrics.density).toInt().coerceAtLeast(1)
            }
        }
    }

    private fun switchScreenMode(mode: ScreenMode) {
        currentMode = mode
        binding.layoutHomeSection.visibility =
            if (mode == ScreenMode.HOME) View.VISIBLE else View.GONE
        binding.layoutSearchSection.visibility =
            if (mode == ScreenMode.SEARCH) View.VISIBLE else View.GONE
        binding.layoutWatchlistSection.visibility =
            if (mode == ScreenMode.WATCHLIST) View.VISIBLE else View.GONE
        updateTabHighlights()
        binding.scrollMainRoot.smoothScrollTo(0, 0)
    }

    private fun applySafeInsets() {
        val basePad = (16 * resources.displayMetrics.density).toInt()
        ViewCompat.setOnApplyWindowInsetsListener(binding.layoutMainContainer) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout()
            )
            view.setPadding(
                basePad + bars.left,
                basePad + bars.top,
                basePad + bars.right,
                basePad + bars.bottom
            )
            insets
        }
    }

    override fun onResume() {
        super.onResume()
        val offer = pendingOfferAfterBrowser
        if (offer != null) {
            pendingOfferAfterBrowser = null
            binding.cardPostBookingBanner.visibility = View.VISIBLE
            binding.tvPostBookingPrompt.text = String.format(
                Locale.UK,
                "Did you book %s? Enter your exact checkout price to track drops.",
                offer.primaryTitle
            )
            binding.btnConfirmBooked.setOnClickListener {
                binding.cardPostBookingBanner.visibility = View.GONE
                showArmWatcherSheet(offer)
            }
            binding.btnDismissBooked.setOnClickListener {
                binding.cardPostBookingBanner.visibility = View.GONE
            }
        }
    }

    private fun setupPartyAndTicketControls() {
        updatePartyLabels()

        binding.btnAdultsMinus.setOnClickListener {
            if (adultsCount > 1) {
                adultsCount--
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnAdultsPlus.setOnClickListener {
            if (adultsCount < 9) {
                adultsCount++
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnChildrenMinus.setOnClickListener {
            if (childrenCount > 0) {
                childrenCount--
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnChildrenPlus.setOnClickListener {
            if (childrenCount < 8) {
                childrenCount++
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnRoomsMinus.setOnClickListener {
            if (roomsCount > 1) {
                roomsCount--
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnRoomsPlus.setOnClickListener {
            if (roomsCount < 6) {
                roomsCount++
                updatePartyLabels()
                runActiveSearch()
            }
        }
        binding.btnCabinClass.setOnClickListener {
            cabinIndex = (cabinIndex + 1) % cabinClasses.size
            updatePartyLabels()
            runActiveSearch()
        }
    }

    private fun updatePartyLabels() {
        binding.tvAdultsCount.text = "Adults: $adultsCount"
        binding.tvChildrenCount.text = "Children: $childrenCount"
        binding.tvRoomsCount.text = "Rooms: $roomsCount"
        binding.btnCabinClass.text = "Ticket Class: ${cabinClasses[cabinIndex]} (Tap to change)"
    }

    private fun setupDateTimePickers() {
        updateDateTimeButtons()

        binding.btnPickCollectionDateTime.setOnClickListener {
            if (selectedCategory == "HOTEL") {
                pickDateOnly(collectCalendar) {
                    if (!returnCalendar.after(collectCalendar)) {
                        returnCalendar.timeInMillis =
                            collectCalendar.timeInMillis + TimeUnit.DAYS.toMillis(1)
                    }
                    updateDateTimeButtons()
                    runActiveSearch()
                }
            } else {
                pickDateAndTime(collectCalendar) {
                    if (!returnCalendar.after(collectCalendar)) {
                        returnCalendar.timeInMillis =
                            collectCalendar.timeInMillis + TimeUnit.DAYS.toMillis(1)
                    }
                    updateDateTimeButtons()
                    runActiveSearch()
                }
            }
        }

        binding.btnPickReturnDateTime.setOnClickListener {
            if (selectedCategory == "HOTEL") {
                pickDateOnly(returnCalendar) {
                    if (!returnCalendar.after(collectCalendar)) {
                        returnCalendar.timeInMillis =
                            collectCalendar.timeInMillis + TimeUnit.DAYS.toMillis(1)
                    }
                    updateDateTimeButtons()
                    runActiveSearch()
                }
            } else {
                pickDateAndTime(returnCalendar) {
                    if (!returnCalendar.after(collectCalendar)) {
                        returnCalendar.timeInMillis =
                            collectCalendar.timeInMillis + TimeUnit.DAYS.toMillis(1)
                    }
                    updateDateTimeButtons()
                    runActiveSearch()
                }
            }
        }
    }

    private fun pickDateOnly(target: Calendar, onDone: () -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.set(Calendar.YEAR, year)
                target.set(Calendar.MONTH, month)
                target.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                onDone()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun pickDateAndTime(target: Calendar, onDone: () -> Unit) {
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.set(Calendar.YEAR, year)
                target.set(Calendar.MONTH, month)
                target.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                TimePickerDialog(
                    this,
                    { _, hourOfDay, minute ->
                        target.set(Calendar.HOUR_OF_DAY, hourOfDay)
                        target.set(Calendar.MINUTE, minute)
                        onDone()
                    },
                    target.get(Calendar.HOUR_OF_DAY),
                    target.get(Calendar.MINUTE),
                    true
                ).show()
            },
            target.get(Calendar.YEAR),
            target.get(Calendar.MONTH),
            target.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun updateDateTimeButtons() {
        when (selectedCategory) {
            "HOTEL" -> {
                binding.btnPickCollectionDateTime.text =
                    "Check-in: ${dateOnlyFormatter.format(collectCalendar.time)}"
                binding.btnPickReturnDateTime.text =
                    "Check-out: ${dateOnlyFormatter.format(returnCalendar.time)}"
            }
            "FLIGHT" -> {
                binding.btnPickCollectionDateTime.text =
                    "Outbound: ${dateTimeFormatter.format(collectCalendar.time)}"
                binding.btnPickReturnDateTime.text =
                    "Inbound: ${dateTimeFormatter.format(returnCalendar.time)}"
            }
            else -> {
                binding.btnPickCollectionDateTime.text =
                    "Collect: ${dateTimeFormatter.format(collectCalendar.time)}"
                binding.btnPickReturnDateTime.text =
                    "Return: ${dateTimeFormatter.format(returnCalendar.time)}"
            }
        }
    }

    private fun getSelectedDaysOrNights(): Int {
        val diffMillis = returnCalendar.timeInMillis - collectCalendar.timeInMillis
        return TimeUnit.MILLISECONDS.toDays(diffMillis).toInt().coerceAtLeast(1)
    }

    private fun getFormattedDateRange(): String {
        return "${compactDateFormatter.format(collectCalendar.time)} → ${compactDateFormatter.format(returnCalendar.time)}"
    }

    private fun setSuggestionsForCategory(category: String) {
        when (category) {
            "FLIGHT" -> {
                binding.etFlightOrigin.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TravelSearchRepository.flightAirports)
                )
                binding.etPrimaryLocation.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TravelSearchRepository.flightAirports)
                )
            }
            "HOTEL" -> {
                binding.etPrimaryLocation.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TravelSearchRepository.hotelDestinations)
                )
            }
            else -> {
                binding.etPrimaryLocation.setAdapter(
                    ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, TravelSearchRepository.carHireLocations)
                )
            }
        }
    }

    private fun applyCategoryState(category: String, runSearch: Boolean) {
        selectedCategory = category
        binding.chipCatCar.isChecked = category == "CAR"
        binding.chipCatHotel.isChecked = category == "HOTEL"
        binding.chipCatFlight.isChecked = category == "FLIGHT"

        setSuggestionsForCategory(category)
        updateDateTimeButtons()

        when (category) {
            "HOTEL" -> {
                binding.etFlightOrigin.visibility = View.GONE
                binding.etPrimaryLocation.hint = "Type hotel town or city (e.g. Saltash)"
                binding.layoutPartySelector.visibility = View.VISIBLE
                binding.layoutRoomsSelector.visibility = View.VISIBLE
                binding.btnCabinClass.visibility = View.GONE
                binding.chipsCarFilters.visibility = View.GONE
                binding.chipsHotelFilters.visibility = View.VISIBLE
                binding.chipsFlightFilters.visibility = View.GONE
                binding.btnSearchOffers.text = "Compare Hotel Rates"
            }
            "FLIGHT" -> {
                binding.etFlightOrigin.visibility = View.VISIBLE
                binding.etPrimaryLocation.hint = "Type arrival airport (e.g. Alicante)"
                binding.layoutPartySelector.visibility = View.VISIBLE
                binding.layoutRoomsSelector.visibility = View.GONE
                binding.btnCabinClass.visibility = View.VISIBLE
                binding.chipsCarFilters.visibility = View.GONE
                binding.chipsHotelFilters.visibility = View.GONE
                binding.chipsFlightFilters.visibility = View.VISIBLE
                binding.btnSearchOffers.text = "Compare Flight Fares"
            }
            else -> {
                binding.etFlightOrigin.visibility = View.GONE
                binding.etPrimaryLocation.hint = "Type car hire airport or town (e.g. Saltash)"
                binding.layoutPartySelector.visibility = View.GONE
                binding.layoutRoomsSelector.visibility = View.GONE
                binding.btnCabinClass.visibility = View.GONE
                binding.chipsCarFilters.visibility = View.VISIBLE
                binding.chipsHotelFilters.visibility = View.GONE
                binding.chipsFlightFilters.visibility = View.GONE
                binding.btnSearchOffers.text = "Compare Car Hire Prices"
            }
        }

        if (runSearch) {
            runActiveSearch()
        }
    }

    private fun setupNavigationAndHomepage() {
        binding.btnModeHome.setOnClickListener { switchScreenMode(ScreenMode.HOME) }
        binding.btnModeSearch.setOnClickListener { switchScreenMode(ScreenMode.SEARCH) }
        binding.btnModeWatchlist.setOnClickListener { switchScreenMode(ScreenMode.WATCHLIST) }

        binding.btnHomeStartCars.setOnClickListener {
            applyCategoryState("CAR", runSearch = true)
            switchScreenMode(ScreenMode.SEARCH)
        }
        binding.btnHomeStartHotels.setOnClickListener {
            applyCategoryState("HOTEL", runSearch = true)
            switchScreenMode(ScreenMode.SEARCH)
        }
        binding.btnHomeStartFlights.setOnClickListener {
            applyCategoryState("FLIGHT", runSearch = true)
            switchScreenMode(ScreenMode.SEARCH)
        }
        binding.btnHomeOpenWatchlist.setOnClickListener {
            switchScreenMode(ScreenMode.WATCHLIST)
        }

        binding.chipCatCar.setOnClickListener { applyCategoryState("CAR", runSearch = true) }
        binding.chipCatHotel.setOnClickListener { applyCategoryState("HOTEL", runSearch = true) }
        binding.chipCatFlight.setOnClickListener { applyCategoryState("FLIGHT", runSearch = true) }

        binding.btnProStatus.setOnClickListener {
            showUpgradeProDialog()
        }
        binding.tvViewTermsFooter.setOnClickListener {
            val intent = Intent(this, TermsActivity::class.java).apply {
                putExtra(TermsActivity.EXTRA_READ_ONLY, true)
            }
            startActivity(intent)
        }
    }

    private fun setupFilterListeners() {
        binding.chipCarFreeCancel.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
        binding.chipCarAutoOnly.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
        binding.chipHotelFreeCancel.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
        binding.chipHotelBreakfast.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
        binding.chipFlightDirect.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
        binding.chipFlightCabinBag.setOnCheckedChangeListener { _, _ -> runActiveSearch() }
    }

    private fun clearSearchToLandingState() {
        binding.etFlightOrigin.setText("")
        binding.etPrimaryLocation.setText("")
        adultsCount = 2
        childrenCount = 0
        roomsCount = 1
        cabinIndex = 0
        updatePartyLabels()

        binding.chipCarFreeCancel.isChecked = false
        binding.chipCarAutoOnly.isChecked = false
        binding.chipHotelFreeCancel.isChecked = false
        binding.chipHotelBreakfast.isChecked = false
        binding.chipFlightDirect.isChecked = false
        binding.chipFlightCabinBag.isChecked = false

        collectCalendar.timeInMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(14)
        collectCalendar.set(Calendar.HOUR_OF_DAY, 10)
        collectCalendar.set(Calendar.MINUTE, 0)

        returnCalendar.timeInMillis = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(21)
        returnCalendar.set(Calendar.HOUR_OF_DAY, 10)
        returnCalendar.set(Calendar.MINUTE, 0)

        applyCategoryState("CAR", runSearch = true)
        switchScreenMode(ScreenMode.HOME)
        Toast.makeText(this, "Search cleared — back on FareSwoop Home", Toast.LENGTH_SHORT).show()
    }

    private fun triggerCompareWithLogoLoading() {
        val placeLabel = binding.etPrimaryLocation.text.toString().trim().ifBlank { "your location" }
        binding.tvLoadingMessage.text = "Searching $placeLabel across major providers..."
        binding.layoutLoadingOverlay.visibility = View.VISIBLE
        mainHandler.postDelayed({
            runActiveSearch()
            binding.layoutLoadingOverlay.visibility = View.GONE
            Toast.makeText(
                this,
                "Updated $selectedCategory deals for $placeLabel (${getFormattedDateRange()})",
                Toast.LENGTH_SHORT
            ).show()
        }, 650L)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun refreshProAndAdsState() {
        val isPro = AppPreferences.isPro(this)
        if (isPro) {
            binding.btnProStatus.text = "PRO"
            binding.adViewBanner.visibility = View.GONE
        } else {
            binding.btnProStatus.text = "Pro"
            binding.adViewBanner.visibility = View.VISIBLE
            try {
                binding.adViewBanner.loadAd(AdRequest.Builder().build())
            } catch (_: Exception) {
            }
        }
    }

    private fun showUpgradeProDialog() {
        val isPro = AppPreferences.isPro(this)
        if (isPro) {
            Toast.makeText(
                this,
                "FareSwoop Pro is active (Unlimited 1h alerts & Ad-Free)",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Upgrade to FareSwoop Pro")
            .setMessage(
                "• Unlimited active Car, Hotel & Flight watchers (Free limit: 2)\n" +
                    "• Rapid 1-hour price checks (Free: every 12h)\n" +
                    "• 100% Ad-free experience\n\n" +
                    "£2.99/month or £14.99/year"
            )
            .setPositiveButton("Unlock Pro") { _, _ ->
                AppPreferences.setPro(this, true)
                PriceCheckWorker.schedulePeriodicChecks(
                    this,
                    AppPreferences.PRO_FAST_INTERVAL_HOURS
                )
                refreshProAndAdsState()
                Toast.makeText(this, "FareSwoop Pro Unlocked!", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Keep Free Plan", null)
            .show()
    }

    private fun setupLists() {
        offerAdapter = TravelOfferAdapter(
            onBookClick = { offer ->
                pendingOfferAfterBrowser = offer
                openInCustomTab(offer.bookingUrl)
            },
            onTrackClick = { offer ->
                showArmWatcherSheet(offer)
            }
        )
        binding.rvOffers.layoutManager = LinearLayoutManager(this)
        binding.rvOffers.adapter = offerAdapter

        watchlistAdapter = WatchlistAdapter(
            onOpenDealClick = { item ->
                if (item.savingsGbp >= item.minDropThresholdGbp) {
                    val intent = Intent(this, DropAlertActivity::class.java).apply {
                        putExtra(DropAlertActivity.EXTRA_BOOKING_ID, item.id)
                    }
                    startActivity(intent)
                } else {
                    openInCustomTab(item.bookingUrl)
                }
            },
            onDeleteClick = { item ->
                lifecycleScope.launch {
                    FareSwoopDatabase.getInstance(this@MainActivity).dao().delete(item)
                }
            }
        )
        binding.rvWatchlist.layoutManager = LinearLayoutManager(this)
        binding.rvWatchlist.adapter = watchlistAdapter
    }

    private fun setupSearchControls() {
        binding.btnSearchOffers.setOnClickListener {
            triggerCompareWithLogoLoading()
        }

        binding.btnClearSearch.setOnClickListener {
            clearSearchToLandingState()
        }

        binding.btnRunInstantCheck.setOnClickListener {
            val oneTime = OneTimeWorkRequestBuilder<PriceCheckWorker>().build()
            WorkManager.getInstance(this).enqueue(oneTime)
            Toast.makeText(this, "Scanning live rates for app notifications...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runActiveSearch() {
        val location = binding.etPrimaryLocation.text.toString().trim()
        val startIso = isoDateFormatter.format(collectCalendar.time)
        val endIso = isoDateFormatter.format(returnCalendar.time)
        val pickupTime = timeOnlyFormatter.format(collectCalendar.time)
        val returnTime = timeOnlyFormatter.format(returnCalendar.time)
        val daysOrNights = getSelectedDaysOrNights()
        val selectedCabin = cabinClasses[cabinIndex]

        val results = when (selectedCategory) {
            "HOTEL" -> TravelSearchRepository.searchHotels(
                destination = location,
                checkInIso = startIso,
                checkOutIso = endIso,
                nightsCount = daysOrNights,
                adults = adultsCount,
                children = childrenCount,
                rooms = roomsCount,
                breakfastIncludedOnly = binding.chipHotelBreakfast.isChecked,
                freeCancelOnly = binding.chipHotelFreeCancel.isChecked
            )
            "FLIGHT" -> TravelSearchRepository.searchFlights(
                origin = binding.etFlightOrigin.text.toString().trim(),
                destination = location,
                outboundDateIso = startIso,
                inboundDateIso = endIso,
                adults = adultsCount,
                children = childrenCount,
                cabinClass = selectedCabin,
                directOnly = binding.chipFlightDirect.isChecked,
                cabinBagOnly = binding.chipFlightCabinBag.isChecked
            )
            else -> TravelSearchRepository.searchCars(
                pickupLocation = location,
                pickupDateIso = startIso,
                pickupTime = pickupTime,
                returnDateIso = endIso,
                returnTime = returnTime,
                rentalDays = daysOrNights,
                automaticOnly = binding.chipCarAutoOnly.isChecked,
                freeCancelOnly = binding.chipCarFreeCancel.isChecked
            )
        }
        offerAdapter.submitList(results)
        val displayPlace = location.ifBlank { "default location" }
        binding.tvResultsHeader.text =
            "Showing ${results.size} $selectedCategory deals for $displayPlace • ${getFormattedDateRange()}"
    }

    private fun showArmWatcherSheet(offer: TravelOffer) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetArmWatcherBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val dates = getFormattedDateRange()

        sheetBinding.switchSameSupplier.text = when (offer.category) {
            "HOTEL" -> "Same hotel only (off = any equal/higher star hotel)"
            "FLIGHT" -> "Same airline only (off = any matching route)"
            else -> "Same supplier only (off = any equal/better car)"
        }

        val summaryTitle = "${offer.primaryTitle} ($dates)"

        sheetBinding.tvSheetSummary.text = summaryTitle
        sheetBinding.etBookedPrice.setText(String.format(Locale.UK, "%.2f", offer.totalPriceGbp))
        sheetBinding.etCancelDeadline.setText(
            if (offer.freeCancellation) "Free cancellation active" else "Check fare rules"
        )

        sheetBinding.btnActivateWatcher.setOnClickListener {
            val bookedPrice = sheetBinding.etBookedPrice.text.toString().toDoubleOrNull()
                ?: offer.totalPriceGbp
            val cancelDeadline = sheetBinding.etCancelDeadline.text.toString().trim()
                .ifBlank { "Check fare rules" }
            val chosenHours = when {
                sheetBinding.rbEvery1h.isChecked -> 1L
                sheetBinding.rbEvery3h.isChecked -> 3L
                sheetBinding.rbEvery6h.isChecked -> 6L
                else -> 12L
            }

            lifecycleScope.launch {
                val dao = FareSwoopDatabase.getInstance(this@MainActivity).dao()
                val isPro = AppPreferences.isPro(this@MainActivity)
                val activeCount = dao.getActiveWatchers().size

                if (!isPro && activeCount >= AppPreferences.FREE_MAX_ACTIVE_TRACKERS) {
                    dialog.dismiss()
                    showUpgradeProDialog()
                    return@launch
                }

                val effectiveInterval = if (isPro) {
                    chosenHours.coerceAtLeast(AppPreferences.PRO_FAST_INTERVAL_HOURS)
                } else {
                    AppPreferences.FREE_CHECK_INTERVAL_HOURS
                }

                val tracker = TrackedBooking(
                    category = offer.category,
                    title = summaryTitle,
                    dateRange = dates,
                    supplierName = offer.providerName,
                    bookingUrl = offer.bookingUrl,
                    bookedPriceGbp = bookedPrice,
                    latestPriceGbp = bookedPrice,
                    minDropThresholdGbp = 10.0,
                    sameSupplierOnly = sheetBinding.switchSameSupplier.isChecked,
                    freeCancelDeadline = cancelDeadline,
                    checkIntervalHours = effectiveInterval
                )
                val insertedId = dao.insert(tracker)
                val savedTracker = tracker.copy(id = insertedId)
                PriceCheckWorker.schedulePeriodicChecks(this@MainActivity, effectiveInterval)
                PriceCheckWorker.sendWatchActivatedNotification(this@MainActivity, savedTracker)

                Toast.makeText(
                    this@MainActivity,
                    "Watch alert active! Confirmation notification sent.",
                    Toast.LENGTH_LONG
                ).show()

                dialog.dismiss()
                switchScreenMode(ScreenMode.WATCHLIST)
            }
        }

        dialog.show()
    }

    private fun observeWatchlist() {
        lifecycleScope.launch {
            FareSwoopDatabase.getInstance(this@MainActivity).dao().observeAll()
                .collect { items ->
                    watchlistAdapter.submitList(items)
                    val totalSaved = items.sumOf { it.savingsGbp }
                    binding.tvSummaryWatching.text = "${items.size} Watching"
                    binding.tvSummarySaved.text = String.format(
                        Locale.UK,
                        "£%.0f Potential Savings",
                        totalSaved
                    )
                    binding.tvEmptyWatchlist.visibility =
                        if (items.isEmpty()) View.VISIBLE else View.GONE
                }
        }
    }

    private fun openInCustomTab(url: String) {
        val uri = Uri.parse(url)
        try {
            val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
            customTabsIntent.launchUrl(this, uri)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, uri))
            } catch (_: Exception) {
                Toast.makeText(this, "Unable to open booking link on this device", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

class TravelOfferAdapter(
    private val onBookClick: (TravelOffer) -> Unit,
    private val onTrackClick: (TravelOffer) -> Unit
) : RecyclerView.Adapter<TravelOfferAdapter.VH>() {

    private var items: List<TravelOffer> = emptyList()

    fun submitList(list: List<TravelOffer>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemCarOfferBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onBookClick, onTrackClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemCarOfferBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            offer: TravelOffer,
            onBookClick: (TravelOffer) -> Unit,
            onTrackClick: (TravelOffer) -> Unit
        ) {
            binding.tvVehicleTitle.text = "${offer.badgeLabel} • ${offer.primaryTitle}"
            binding.tvSupplier.text = offer.providerName
            binding.tvSpecs.text = offer.detailsSummary
            binding.tvCancelBadge.text =
                if (offer.freeCancellation) "Free Cancellation" else "Standard Policy"
            binding.tvTotalPrice.text = String.format(Locale.UK, "Est. £%.2f total", offer.totalPriceGbp)
            binding.tvDailyPrice.text = offer.unitPriceLabel

            binding.btnBookBrowser.setOnClickListener { onBookClick(offer) }
            binding.btnTrackOffer.setOnClickListener { onTrackClick(offer) }
        }
    }
}

class WatchlistAdapter(
    private val onOpenDealClick: (TrackedBooking) -> Unit,
    private val onDeleteClick: (TrackedBooking) -> Unit
) : RecyclerView.Adapter<WatchlistAdapter.VH>() {

    private var items: List<TrackedBooking> = emptyList()

    fun submitList(list: List<TrackedBooking>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemWatchlistBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(items[position], onOpenDealClick, onDeleteClick)
    }

    override fun getItemCount(): Int = items.size

    class VH(private val binding: ItemWatchlistBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(
            item: TrackedBooking,
            onOpenDealClick: (TrackedBooking) -> Unit,
            onDeleteClick: (TrackedBooking) -> Unit
        ) {
            binding.tvWatchTitle.text = "[${item.category}] ${item.title}"
            binding.tvWatchDates.text = "${item.dateRange} • ${item.supplierName}"
            binding.tvBookedVsLive.text = String.format(
                Locale.UK,
                "Booked: £%.2f  →  Live: £%.2f",
                item.bookedPriceGbp,
                item.latestPriceGbp
            )

            val drop = item.savingsGbp
            if (drop >= item.minDropThresholdGbp) {
                binding.tvStatusPill.text = String.format(Locale.UK, "PRICE DROP -£%.2f", drop)
                binding.btnActionDeal.text = "Rebook & Save"
            } else {
                binding.tvStatusPill.text = "Watching • Every ${item.checkIntervalHours}h"
                binding.btnActionDeal.text = "Open Booking"
            }

            binding.btnActionDeal.setOnClickListener { onOpenDealClick(item) }
            binding.btnRemoveWatcher.setOnClickListener { onDeleteClick(item) }
        }
    }
}
