package com.example.aeropass.data.database.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create new credentials table with updated schema
        database.execSQL("""
            CREATE TABLE IF NOT EXISTS `credentials_new` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `credentialType` TEXT NOT NULL DEFAULT 'LOGIN',
                `title` TEXT NOT NULL,
                `encryptedData` TEXT NOT NULL,
                `category` TEXT NOT NULL DEFAULT 'General',
                `tags` TEXT NOT NULL DEFAULT '',
                `dateCreated` INTEGER NOT NULL,
                `lastUpdated` INTEGER NOT NULL,
                `isFavorite` INTEGER NOT NULL DEFAULT 0
            )
        """)
        
        // Migrate existing data from old table to new table
        // Convert old credential format to new LOGIN credential format
        database.execSQL("""
            INSERT INTO credentials_new (
                id, credentialType, title, encryptedData, category, tags, 
                dateCreated, lastUpdated, isFavorite
            )
            SELECT 
                id,
                'LOGIN' as credentialType,
                websiteName as title,
                json_object(
                    'username', username,
                    'email', email,
                    'password', encryptedPassword,
                    'url', url,
                    'recoveryInfo', recoveryInfo,
                    'notes', notes
                ) as encryptedData,
                category,
                '' as tags,
                dateCreated,
                lastUpdated,
                isFavorite
            FROM credentials
        """)
        
        // Drop old table
        database.execSQL("DROP TABLE credentials")
        
        // Rename new table to credentials
        database.execSQL("ALTER TABLE credentials_new RENAME TO credentials")
        
        // Create index for title
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_credentials_title` ON `credentials` (`title`)")
    }
}