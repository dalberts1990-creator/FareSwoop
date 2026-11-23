package com.fareswoop.app

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "tracked_bookings")
data class TrackedBooking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String,
    val title: String,
    val dateRange: String,
    val supplierName: String,
    val bookingUrl: String,
    val bookedPriceGbp: Double,
    val latestPriceGbp: Double,
    val minDropThresholdGbp: Double = 10.0,
    val sameSupplierOnly: Boolean = false,
    val freeCancelDeadline: String,
    val checkIntervalHours: Long = 12L,
    val isActive: Boolean = true,
    val lastCheckedMillis: Long = System.currentTimeMillis()
) {
    val savingsGbp: Double
        get() = (bookedPriceGbp - latestPriceGbp).coerceAtLeast(0.0)
}

@Dao
interface TrackedBookingDao {
    @Query("SELECT * FROM tracked_bookings ORDER BY id DESC")
    fun observeAll(): Flow<List<TrackedBooking>>

    @Query("SELECT * FROM tracked_bookings WHERE isActive = 1")
    suspend fun getActiveWatchers(): List<TrackedBooking>

    @Query("SELECT * FROM tracked_bookings WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TrackedBooking?

    @Insert
    suspend fun insert(booking: TrackedBooking): Long

    @Update
    suspend fun update(booking: TrackedBooking)

    @Delete
    suspend fun delete(booking: TrackedBooking)
}

@Database(entities = [TrackedBooking::class], version = 1)
abstract class FareSwoopDatabase : RoomDatabase() {
    abstract fun dao(): TrackedBookingDao

    companion object {
        @Volatile
        private var INSTANCE: FareSwoopDatabase? = null

        fun getInstance(context: Context): FareSwoopDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    FareSwoopDatabase::class.java,
                    "fareswoop.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
