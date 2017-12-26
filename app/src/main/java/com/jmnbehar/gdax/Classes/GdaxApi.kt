package com.jmnbehar.gdax.Classes

import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import java.time.Clock
import java.time.LocalDateTime
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by jmnbehar on 12/18/2017.
 */

class ApiCredentials(val passPhrase: String, val apiKey: String, val secret: String)



sealed class GdaxApi: FuelRouting {


    companion object {
        lateinit var credentials: ApiCredentials
        val basePath = "https://api.gdax.com"

        init {
            FuelManager.instance.basePath = basePath
        }

    }

    override val basePath = Companion.basePath


    class accounts() : GdaxApi()
    class account(val accountId: String) : GdaxApi()
    class products() : GdaxApi()
    class ticker(val productId: String) : GdaxApi()
    class candles(val productId: String, val time: Int = 86400, val granularity: Int = 432) : GdaxApi()


    fun executeRequest(onComplete: (result: Result<String, FuelError>) -> Unit) {
        Fuel.request(this).responseString { _, _, result ->
            onComplete(result)
        }
    }

    override val method: Method
        get() {
            return when (this) {
                is accounts -> Method.GET
                is account -> Method.GET
                is products -> Method.GET
                is ticker -> Method.GET
                is candles -> Method.GET
            }
        }


    override val path: String
        get() {
            return when (this) {
                is accounts -> "/accounts"
                is account -> "/accounts/$accountId"
                is products -> "/products"
                is ticker -> "/products/$productId/ticker"
                is candles -> "/products/$productId/candles"
            }
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            when (this) {
                is candles -> {

                    var now: LocalDateTime = LocalDateTime.now(Clock.systemUTC())
                    var start = now.minusDays(1)
                    return listOf(Pair("start", start), Pair("end", now), Pair("granularity", granularity.toString()))
                }
                else -> return null
            }
        }


    override val headers: Map<String, String>?
        get() {
            val body = ""
            var timestamp = Date().toInstant().epochSecond.toString()
            var message = timestamp + method + path + body
            println("timestamp:")
            println(timestamp)

            val secretDecoded = Base64.getDecoder().decode(credentials.secret)

            val sha256HMAC = Mac.getInstance("HmacSHA256")
            val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
            sha256HMAC.init(secretKey)

            val hash = Base64.getEncoder().encodeToString(sha256HMAC.doFinal(message.toByteArray()))
            println("hash:")
            println(hash)

            var headers: Map<String, String> = mapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.passPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))
            return headers
        }
}