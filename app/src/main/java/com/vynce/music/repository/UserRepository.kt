package com.vynce.music.repository

import com.vynce.music.db.daos.DatabaseDao
import com.vynce.music.models.User
import kotlinx.coroutines.flow.Flow
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val databaseDao: DatabaseDao
) {
    val currentUser: Flow<User?> = databaseDao.getUser()

    suspend fun saveUser(email: String, name: String?) {
        val user = User(
            email = email,
            name = name,
            avatarUrl = getGravatarUrl(email)
        )
        databaseDao.upsertUser(user)
    }

    private fun getGravatarUrl(email: String): String {
        // Use SHA-256 for Gravatar (Standard practice)
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "https://www.gravatar.com/avatar/$hash?s=200&d=identicon"
    }

    suspend fun clearUser() {
        databaseDao.deleteUser(User(email = ""))
    }
}












