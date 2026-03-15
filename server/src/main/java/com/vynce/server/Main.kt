package com.vynce.server

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.compression.ContentEncoding
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.encoding.zstd.ZstdEncoder
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo

fun main() {

    val client = HttpClient(CIO) {
        install(ContentEncoding) {

            customEncoder(ZstdEncoder())

            gzip()

            deflate()
        }
    }

    embeddedServer(
        Netty,
        port = 8080,
        watchPaths = listOf("classes")
    ) {
        routing {
            get("/") {
                call.respondText("Hot reload test")
            }
        }

        routing {

            get("/search") {

                val response: HttpResponse = client.post(
                    "https://www.youtube.com/youtubei/v1/search"
                ) {

                    contentType(ContentType.Application.Json)
                    headers {
                        append(
                            HttpHeaders.UserAgent,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
                        )
                    }
                    setBody(
                        """
                        """.trimIndent()
                    )
                }

                val channel: ByteReadChannel = response.body()
                call.respondBytesWriter(ContentType.Application.Json) {
                    channel.copyTo(this)
                }
            }
        }
        routing {
            get("/player") {

                val videoId = call.request.queryParameters["id"]
                    ?: return@get call.respondText("Missing id parameter")

                val response: HttpResponse = client.post(
                    "https://www.youtube.com/youtubei/v1/player"
                ) {

                    contentType(ContentType.Application.Json)

                    headers {

                        append(
                            HttpHeaders.UserAgent,
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/145.0.0.0 Safari/537.36"
                        )

                        append(
                            "X-Goog-Visitor-Id",
                            "Cgt4eXphYmNkZWYxMiD4zq2uBjIKCgJJThIEGgAgQA%3D%3D"
                        )

                        append(
                            HttpHeaders.Cookie,
                            "VISITOR_INFO1_LIVE=vNSTtOAlZxw; VISITOR_PRIVACY_METADATA=CgJJThIEGgAgQA%3D%3D; LOGIN_INFO=AFmmF2swRgIhAKlDYkUtAuiC6OJk1nAt93Bw5doYUqQQ_t6oUgHBzv3XAiEAqq8ESY_JpEAHSjsdbh2iyrtOVER1KziOiTkOzcRtTCU:QUQ3MjNmeDRGRHgwYUR5V3JZY3hSdzRLaWVVN3k3VEg3RnpXRHZUVGhGRVFpTDNNeEU1WFB0V190WU56aTBscTFMVzJZRk5XclY3dTREdFdva2RzVVdLR19kSExMLVBsZ1llYUVFTlNaYTVPQ3Q2Ny1QY25CcmdvTm52QlhxRWJzQkxLRzk3OW4wWXFrUE42bDFTMmxkTGs0aVdFMzJVRG53; YSC=i-XG-hVQGHg; _gcl_au=1.1.351219756.1772817135; HSID=AX79kDPK8t5gYkM2B; SSID=A2-r6D-MX8zZIWbYN; APISID=9wzpPhFkQIzeGAPV/AUR0T6eGg5EE3qTNU; SAPISID=qv4cLSRF0biVPJRO/Ajk9-IFLENvWKes9d; __Secure-1PAPISID=qv4cLSRF0biVPJRO/Ajk9-IFLENvWKes9d; __Secure-3PAPISID=qv4cLSRF0biVPJRO/Ajk9-IFLENvWKes9d; SID=g.a0007ggnqpp81L8fJ-n-UjQYBlwcvGkvOTCpEonDtx-Co5Uasj74tK6BMfv8s1a63U6EOh-fLgACgYKAUgSARMSFQHGX2MigJLcoWeolxfTT6EdjvG86hoVAUF8yKoL1FUQxx1r1ReSkuXjeWzM0076; __Secure-1PSID=g.a0007ggnqpp81L8fJ-n-UjQYBlwcvGkvOTCpEonDtx-Co5Uasj74tJFg4lzY29fsf0LZVlDLkwACgYKASESARMSFQHGX2MilHvcSmwh1f-zk3utmqa1-xoVAUF8yKpoV7DMsl168myp4ZBPJ_tk0076; __Secure-3PSID=g.a0007ggnqpp81L8fJ-n-UjQYBlwcvGkvOTCpEonDtx-Co5Uasj74p6FksrXWz6UCKCrW9mz50AACgYKAe8SARMSFQHGX2MiSFVjDhB53F9iDmbyUUZLGRoVAUF8yKrj1WPiwNjAWwKsoVPCX2SA0076; __Secure-1PSIDTS=sidts-CjQBBj1CYo2X34FKANVGwDiWCL05RAAvliewuJMh0pNdYSlG0-HHqP1jXRAN2f0NWx9SMetvEAA; __Secure-3PSIDTS=sidts-CjQBBj1CYo2X34FKANVGwDiWCL05RAAvliewuJMh0pNdYSlG0-HHqP1jXRAN2f0NWx9SMetvEAA; __Secure-YNID=16.YT=c5si1PK8Z6vOeAaB7om-yBsnX6K_XX8UahlzBR-jopQOclqki0cW4seWF90_siYWW4_OiocbHiRpnnZbWGehAbh7aRHqtjtGjoYZcKHcf41T9jkt5LvHb_Y3EtAkTRufXbKVmSHZAD3euORLiCwU6Qn8-JDzYJ1Vhq2GjrhWGfuruqTkR0_Y89HswCVFJNdJd-1U5V4t5nWlYkRlh-OWcat8Reublrh_xM5qjUU_tk6Af2PoWg5dSLsnjDLov2xp7Wsm3Qo-MvIGcq373l1_EqQ47wb8w9uTuQszCCbe9iMSm6PoVZznROmbFVMrg8xpYDe3bd9aOpgUAUTYhgIfQA; PREF=f4=4000000&f6=40000000&tz=Asia.Calcutta&repeat=NONE&autoplay=true; __Secure-ROLLOUT_TOKEN=CLu8poezzI3sHBClquarrv-KAxjHiKzag6GTAw%3D%3D; SIDCC=AKEyXzU3CSeEEMd3uP7i6jQCm4aMdxXK9FnQLfYt7JzA33fmk4zsuXNc87gnERSHSyNMNhhYSA; __Secure-1PSIDCC=AKEyXzWdaoYDUjRBKbM1DLmaxb2BxZtvJFxuR33AT7hp_WaBcZEgMJU-rF-cJPufXk78-Lj-r7Q; __Secure-3PSIDCC=AKEyXzXuYOAsM7fdL-qLY_pNx7R9Kjmcwe_nYjiZ77eeuE_U6Zp1oL9ncwVUvdwi6oKA8mWxwEU"
                        )
                    }
                    setBody(
                        """
    {
      "context": {
        "client": {
          "clientName": "WEB_REMIX",
          "clientVersion": "1.20260311.03.00",
          "visitorData": "Cgt4eXphYmNkZWYxMiD4zq2uBjIKCgJJThIEGgAgQA%3D%3D"
        }
      },
      "videoId": "$videoId"
    }
    """.trimIndent()
                    )
                }

                val channel: ByteReadChannel = response.body()

                call.respondBytesWriter(ContentType.Application.Json) {
                    channel.copyTo(this)
                }
            }
        }

    }.start(wait = true)
}