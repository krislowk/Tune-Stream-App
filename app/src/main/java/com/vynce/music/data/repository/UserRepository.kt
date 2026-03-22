package com.vynce.music.data.repository

import com.vynce.music.data.local.UserDao
import com.vynce.music.data.model.User
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepository @Inject constructor(
    private val userDao: UserDao
) {
    val currentUser: Flow<User?> = userDao.getUser()

    suspend fun saveUser(email: String, name: String?) {
        val user = User(
            email = email,
            name = name,
            avatarUrl = getGravatarUrl(email)
        )
        userDao.insertUser(user)
    }

    private fun getGravatarUrl(email: String): String {
        // Use SHA-256 for Gravatar (Standard practice)
        val hash = java.security.MessageDigest.getInstance("SHA-256")
            .digest(email.trim().lowercase().toByteArray())
            .joinToString("") { "%02x".format(it) }
        return "https://www.gravatar.com/avatar/$hash?s=200&d=identicon"
    }

    suspend fun clearUser() {
        userDao.deleteUser(User(email = ""))
    }
}
