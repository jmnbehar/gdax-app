package com.jmnbehar.gdax.Classes

import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Created by jmnbehar on 12/19/2017.
 */

enum class TradeType {
    BUY,
    SELL;

    override fun toString() : String {
        return when (this) {
            BUY -> "buy"
            SELL -> "sell"
        }
    }
}
enum class TradeSubType {
    MARKET,
    LIMIT,
    STOP;

    override fun toString() : String {
        return when (this) {
            MARKET -> "market"
            LIMIT -> "limit"
            STOP -> "stop"
        }
    }
    companion object {
        fun fromString(string: String) : TradeSubType {
            return when (string) {
                "market"  ->  MARKET
                "limit"  -> LIMIT
                "stop" -> STOP
                else -> MARKET
            }
        }
    }
}

data class ApiProduct(
        val id: String,
        val base_currency: String,
        val quote_currency: String,
        val base_min_size: String,
        val base_max_size: String,
        val quote_increment: String,
        val display_name: String,
        val status: String,
        val margin_enabled: Boolean,
        val status_message: String?)

data class ApiOrder(
        val id: String,
        val price: String,
        val size: String?,
        val product_id: String,
        val side: String,
        val stp: String,
        val funds: String?,
        val specified_funds: String?,
        val type: String,
        val time_in_force: String,
        val post_only: Boolean,
        val created_at: String,
        val done_at: String,
        val done_reason: String,
        val fill_fees: String,
        val filled_size: String,
        val executed_value: String,
        val status: String,
        val settled: Boolean)

data class ApiFill(
        val trade_id: Int,
        val product_id: String,
        val price: String,
        val size: String,
        val order_id: String,
        val created_at: String,
        val liquidity: String,
        val fee: String,
        val settled: Boolean,
        val side: String)

data class ApiTicker(
        val trade_id: Int,
        val price: String,
        val size: String,
        val volume: String,
        val time: String)

data class ApiCurrencies(
        val id: String,
        val name: String,
        val min_size: String)

data class ApiStats(
        val open: String,
        val high: String,
        val low: String,
        val volume: String)

data class ApiTime(
        val iso: String,
        val epoch: String)

data class Candle(
        val time: Double,
        val low: Double,
        val high: Double,
        val open: Double,
        val close: Double,
        val volume: Double) {
     companion object {
         fun getCandles(productId: String, time: Int, onComplete: (List<Candle>) -> Unit) {
             GdaxApi.candles(productId, time = time).executeRequest { result ->
                 when (result) {
                     is Result.Failure -> {
                         println("Error!: ${result.error}")
                     }
                     is Result.Success -> {
                         val gson = Gson()
                         val apiCandles = result.value
                         val candleDoubleList: List<List<Double>> = gson.fromJson(apiCandles, object : TypeToken<List<List<Double>>>() {}.type)
                         var candles = candleDoubleList.map { Candle(it[0], it[1], it[2], it[3], it[4], it[5]) }

                         var now: LocalDateTime = LocalDateTime.now(Clock.systemUTC())
                         var start = now.toEpochSecond(ZoneOffset.UTC) - time - 60

                         candles = candles.filter { it.time >=  start }

                         candles = candles.reversed()
                         onComplete(candles)
                     }
                 }
             }
         }
     }
}

data class ApiAccount(
        val id: String,
        val currency: String,
        val balance: String,
        val holds: String,
        val available: String,
        val profile_id: String)