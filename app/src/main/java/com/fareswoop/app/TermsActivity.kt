package com.fareswoop.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.fareswoop.app.databinding.ActivityTermsBinding

class TermsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTermsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTermsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val isReadOnlyReview = intent.getBooleanExtra(EXTRA_READ_ONLY, false)

        binding.tvTermsBody.text = """
            FARESWOOP TERMS & CONDITIONS AND LIABILITY DISCLAIMER

            1. INFORMATION & PRICE MONITORING SERVICE ONLY
            FareSwoop is a price comparison and automated rate-tracking utility. FareSwoop is NOT a travel agency, car rental supplier, airline, hotelier, or booking agent. All reservations, payments, modifications, and cancellations take place directly on third-party provider websites via your device browser.

            2. NO LIABILITY FOR BOOKINGS, PRICES, OR CANCELLATIONS
            To the fullest extent permitted by applicable law, the creator, owner, and operator of FareSwoop accept ZERO liability or responsibility for:
            • Accuracy, availability, or sudden changes in third-party car hire, hotel, or flight prices, taxes, deposits, or insurance fees.
            • Missed price drops, delayed background notifications, battery-saver restrictions on your device, or network outages.
            • Cancellation fees, non-refundable fares, double bookings, or disputes with third-party travel suppliers.

            3. YOUR RESPONSIBILITY BEFORE REBOOKING
            You are solely responsible for verifying that any new booking is confirmed and that your original reservation qualifies for free cancellation BEFORE cancelling or rebooking any travel service.

            4. AFFILIATE LINKS & ADVERTISING
            FareSwoop may earn affiliate commissions when you click through to third-party travel websites and complete a booking, at no additional cost to you.

            5. ACCEPTANCE OF TERMS
            By tapping "I Agree & Accept", you acknowledge that you have read, understood, and accepted these Terms & Conditions and release the developer of FareSwoop from all claims or losses arising from your use of the app.
        """.trimIndent()

        if (isReadOnlyReview) {
            binding.btnAcceptTerms.text = "Close"
            binding.btnDeclineTerms.visibility = View.GONE
            binding.btnAcceptTerms.setOnClickListener { finish() }
        } else {
            binding.btnAcceptTerms.setOnClickListener {
                AppPreferences.setTermsAccepted(this, true)
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            binding.btnDeclineTerms.setOnClickListener {
                finishAffinity()
            }
        }
    }

    companion object {
        const val EXTRA_READ_ONLY = "extra_read_only"
    }
}
