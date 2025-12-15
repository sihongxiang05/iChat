package com.example.ichat.chat

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ChatMessageEntity::class], version = 2, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatMessageDao

    companion object {
        @Volatile private var INSTANCE: ChatDatabase? = null
        fun get(ctx: Context): ChatDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(ctx.applicationContext, ChatDatabase::class.java, "chat.db")
                .addMigrations(MIGRATION_1_2)
                .build().also { INSTANCE = it }
        }
        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN messageId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_messages ADD COLUMN status TEXT NOT NULL DEFAULT 'delivered'")
            }
        }
    }
}
