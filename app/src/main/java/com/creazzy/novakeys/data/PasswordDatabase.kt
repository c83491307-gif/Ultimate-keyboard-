package com.creazzy.novakeys.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

@Database(entities = [PasswordEntry::class], version = 1, exportSchema = true)
abstract class PasswordDatabase : RoomDatabase() {
    abstract fun passwords(): PasswordDao

    companion object {
        @Volatile private var INSTANCE: PasswordDatabase? = null
        fun get(context: Context): PasswordDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: run {
                System.loadLibrary("sqlcipher")
                val key = DatabaseKeyManager(context.applicationContext).passphrase()
                val factory = SupportOpenHelperFactory(key)
                Room.databaseBuilder(context.applicationContext, PasswordDatabase::class.java, "novakeys-vault.db")
                    .openHelperFactory(factory)
                    .build().also { INSTANCE = it }
            }
        }
    }
}
