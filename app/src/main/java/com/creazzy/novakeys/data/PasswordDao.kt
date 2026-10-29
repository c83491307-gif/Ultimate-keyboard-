package com.creazzy.novakeys.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface PasswordDao {
    @Query("SELECT * FROM password_entries ORDER BY label COLLATE NOCASE")
    fun observeAll(): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE website LIKE '%' || :query || '%' OR label LIKE '%' || :query || '%' OR username LIKE '%' || :query || '%' ORDER BY label COLLATE NOCASE")
    fun search(query: String): Flow<List<PasswordEntry>>

    @Query("SELECT * FROM password_entries WHERE website = :website COLLATE NOCASE LIMIT 10")
    suspend fun forWebsite(website: String): List<PasswordEntry>

    @Query("SELECT * FROM password_entries WHERE id = :id LIMIT 1")
    suspend fun byId(id: Long): PasswordEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: PasswordEntry): Long

    @Delete
    suspend fun delete(entry: PasswordEntry)

    @Query("SELECT password FROM password_entries")
    suspend fun allPasswords(): List<String>
}
