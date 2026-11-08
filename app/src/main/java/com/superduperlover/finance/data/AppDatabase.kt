package com.superduperlover.finance.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        AccountEntity::class,
        CategoryEntity::class,
        TxnEntity::class,
        BudgetEntity::class,
        GoalEntity::class,
        ContributionEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "finance.db")
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
