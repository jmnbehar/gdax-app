package com.anyexchange.cryptox.classes

import com.anyexchange.cryptox.api.AnyApi
import com.anyexchange.cryptox.api.ApiInitData
import com.anyexchange.cryptox.api.CBProProduct
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import java.util.*


/**
 * Created by anyexchange on 12/20/2017.
 */

class Product(var currency: Currency, tradingPairsIn: List<TradingPair>) {
    constructor(apiProduct: CBProProduct, tradingPairs: List<TradingPair>)
            : this(Currency(apiProduct.base_currency), tradingPairs)

    init {
        currency.addToList()
    }

    var tradingPairs = tradingPairsIn.sortTradingPairs()
        set(value) {
            field = value.sortTradingPairs()
            if (dayCandles.size < tradingPairs.size) {
                val candlesSizeTemp = dayCandles.size
                val hourCandlesTemp = hourCandles
                val dayCandlesTemp = dayCandles
                val weekCandlesTemp = weekCandles
                val monthCandlesTemp = monthCandles
                val yearCandlesTemp = yearCandles
                val priceTemp = price
                price       = Array(tradingPairCount) { i -> if (i < priceTemp.size)  {priceTemp[i] } else { 0.0 } }
                hourCandles = Array(tradingPairCount) { i -> if (i < candlesSizeTemp) { hourCandlesTemp[i] } else { listOf() } }
                dayCandles  = Array(tradingPairCount) { i -> if (i < candlesSizeTemp) { dayCandlesTemp[i]  } else { listOf() } }
                weekCandles = Array(tradingPairCount) { i -> if (i < candlesSizeTemp) { weekCandlesTemp[i] } else { listOf() } }
                monthCandles = Array(tradingPairCount){ i -> if (i < candlesSizeTemp) { monthCandlesTemp[i]} else { listOf() } }
                yearCandles = Array(tradingPairCount) { i -> if (i < candlesSizeTemp) { yearCandlesTemp[i] } else { listOf() } }
            }
        }
    val defaultTradingPair: TradingPair?
        get() {
            val defaultFiatPair = tradingPairs.find { it.quoteCurrency == Account.defaultFiatCurrency }
            return defaultFiatPair ?: tradingPairs.firstOrNull()
        }

    private val defaultFavorites: List<Currency>
        get() = listOf(Currency.BTC, Currency.ETH, Currency.LTC, Currency.BCH)

    private var isFavoriteBackingBool: Boolean? = null
    var isFavorite: Boolean
        get() {
            return isFavoriteBackingBool ?: (totalBalanceAcrossAllAccounts() > 0 || defaultFavorites.contains(currency))
        }
        set(value) {
            isFavoriteBackingBool = value
        }

    private fun tradingPairIndex(tradingPair: TradingPair?) : Int {
        //null trading pair will simply select the default fiat pair
        val tempIndex = tradingPairs.indexOf(tradingPair)
        return if (tempIndex == -1) { 0 } else { tempIndex }
    }

    private val tradingPairCount: Int
        get() {
            val actualCount = tradingPairs.size
            return if (actualCount > 0) {
                actualCount
            } else {
                1
            }
        }
    private var price = Array(tradingPairCount) { 0.0 }

    val defaultPrice: Double
        get() = priceForQuoteCurrency(Account.defaultFiatCurrency)

    private var hourCandles = Array<List<Candle>>(tradingPairCount) { listOf() }
    private var dayCandles = Array<List<Candle>>(tradingPairCount)  { listOf() }
    private var weekCandles = Array<List<Candle>>(tradingPairCount) { listOf() }
    private var monthCandles = Array<List<Candle>>(tradingPairCount){ listOf() }
    private var yearCandles = Array<List<Candle>>(tradingPairCount) { listOf() }

    val defaultDayCandles: List<Candle>
        get() = candlesForTimespan(Timespan.DAY, defaultTradingPair)

    private var candlesTimespan = Timespan.DAY

    //TODO: this is extremely horribly inefficient, pls fix
    var accounts : Map<Exchange, Account>
        get() = accountsBackingList?.associateBy { it.exchange } ?: emptyMap()
        set(value) {
            accountsBackingList = value.values.toList()
        }
    private var accountsBackingList : List<Account>? = listOf()

    fun percentChange(timespan: Timespan, quoteCurrency: Currency) : Double {
        val currentPrice = priceForQuoteCurrency(quoteCurrency)
        val candles = candlesForTimespan(timespan, quoteCurrency)
        val open = if (candles.isNotEmpty()) {
            candles.first().close
        } else {
            currentPrice
        }
        val change = currentPrice - open

        val weightedChange: Double = (change / open)
        return weightedChange * 100.0
    }

    private fun candlesForTimespan(timespan: Timespan, quoteCurrency: Currency) : List<Candle> {
        val tradingPair = tradingPairs.find { it.quoteCurrency == quoteCurrency }
        return candlesForTimespan(timespan, tradingPair)
    }
    fun candlesForTimespan(timespan: Timespan, tradingPair: TradingPair?): List<Candle> {
        val tradingPairIndex = tradingPairIndex(tradingPair)
        return when (timespan) {
            Timespan.HOUR -> hourCandles[tradingPairIndex]
            Timespan.DAY -> dayCandles[tradingPairIndex]
            Timespan.WEEK -> weekCandles[tradingPairIndex]
            Timespan.MONTH -> monthCandles[tradingPairIndex]
            Timespan.YEAR -> yearCandles[tradingPairIndex]
        }
    }

    fun priceForQuoteCurrency(quoteCurrency: Currency) : Double {
        // First, check for a straight up trading pair for quote currency
        val tradingPair = tradingPairs.find { it.quoteCurrency == quoteCurrency }
        if (tradingPair != null) {
            return priceForTradingPair(tradingPair)
        }
        // If the trading pair is not available, check for a relevant
        val stablecoinTradingPair = tradingPairs.find { it.quoteCurrency == quoteCurrency.relevantStableCoin }
        if (stablecoinTradingPair != null) {
            return priceForTradingPair(stablecoinTradingPair)
        }
        // Next, check against bitcoin
        if (quoteCurrency != Currency.BTC) {
            val btcTradingPair = tradingPairs.find { it.quoteCurrency == Currency.BTC }
            val btcPrice = Product.map[Currency.BTC.id]?.priceForQuoteCurrency(quoteCurrency) ?: 0.0
            if (btcTradingPair != null && btcPrice > 0) {
                val priceInBtc = priceForTradingPair(btcTradingPair)
                return priceInBtc * btcPrice
            }
        }

        // Last resort, check against ether
        if (quoteCurrency != Currency.ETH) {
            val ethTradingPair = tradingPairs.find { it.quoteCurrency == Currency.ETH }
            val ethPrice = Product.map[Currency.ETH.id]?.priceForQuoteCurrency(quoteCurrency) ?: 0.0
            if (ethTradingPair != null && ethPrice > 0) {
                val priceInEth = priceForTradingPair(ethTradingPair)
                return priceInEth * ethPrice
            }
        }

        // If all that fails, return 0 and hang your head in shame
        return 0.0
    }

    private fun priceForTradingPair(tradingPair: TradingPair?) : Double {
        var tradingPairIndex: Int = tradingPairs.indexOf(tradingPair)
        if (tradingPairIndex == -1) { tradingPairIndex = 0 }
        return price.getOrNull(tradingPairIndex) ?: 0.0
    }

    fun setPriceForTradingPair(newPrice: Double, tradingPair: TradingPair) {
        var tradingPairIndex: Int = tradingPairs.indexOf(tradingPair)
        if (tradingPairIndex == -1) { tradingPairIndex = 0 }
        price[tradingPairIndex] = newPrice
    }

    fun updateCandles(timespan: Timespan, tradingPairOpt: TradingPair?, apiInitData: ApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (didUpdate: Boolean) -> Unit) {
        val now = Calendar.getInstance()
        val longAgo = Calendar.getInstance()
        longAgo.add(Calendar.YEAR, -2)
        val longAgoInSeconds = longAgo.timeInSeconds()
        val nowInSeconds = now.timeInSeconds()
        val tradingPair = tradingPairOpt ?: defaultTradingPair
        if (tradingPair == null) {
            onFailure(Result.Failure(FuelError(Exception())))
            return
        }

        var candles = candlesForTimespan(timespan, tradingPairOpt).toMutableList()

        val lastCandleTime = candles.lastOrNull()?.closeTime ?: longAgoInSeconds
        val nextCandleTime: Long = lastCandleTime + Candle.granularityForTimespan(timespan)

        candlesTimespan = timespan

        if (nextCandleTime < nowInSeconds) {
            var missingTime = nowInSeconds - lastCandleTime

            val timespanLong = timespan.value()
            if (missingTime > timespanLong) {
                missingTime = timespanLong
            }

            val granularity = Candle.granularityForTimespan(timespan)
            AnyApi(apiInitData).getCandles(tradingPair.exchange, tradingPair, missingTime, 0, granularity, onFailure) { candleList ->
                var didGetNewCandle = false
                if (candleList.isNotEmpty()) {
                    val newLastCandleTime = candleList.lastOrNull()?.closeTime?.toInt() ?: 0.0
                    didGetNewCandle = (lastCandleTime != newLastCandleTime)
                    if (didGetNewCandle) {
                        val timespanStart = nowInSeconds - timespanLong

                        if (candles.isNotEmpty()) {
                            val firstInTimespan = candles.indexOfFirst { candle -> candle.closeTime >= timespanStart }
                            candles = if (firstInTimespan >= 0) {
                                candles.subList(firstInTimespan, candles.lastIndex).toMutableList()
                            } else {
                                mutableListOf()
                            }
                            candles.addAll(candleList)
                        } else {
                            candles = candleList.toMutableList()
                        }

                        var tradingPairIndex: Int = tradingPairs.indexOf(tradingPair)
                        if (tradingPairIndex == -1) { tradingPairIndex = 0 }
                        when (timespan) {
                            Timespan.HOUR -> hourCandles[tradingPairIndex] = candles
                            Timespan.DAY -> dayCandles[tradingPairIndex] = candles
                            Timespan.WEEK -> weekCandles[tradingPairIndex] = candles
                            Timespan.MONTH -> monthCandles[tradingPairIndex] = candles
                            Timespan.YEAR -> yearCandles[tradingPairIndex] = candles
                        }
                        //TODO: consider whether or not we should do this:
                        price[tradingPairIndex] = candles.last().close
                    }
                }
                onComplete(didGetNewCandle)
            }
        } else {
            onComplete(false)
        }
    }

    override fun toString(): String {
        var alertString = currency.toString() + '\n'
        for (tradingPair in tradingPairs) {
            alertString += tradingPair.toString() + '\n'
        }
        return alertString
    }

    fun totalValueOfRelevantAccounts(quoteCurrency: Currency) : Double {
        val totalBalance = totalBalanceAcrossAllAccounts()
        return totalBalance * priceForQuoteCurrency(quoteCurrency)
    }

    fun totalBalanceAcrossAllAccounts() : Double {
        var totalBalance = 0.0
        for (accountPair in accounts) {
            totalBalance += accountPair.value.balance
        }
        return totalBalance
    }

    fun addToHashMap() {
        map[currency.id] = this
    }

    companion object {
        var map = mutableMapOf<String, Product>()

        fun favorites() : List<Product> {
            return map.values.filter { it.isFavorite }
        }

        val dummyProduct = Product(Currency.USD, listOf())

        fun forCurrency(currency: Currency) : Product? {
            return map[currency.id]
        }

        fun updateImportantCandles(apiInitData: ApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            var candlesUpdated = 0

            var alertCurrencies: Set<Currency> = setOf()
            if (apiInitData != null) {
                val prefs = Prefs(apiInitData.context)
                val basicAlertCurrencies = prefs.alerts.map { it.currency }
                val quickChangeCurrencies = prefs.quickChangeAlertCurrencies.asSequence().map { Currency(it) }.toSet()
                alertCurrencies = quickChangeCurrencies.plus(basicAlertCurrencies)
            }
            val importantProducts = map.values.filter { it.isFavorite || alertCurrencies.contains(it.currency) }
            val count = importantProducts.size

            if (map.isEmpty()) {
                onComplete()
            } else {
                for (product in importantProducts) {
                    val tradingPair = product.defaultTradingPair
                    product.updateCandles(Timespan.DAY, tradingPair, apiInitData, onFailure) { didUpdate ->
                        candlesUpdated++
                        if (candlesUpdated == count) {
                            if (didUpdate && apiInitData?.context != null) {
                                Prefs(apiInitData.context).stashProducts()
                            }
                            onComplete()
                        }
                    }
                }
            }
        }


    }

}
