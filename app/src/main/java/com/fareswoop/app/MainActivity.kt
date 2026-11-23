package com.fareswoop.app

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
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
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var offerAdapter: TravelOfferAdapter
    private lateinit var watchlistAdapter: WatchlistAdapter
    private var pendingOfferAfterBrowser: TravelOffer? = null
    private var selectedCategory = "CAR"

    private val requestNotifPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!AppPreferences.hasAcceptedTerms(this)) {
            startActivity(Intent(this, TermsActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ensureNotificationPermission()
        val defaultHours = if (AppPreferences.isPro(this)) {
            AppPreferences.PRO_FAST_INTERVAL_HOURS
        } else {
            AppPreferences.FREE_CHECK_INTERVAL_HOURS
        }
        PriceCheckWorker.schedulePeriodicChecks(this, defaultHours)

        refreshProAndAdsState()
        setupCategoryTabs()
        setupLists()
        setupSearchControls()
        observeWatchlist()
    }

    override fun onResume() {
        super.onResume()
        val offer = pendingOfferAfterBrowser
        if (offer != null) {
            pendingOfferAfterBrowser = null
            binding.cardPostBookingBanner.visibility = View.VISIBLE
            binding.tvPostBookingPrompt.text = String.format(
                Locale.UK,
                "Did you book %s for £%.2f?",
                offer.primaryTitle,
                offer.totalPriceGbp
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
            binding.btnProStatus.text = "Go Pro"
            binding.adViewBanner.visibility = View.VISIBLE
            binding.adViewBanner.loadAd(AdRequest.Builder().build())
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

    private fun setupCategoryTabs() {
        binding.chipCatCar.setOnClickListener {
            selectedCategory = "CAR"
            binding.etFlightOrigin.visibility = View.GONE
            binding.etPrimaryLocation.hint = "Pick-up Airport or City (e.g. Alicante ALC)"
            binding.etPrimaryLocation.setText("Alicante Airport (ALC)")
            binding.chipsCarFilters.visibility = View.VISIBLE
            binding.chipsHotelFilters.visibility = View.GONE
            binding.chipsFlightFilters.visibility = View.GONE
            binding.btnSearchOffers.text = "Compare Car Hire Prices"
            runActiveSearch()
        }

        binding.chipCatHotel.setOnClickListener {
            selectedCategory = "HOTEL"
            binding.etFlightOrigin.visibility = View.GONE
            binding.etPrimaryLocation.hint = "Destination City or Hotel"
            binding.etPrimaryLocation.setText("Alicante City Centre")
            binding.chipsCarFilters.visibility = View.GONE
            binding.chipsHotelFilters.visibility = View.VISIBLE
            binding.chipsFlightFilters.visibility = View.GONE
            binding.btnSearchOffers.text = "Compare Hotel Rates"
            runActiveSearch()
        }

        binding.chipCatFlight.setOnClickListener {
            selectedCategory = "FLIGHT"
            binding.etFlightOrigin.visibility = View.VISIBLE
            binding.etPrimaryLocation.hint = "Arrival Airport (e.g. Alicante ALC)"
            binding.etPrimaryLocation.setText("Alicante (ALC)")
            binding.chipsCarFilters.visibility = View.GONE
            binding.chipsHotelFilters.visibility = View.GONE
            binding.chipsFlightFilters.visibility = View.VISIBLE
            binding.btnSearchOffers.text = "Compare Flight Fares"
            runActiveSearch()
        }

        binding.btnModeSearch.setOnClickListener {
            binding.layoutSearchSection.visibility = View.VISIBLE
            binding.layoutWatchlistSection.visibility = View.GONE
        }
        binding.btnModeWatchlist.setOnClickListener {
            binding.layoutSearchSection.visibility = View.GONE
            binding.layoutWatchlistSection.visibility = View.VISIBLE
        }
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

        runActiveSearch()
    }

    private fun setupSearchControls() {
        binding.btnSearchOffers.setOnClickListener { runActiveSearch() }

        binding.btnRunInstantCheck.setOnClickListener {
            val oneTime = OneTimeWorkRequestBuilder<PriceCheckWorker>().build()
            WorkManager.getInstance(this).enqueue(oneTime)
            Toast.makeText(this, "Scanning live Car, Hotel & Flight rates...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun runActiveSearch() {
        val location = binding.etPrimaryLocation.text.toString().trim()
        val results = when (selectedCategory) {
            "HOTEL" -> TravelSearchRepository.searchHotels(
                destination = location,
                breakfastIncludedOnly = binding.chipHotelBreakfast.isChecked,
                freeCancelOnly = binding.chipHotelFreeCancel.isChecked
            )
            "FLIGHT" -> TravelSearchRepository.searchFlights(
                origin = binding.etFlightOrigin.text.toString().trim(),
                destination = location,
                directOnly = binding.chipFlightDirect.isChecked,
                cabinBagOnly = binding.chipFlightCabinBag.isChecked
            )
            else -> TravelSearchRepository.searchCars(
                pickupLocation = location,
                automaticOnly = binding.chipCarAutoOnly.isChecked,
                freeCancelOnly = binding.chipCarFreeCancel.isChecked
            )
        }
        offerAdapter.submitList(results)
    }

    private fun showArmWatcherSheet(offer: TravelOffer) {
        val dialog = BottomSheetDialog(this)
        val sheetBinding = BottomSheetArmWatcherBinding.inflate(layoutInflater)
        dialog.setContentView(sheetBinding.root)

        val location = binding.etPrimaryLocation.text.toString().trim()
        val dates = binding.etDateRange.text.toString().trim().ifBlank { "14 Jun – 21 Jun" }

        sheetBinding.switchSameSupplier.text = when (offer.category) {
            "HOTEL" -> "Same hotel only (off = any equal/higher star hotel)"
            "FLIGHT" -> "Same airline only (off = any matching route)"
            else -> "Same supplier only (off = any equal/better car)"
        }

        val summaryTitle = if (offer.category == "FLIGHT") {
            "${offer.primaryTitle} ($dates)"
        } else {
            "$location • ${offer.primaryTitle}"
        }

        sheetBinding.tvSheetSummary.text = "$summaryTitle • $dates"
        sheetBinding.etBookedPrice.setText(String.format(Locale.UK, "%.2f", offer.totalPriceGbp))
        sheetBinding.etMinDrop.setText("10")
        sheetBinding.etCancelDeadline.setText(
            if (offer.freeCancellation) "Free cancellation active" else "Check fare rules"
        )

        sheetBinding.btnActivateWatcher.setOnClickListener {
            val bookedPrice = sheetBinding.etBookedPrice.text.toString().toDoubleOrNull()
                ?: offer.totalPriceGbp
            val minDrop = sheetBinding.etMinDrop.text.toString().toDoubleOrNull() ?: 10.0
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
                    minDropThresholdGbp = minDrop,
                    sameSupplierOnly = sheetBinding.switchSameSupplier.isChecked,
                    freeCancelDeadline = cancelDeadline,
                    checkIntervalHours = effectiveInterval
                )
                dao.insert(tracker)
                PriceCheckWorker.schedulePeriodicChecks(this@MainActivity, effectiveInterval)

                Toast.makeText(
                    this@MainActivity,
                    "${offer.category} watcher armed! Checking every ${effectiveInterval}h.",
                    Toast.LENGTH_LONG
                ).show()

                dialog.dismiss()
                binding.layoutSearchSection.visibility = View.GONE
                binding.layoutWatchlistSection.visibility = View.VISIBLE
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
        val customTabsIntent = CustomTabsIntent.Builder().setShowTitle(true).build()
        customTabsIntent.launchUrl(this, Uri.parse(url))
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
            binding.tvTotalPrice.text = String.format(Locale.UK, "£%.2f total", offer.totalPriceGbp)
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
