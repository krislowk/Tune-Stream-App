package com.vynce.music.repository

import android.util.Log
import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.models.User
import com.vynce.vynceclient.YouTube
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val databaseDao: DatabaseDao,
    private val preferenceRepository: PreferenceRepository
) {
    val currentUser: Flow<User?> = databaseDao.getUser()

    suspend fun login(cookie: String, visitorData: String? = null) {
        preferenceRepository.saveCookie(cookie)
        if (!visitorData.isNullOrBlank()) {
            preferenceRepository.saveVisitorData(visitorData)
        } else {
            YouTube.visitorData().onSuccess { data ->
                preferenceRepository.saveVisitorData(data)
            }
        }
        refreshAccountInfo()
    }

    suspend fun logout() {
        preferenceRepository.clearSession()
        databaseDao.deleteUser(User(email = ""))
    }

    suspend fun refreshAccountInfo() {
        val cookie = YouTube.cookie
        if (cookie.isNullOrBlank()) return

        // Ensure visitor data is present
        if (YouTube.visitorData.isNullOrBlank() || YouTube.visitorData == YouTube.DEFAULT_VISITOR_DATA) {
            YouTube.visitorData().onSuccess { data ->
                preferenceRepository.saveVisitorData(data)
            }
        }

        YouTube.accountInfo()
            .onSuccess { info ->
                val user = User(
                    email = info.email,
                    name = info.name,
                    avatarUrl = info.thumbnailUrl,
                    cookie = YouTube.cookie,
                    visitorData = YouTube.visitorData
                )
                databaseDao.upsertUser(user)
            }
            .onFailure {
                Log.e("UserRepository", "Failed to refresh account info", it)
            }
    }

    suspend fun saveUser(email: String, name: String?, avatarUrl: String?) {
        val user = User(
            email = email,
            name = name,
            avatarUrl = avatarUrl,
            cookie = YouTube.cookie,
            visitorData = YouTube.visitorData
        )
        databaseDao.upsertUser(user)
    }

    suspend fun clearUser() {
        databaseDao.deleteUser(User(email = ""))
    }
}















