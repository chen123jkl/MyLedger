package com.example.myledger.data


import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert
    suspend fun insert(transaction: Transaction)

    @Update
    suspend fun update(transaction: Transaction)

    @Delete
    suspend fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY date DESC")
    fun getAll(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions WHERE strftime('%Y-%m', date/1000, 'unixepoch') = :yearMonth ORDER BY date DESC")
    fun getByMonth(yearMonth: String): Flow<List<Transaction>>

    @Query("SELECT SUM(amount) FROM transactions WHERE strftime('%Y-%m', date/1000, 'unixepoch') = :yearMonth")
    fun getTotalByMonth(yearMonth: String): Flow<Double?>
}