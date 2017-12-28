package com.jmnbehar.gdax.Classes

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
        val volume: Double)

data class ApiAccount(
        val id: String,
        val currency: String,
        val balance: String,
        val holds: String,
        val available: String,
        val profile_id: String)