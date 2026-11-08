package com.superduperlover.finance.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.execSQL

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TxnEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        ContributionEntity::class,
    ],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = Migration1To2::class)],
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "finance.db")
                .addMigrations(Migration2To3)
                .addCallback(SeedCallback)
                .build()
    }
}

/** Default categories (icon = key into ui/components/CategoryIcons). Inserted once, when the DB is created. */
private object SeedCallback : RoomDatabase.Callback() {
    private val expense = listOf(
        "Ăn uống" to "food",
        "Đi lại" to "transport",
        "Nhà ở" to "home",
        "Hóa đơn" to "bill",
        "Mua sắm" to "shopping",
        "Giải trí" to "fun",
        "Sức khỏe" to "health",
        "Giáo dục" to "education",
        "Khác" to "other",
    )
    private val income = listOf(
        "Lương" to "salary",
        "Thưởng" to "bonus",
        "Thu nhập khác" to "income_other",
    )

    override fun onCreate(db: SupportSQLiteDatabase) {
        expense.forEach { (name, icon) -> insert(db, name, "EXPENSE", icon) }
        income.forEach { (name, icon) -> insert(db, name, "INCOME", icon) }
    }

    private fun insert(db: SupportSQLiteDatabase, name: String, kind: String, icon: String) {
        db.execSQL(
            "INSERT INTO categories (name, kind, icon, archived) VALUES (?, ?, ?, 0)",
            arrayOf<Any>(name, kind, icon),
        )
    }
}

/**
 * v1 -> v2 (currencies + account colour). Room adds the columns itself (currency 'VND', colorKey 'emerald',
 * toAmount NULL); this picks the default colour of each existing account by its type. No data is lost.
 */
class Migration1To2 : AutoMigrationSpec {
    override fun onPostMigrate(connection: SQLiteConnection) {
        connection.execSQL(
            "UPDATE accounts SET colorKey = CASE type " +
                "WHEN 'BANK' THEN 'sky' WHEN 'CARD' THEN 'indigo' WHEN 'EWALLET' THEN 'violet' ELSE 'emerald' END",
        )
    }
}

/**
 * v2 -> v3: budgets repeat (period, startDate, rollover) instead of belonging to one month; goals get completedAt.
 * Existing budgets become MONTH budgets starting on the 1st of their old month, rollover off. Rows are copied,
 * nothing is lost. The CREATE statement is the one Room generated into schemas/.../3.json.
 */
object Migration2To3 : Migration(2, 3) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "CREATE TABLE IF NOT EXISTS `budgets_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`period` TEXT NOT NULL, `startDate` TEXT NOT NULL, `accountId` INTEGER, `categoryId` INTEGER, " +
                "`limitAmount` INTEGER NOT NULL, `currency` TEXT NOT NULL DEFAULT 'VND', `rollover` INTEGER NOT NULL)",
        )
        connection.execSQL(
            "INSERT INTO `budgets_new` (`id`, `period`, `startDate`, `accountId`, `categoryId`, `limitAmount`, " +
                "`currency`, `rollover`) SELECT `id`, 'MONTH', `month` || '-01', `accountId`, `categoryId`, " +
                "`limitAmount`, `currency`, 0 FROM `budgets`",
        )
        connection.execSQL("DROP TABLE `budgets`")
        connection.execSQL("ALTER TABLE `budgets_new` RENAME TO `budgets`")
        connection.execSQL("ALTER TABLE `goals` ADD COLUMN `completedAt` TEXT")
    }
}
