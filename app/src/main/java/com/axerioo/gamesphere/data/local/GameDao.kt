package com.axerioo.gamesphere.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// --- GameDao Interface ---
// This interface defines the Data Access Object (DAO) for the 'games' table.

@Dao
interface GameDao {

    // --- Get All Games ---
    // Retrieves all games from the 'games' table, ordered alphabetically by name.
    @Query("SELECT * FROM games ORDER BY name ASC")
    fun getAllGames(): Flow<List<GameEntity>>

    // --- Get Game by ID ---
    // Retrieves a single game from the 'games' table based on its ID.
    @Query("SELECT * FROM games WHERE id = :id")
    suspend fun getGameById(id: Long): GameEntity?

    // --- Insert Game ---
    // Inserts a new game into the 'games' table.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGame(game: GameEntity)

    // --- Delete Game by ID ---
    // Deletes a game from the 'games' table based on its ID.
    @Query("DELETE FROM games WHERE id = :id")
    suspend fun deleteGameById(id: Long)

    // --- Is Game in Library ---
    // A convenience method to check if a game with a specific ID exists in the library (games table).
    @Query("SELECT COUNT(id) FROM games WHERE id = :id")
    suspend fun isGameInLibrary(id: Long): Int
}