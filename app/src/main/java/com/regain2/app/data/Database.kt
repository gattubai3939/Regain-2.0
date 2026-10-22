package com.regain2.app.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "category_override")
data class CategoryOverride(
    @PrimaryKey val packageName: String,
    val category: String
)

/** One rule per app: full block, daily limit (minutes, 0 = none), pause screen. */
@Entity(tableName = "app_rule")
data class AppRule(
    @PrimaryKey val packageName: String,
    val blocked: Boolean = false,
    val limitMinutes: Int = 0,
    val pause: Boolean = false
)

@Dao
interface CategoryDao {
    @Query("SELECT * FROM category_override")
    fun observeAll(): Flow<List<CategoryOverride>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: CategoryOverride)
}

@Dao
interface RuleDao {
    @Query("SELECT * FROM app_rule")
    fun observeAll(): Flow<List<AppRule>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppRule)

    @Query("DELETE FROM app_rule WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}

@Database(entities = [CategoryOverride::class, AppRule::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun ruleDao(): RuleDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext, AppDatabase::class.java, "regain2.db"
            ).fallbackToDestructiveMigration().build().also { instance = it }
        }
    }
}
