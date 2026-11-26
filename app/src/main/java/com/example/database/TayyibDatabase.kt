package com.example.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "scan_history")
data class ScanHistoryEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val brands: String,
    val imageUrl: String?,
    val verdict: String,
    val evidenceQuality: String,
    val scannedAtMillis: Long
)

@Entity(tableName = "favourites")
data class FavouriteEntity(
    @PrimaryKey val barcode: String,
    val productName: String,
    val brands: String,
    val imageUrl: String?,
    val lastKnownVerdict: String,
    val lastVerifiedMillis: Long
)

@Entity(tableName = "cached_products")
data class CachedProductEntity(
    @PrimaryKey val barcode: String,
    val rawProductJson: String,
    val cachedAtMillis: Long
)

@Dao
interface ScanHistoryDao {
    @Query("SELECT * FROM scan_history ORDER BY scannedAtMillis DESC")
    fun observeAll(): Flow<List<ScanHistoryEntity>>

    @Query("SELECT * FROM scan_history ORDER BY scannedAtMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ScanHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: ScanHistoryEntity)

    @Query("DELETE FROM scan_history WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)

    @Query("DELETE FROM scan_history")
    suspend fun clearAll()
}

@Dao
interface FavouriteDao {
    @Query("SELECT * FROM favourites ORDER BY lastVerifiedMillis DESC")
    fun observeAll(): Flow<List<FavouriteEntity>>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE barcode = :barcode)")
    fun observeIsFavourite(barcode: String): Flow<Boolean>

    @Query("SELECT EXISTS(SELECT 1 FROM favourites WHERE barcode = :barcode)")
    suspend fun isFavourite(barcode: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: FavouriteEntity)

    @Query("DELETE FROM favourites WHERE barcode = :barcode")
    suspend fun deleteByBarcode(barcode: String)
}

@Dao
interface CachedProductDao {
    @Query("SELECT * FROM cached_products WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): CachedProductEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CachedProductEntity)

    @Query("DELETE FROM cached_products")
    suspend fun clearAll()

    @Query("SELECT COUNT(*) FROM cached_products")
    suspend fun count(): Int
}

@Database(
    entities = [
        ScanHistoryEntity::class,
        FavouriteEntity::class,
        CachedProductEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class TayyibDatabase : RoomDatabase() {
    abstract fun scanHistoryDao(): ScanHistoryDao
    abstract fun favouriteDao(): FavouriteDao
    abstract fun cachedProductDao(): CachedProductDao

    companion object {
        fun build(context: Context): TayyibDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                TayyibDatabase::class.java,
                "tayyib_local.db"
            ).fallbackToDestructiveMigration(dropAllTables = true).build()
    }
}
