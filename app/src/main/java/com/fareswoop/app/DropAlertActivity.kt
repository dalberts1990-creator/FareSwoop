package com.fareswoop.app

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import androidx.lifecycle.lifecycleScope
import com.fareswoop.app.databinding.ActivityDropAlertBinding
import kotlinx.coroutines.launch
import java.util.Locale

class DropAlertActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDropAlertBinding
    private var booking: TrackedBooking? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDropAlertBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bookingId = intent.getLongExtra(EXTRA_BOOKING_ID, -1L)
        if (bookingId <= 0L) {
            finish()
            return
        }

        lifecycleScope.launch {
            val item = FareSwoopDatabase.getInstance(this@DropAlertActivity).dao().getById(bookingId)
            if (item == null) {
                finish()
                return@launch
            }
            booking = item
            bindBooking(item)
        }

        binding.btnRebookNow.setOnClickListener {
            val url = booking?.bookingUrl ?: return@setOnClickListener
            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
        }

        binding.btnCancelOldBooking.setOnClickListener {
            val url = booking?.bookingUrl ?: return@setOnClickListener
            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
        }

        binding.btnUpdateTargetPrice.setOnClickListener {
            val current = booking ?: return@setOnClickListener
            lifecycleScope.launch {
                val updated = current.copy(bookedPriceGbp = current.latestPriceGbp)
                FareSwoopDatabase.getInstance(this@DropAlertActivity).dao().update(updated)
                Toast.makeText(
                    this@DropAlertActivity,
                    String.format(Locale.UK, "Target updated to £%.2f", current.latestPriceGbp),
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }

        binding.btnBackHome.setOnClickListener { finish() }
    }

    private fun bindBooking(item: TrackedBooking) {
        val savings = item.savingsGbp
        binding.tvDropHeadline.text = String.format(Locale.UK, "Save £%.2f Now", savings)
        binding.tvDropSubhead.text = item.title
        binding.tvPriceComparison.text = String.format(
            Locale.UK,
            "Original Booked Price: £%.2f\nNew Live Rate: £%.2f\nCancellation Window: %s\n\nReminder: Confirm your new booking first and verify free cancellation rules before cancelling your original reservation.",
            item.bookedPriceGbp,
            item.latestPriceGbp,
            item.freeCancelDeadline
        )
    }

    companion object {
        const val EXTRA_BOOKING_ID = "extra_booking_id"
    }
}
