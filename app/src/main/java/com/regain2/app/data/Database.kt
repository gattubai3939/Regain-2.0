package com.regain2.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "category_override")
data class CategoryOverride(
    @PrimaryKey val packageName: String,
    val category: String
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_override")
    fun observeAll(): Flow<List<CategoryOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CategoryOverride)
}

@Database(entities = [CategoryOverride::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "regain2.db"
            ).build().also { instance = it }
        }
    }
}
