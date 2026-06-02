package com.vynce.music

import com.vynce.vynceclient.YouTube
import kotlinx.coroutines.test.runTest
import org.junit.Test

class YouTubeAccountInfoTest {

    @Test
    fun `accountInfo should NOT crash with null cookie`(): Unit = runTest {

        try {
            val result = YouTube.accountInfo()
            println("SUCCESS: $result")
        } catch (e: Throwable) {
            println("🔥 CRASH CAUGHT")
            e.printStackTrace()

            // this forces full visibility in logs
            throw e
        }
    }
}


