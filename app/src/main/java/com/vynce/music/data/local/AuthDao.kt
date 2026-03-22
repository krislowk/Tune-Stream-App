package com.vynce.music.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.vynce.music.data.model.AuthSession
import kotlinx.coroutines.flow.Flow

@Dao
interface AuthDao {
    @Query("SELECT * FROM auth_session WHERE id = :id")
    fun getSession(id: String = "default_session"): Flow<AuthSession?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: AuthSession)

    @Query("DELETE FROM auth_session")
    suspend fun clearSession()
}
