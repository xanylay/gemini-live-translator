package com.example.data.local

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

@Entity(tableName = "transcript_entries")
data class TranscriptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val burmeseText: String,
    val timestampMillis: Long,
    val sourceMode: String,
    val engineMode: String
)

@Dao
interface TranscriptDao {
    @Query("SELECT * FROM transcript_entries ORDER BY timestampMillis DESC, id DESC")
    fun observeAllNewestFirst(): Flow<List<TranscriptEntity>>

    @Query("SELECT * FROM transcript_entries ORDER BY timestampMillis ASC, id ASC")
    suspend fun getAllChronological(): List<TranscriptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: TranscriptEntity): Long

    @Query("DELETE FROM transcript_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM transcript_entries")
    suspend fun deleteAll()
}

@Database(entities = [TranscriptEntity::class], version = 1, exportSchema = false)
abstract class TranscriptDatabase : RoomDatabase() {
    abstract fun transcriptDao(): TranscriptDao

    companion object {
        @Volatile
        private var instance: TranscriptDatabase? = null

        fun getInstance(context: Context): TranscriptDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    TranscriptDatabase::class.java,
                    "gemini_live_translator.db"
                ).build().also { instance = it }
            }
        }
    }
}
