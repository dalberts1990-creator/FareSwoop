package com.fareswoop.app

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.Locale
import java.util.concurrent.TimeUnit

class PriceCheckWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dao = FareSwoopDatabase.getInstance(applicationContext).dao()
        val activeWatchers = dao.getActiveWatchers()

        for (watcher in activeWatchers) {
            val newPrice = TravelSearchRepository.fetchLiveQuote(watcher)
            val updated = watcher.copy(
                latestPriceGbp = newPrice,
                lastCheckedMillis = System.currentTimeMillis()
            )
            dao.update(updated)

            val dropAmount = watcher.bookedPriceGbp - newPrice
            if (dropAmount >= watcher.minDropThresholdGbp) {
                sendPriceDropNotification(updated, dropAmount)
            }
        }

        return Result.success()
    }

    private fun sendPriceDropNotification(booking: TrackedBooking, dropAmount: Double) {
        if (ActivityCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val intent = Intent(applicationContext, DropAlertActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(DropAlertActivity.EXTRA_BOOKING_ID, booking.id)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            booking.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = String.format(
            Locale.UK,
            "FareSwoop Alert: Save £%.0f on %s",
            dropAmount,
            booking.category
        )
        val body = String.format(
            Locale.UK,
            "%s dropped from £%.2f to £%.2f. Tap to rebook now.",
            booking.title,
            booking.bookedPriceGbp,
            booking.latestPriceGbp
        )

        val notification = NotificationCompat.Builder(
            applicationContext,
            FareSwoopApp.CHANNEL_PRICE_DROPS
        )
            .setSmallIcon(android.R.drawable.arrow_down_float)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(booking.id.toInt(), notification)
    }

    companion object {
        private const val WORK_NAME = "fareswoop_periodic_price_check"

        fun schedulePeriodicChecks(context: Context, intervalHours: Long = 12L) {
            val safeHours = intervalHours.coerceAtLeast(1L)
            val request = PeriodicWorkRequestBuilder<PriceCheckWorker>(safeHours, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }
    }
}
