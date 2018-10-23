package com.anyexchange.anyx.classes.api

import android.content.Context
import android.os.Handler
import com.anyexchange.anyx.R
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.classes.Currency
import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.core.Method
import com.github.kittinunf.fuel.util.Base64
import com.github.kittinunf.fuel.util.FuelRouting
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Created by anyexchange on 12/18/2017.
 */

@Suppress("ClassName")
sealed class BinanceApi(initData: ApiInitData?) : FuelRouting {
    val context = initData?.context
    val returnToLogin = initData?.returnToLogin ?:  { }

    val binanceExchange = Exchange.Binance

    class ApiCredentials(val apiKey: String, val apiSecret: String, val apiPassPhrase: String, var isVerified: Boolean?)

    companion object {
        var credentials: ApiCredentials? = null

        const val basePath = "https://api.binance.com"

        fun defaultPostFailure(context: Context?, result: Result.Failure<ByteArray, FuelError>) : String {
            val errorCode = ErrorCode.withCode(result.error.response.statusCode)
            return if (context!= null) {
                when (errorCode) {
                    ErrorCode.BadRequest -> context.resources.getString(R.string.error_400)
                    ErrorCode.Unauthorized -> context.resources.getString(R.string.error_401)
                    ErrorCode.Forbidden -> context.resources.getString(R.string.error_403)
                    ErrorCode.NotFound -> context.resources.getString(R.string.error_404)
                    ErrorCode.TooManyRequests -> context.resources.getString(R.string.error_too_many)
                    ErrorCode.ServerError -> context.resources.getString(R.string.error_server)
                    ErrorCode.UnknownError -> context.resources.getString(R.string.error_generic_message, result.errorMessage)
                    else -> ""
                }
            } else {
                ""
            }
        }
    }

    override val basePath = Companion.basePath
    private var timeLock = 0

    fun executeRequest(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onSuccess: (result: Result.Success<String, FuelError>) -> Unit) {
        FuelManager.instance.basePath = basePath
        Fuel.request(this).responseString { _, _, result ->
            when (result) {
                is Result.Failure -> {
                    when (result.error.response.statusCode) {
                        ErrorCode.TooManyRequests.code -> {
                            timeLock++
                            val handler = Handler()
                            var retry = Runnable { }
                            retry = Runnable {
                                timeLock--
                                if (timeLock <= 0) {
                                    timeLock = 0
                                    executeRequest(onFailure, onSuccess)
                                } else {
                                    handler.postDelayed(retry, 200.toLong())
                                }
                            }
                            handler.postDelayed(retry, 1000.toLong())
                        }
                        ErrorCode.Unauthorized.code, ErrorCode.BadRequest.code -> {
                            when (result.errorMessage) {
                                ErrorMessage.InvalidApiKey.toString(), ErrorMessage.InvalidApiKey.toString() -> {
                                    credentials = null
                                    if (context != null) {
                                        Prefs(context).isLoggedIn = false
                                    }
                                    returnToLogin()
                                }
                            }
                            onFailure(result)
                        }
                        else -> onFailure(result)
                    }
                }
                is Result.Success -> {
                    onSuccess(result)
                }
            }
        }
    }

    fun executePost(onFailure: (result: Result.Failure<ByteArray, FuelError>) -> Unit, onSuccess: (result: Result<ByteArray, FuelError>) -> Unit) {
        FuelManager.instance.basePath = basePath
        Fuel.post(this.request.url.toString())
                .header(headers)
                .body(body)
                .response  { _, _, result ->
                    when (result) {
                        is Result.Failure -> onFailure(result)
                        is Result.Success -> onSuccess(result)
                    }
                }
    }

    class candles(initData: ApiInitData?, val tradingPair: TradingPair, val interval: String, var startTime: Long? = null, var endTime: Long? = null, var limit: Int? = null) : BinanceApi(initData) {
        //limit default = 500, max is 1000
        fun getCandles(onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Candle>) -> Unit) {
            var pagesReceived = 0

            val timespan = if (endTime != null && startTime != null) {
                endTime!! - startTime!!
            } else {
                0
            }

            var allCandles = mutableListOf<Candle>()

            this.executeRequest(onFailure) { result ->
                pagesReceived ++
                val gson = Gson()
                val apiCandles = result.value
                try {
                    val candleDoubleList: List<List<Any>> = gson.fromJson(apiCandles, object : TypeToken<List<List<Double>>>() {}.type)
                    var candles = candleDoubleList.mapNotNull {
                        val openTime = (it[0] as? Long)
                        val open = (it[1] as? Double) ?: 0.0
                        val high = (it[2] as? Double) ?: 0.0
                        val low = (it[3] as? Double) ?: 0.0
                        val close = (it[4] as? Double)
                        val volume = (it[5] as? Double) ?: 0.0
                        val closeTime = (it[6] as? Long)
                        val quoteAssetVolume = (it[7] as? Double)
                        val tradeCount = (it[8] as? Long)
                        val takerBuyBaseAssetVolume = (it[9] as? Double)
                        val takerBuyQuoteAssetVolume = (it[10] as? Double)
                        if (close != null && openTime != null && closeTime != null) {
                            Candle(openTime, closeTime, low, high, open, close, volume, quoteAssetVolume, tradeCount, takerBuyBaseAssetVolume, takerBuyQuoteAssetVolume)
                        } else { null }
                    }
                    val now = Calendar.getInstance()

                    val start = now.timeInSeconds() - timespan - 30

                    candles = candles.filter { it.closeTime >= start }

                    //TODO: edit chart library so it doesn't show below 0
                    candles = candles.reversed()
                    allCandles = allCandles.addingCandles(candles)
                    onComplete(allCandles.sortCandles())
                } catch (exception: Exception) {
                    onFailure(Result.Failure(FuelError(exception)))
                }
            }
        }
    }

    class accounts(private val initData: ApiInitData?) : BinanceApi(initData) {

        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<BinanceBalance>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val apiAccountList: List<BinanceBalance> = Gson().fromJson(result.value, object : TypeToken<List<BinanceBalance>>() {}.type)
                    onComplete(apiAccountList)
                } catch (e: Exception) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }

        fun getAllAccountInfo(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            //TODO: fix this
        }

        private fun getAccountsWithProductList(productList: List<Product>, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {

        }

        fun updateAllAccounts(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
//            this.get(onFailure) { apiAccountList ->
//                for (account in Account.cryptoAccounts.plus(Account.fiatAccounts)) {
//                    val apiAccount = apiAccountList.find { a -> a.asset == account.currency.toString() }
//                    apiAccount?.let {
//                        account.updateWithApiAccount(it)
//                    }
//                }
//                onComplete()
//            }
        }

    }

    class account(initData: ApiInitData?, val accountId: String) : BinanceApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (CBProAccount?) -> Unit) {
            this.executeRequest(onFailure) { result ->
                val gson = Gson()
                try {
                    val apiAccount: CBProAccount = gson.fromJson(result.value, object : TypeToken<CBProAccount>() {}.type)
                    onComplete(apiAccount)
                } catch (e: JsonSyntaxException) {
                    try {
                        val apiAccountList: List<CBProAccount> = gson.fromJson(result.value, object : TypeToken<List<CBProAccount>>() {}.type)
                        val apiAccountFirst = apiAccountList.firstOrNull()
                        if (apiAccountFirst != null) {
                            onComplete(apiAccountList.firstOrNull())
                        } else {
                            onFailure(Result.Failure(FuelError(Exception())))
                        }
                    } catch (e: JsonSyntaxException) {
                        onFailure(Result.Failure(FuelError(Exception())))
                    }
                }
            }
        }
    }

    class accountHistory(initData: ApiInitData?, val accountId: String) : BinanceApi(initData)
    class products(initData: ApiInitData?) : BinanceApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<BinanceSymbol>) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val productList: List<BinanceSymbol> = Gson().fromJson(result.value, object : TypeToken<List<BinanceSymbol>>() {}.type)
                    onComplete(productList)
                } catch (e: JsonSyntaxException) {
                    onComplete(listOf())
//                    onFailure(Result.Failure(FuelError(e)))
                } catch (e: IllegalStateException) {
                    onComplete(listOf())
//                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }
    class ticker(initData: ApiInitData?, val tradingPair: TradingPair) : BinanceApi(initData) {
        fun get(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (BinanceTicker) -> Unit) {
            this.executeRequest(onFailure) { result ->
                try {
                    val ticker: BinanceTicker = Gson().fromJson(result.value, object : TypeToken<BinanceTicker>() {}.type)
                    val product = Product.map[tradingPair.baseCurrency.id]
                    product?.setPriceForTradingPair(ticker.price, tradingPair)
                    onComplete(ticker)
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(e)))
                } catch (e: IllegalStateException) {
                    onFailure(Result.Failure(FuelError(e)))
                }
            }
        }
    }
    class orderLimit(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val timeInForce: TimeInForce?, val quantity: Double, val price: Double, val icebergQty: Double? = null) : BinanceApi(initData)
    class orderMarket(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val quantity: Double, val price: Double) : BinanceApi(initData)
    class orderStop(initData: ApiInitData?, val productId: String, val tradeSide: TradeSide, val timeInForce: TimeInForce?, val quantity: Double, val stopPrice: Double) : BinanceApi(initData)

    class listOrders(initData: ApiInitData?, val currency: Currency?) : BinanceApi(initData) {
        fun getAndStash(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Order>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                try {
                    val apiOrderList: List<BinanceOrder> = Gson().fromJson(result.value, object : TypeToken<List<BinanceOrder>>() {}.type)
                    val generalOrderList = apiOrderList.map { Order(it) }

                    if (context != null) {
                        Prefs(context).stashOrders(generalOrderList, Exchange.Binance)
                    }
                    if (currency != null) {
                        val filteredOrderList = generalOrderList.filter { it.tradingPair.baseCurrency == currency }
                        onComplete(filteredOrderList)
                    } else {
                        onComplete(generalOrderList)
                    }
                } catch (e: JsonSyntaxException) {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }
    }
    class getOrder(initData: ApiInitData?, val productId: String, val orderId: Long) : BinanceApi(initData)
    class cancelOrder(initData: ApiInitData?, val productId: String, val orderId: Long) : BinanceApi(initData)

    class fills(initData: ApiInitData?, val tradingPair: TradingPair, val startTime: Long?, val endTime: Long?, val fromTradeId: Long?, val limit: Int? = null) : BinanceApi(initData) {
        fun getAndStash(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: (List<Fill>) -> Unit) {
            this.executeRequest(onFailure) {result ->
                context?.let { context ->
                    try {
                        val prefs = Prefs(context)
                        val apiFillList: List<Fill> = Gson().fromJson(result.value, object : TypeToken<List<Fill>>() {}.type)
                        if (prefs.areFillAlertsActive) {
                            checkForFillAlerts(apiFillList, tradingPair)
                        }
                        prefs.stashFills(apiFillList, tradingPair, binanceExchange)
                        onComplete(apiFillList)
                    } catch (e: JsonSyntaxException) {
                        onFailure(Result.Failure(FuelError(e)))
                    }
                } ?: run {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        }
        private fun checkForFillAlerts(apiFillList: List<Fill>, tradingPair: TradingPair) {
            context?.let {
                val stashedFills = Prefs(context).getStashedFills(tradingPair, binanceExchange)

                if (apiFillList.size > stashedFills.size) {
                    val stashedFillsDate = stashedFills.firstOrNull()?.time?.time ?: 0L
                    for (fill in apiFillList) {
                        val fillDateTime = fill.time.time
                        if (fillDateTime > stashedFillsDate && fillDateTime + TimeInMillis.sixHours > Date().time) {
                            AlertHub.triggerFillAlert(fill, context)
                        } else if (fillDateTime <= stashedFillsDate) {
                            break
                        }
                    }
                }
            }
        }
    }
    //add position?
    class depositAddress(initData: ApiInitData?, val currency: Currency, val status: Boolean? = null) : BinanceApi(initData)

    class sendCrypto(initData: ApiInitData?, val currency: Currency, val destinationAddress: String, val destAddressTag: String?, val amount: Double, val name: String? = null) : BinanceApi(initData)
    class depositHistory(initData: ApiInitData?, val currency: Currency?, val startTime: Long? = null, val endTime: Long? = null) : BinanceApi(initData)
    class withdrawHistory(initData: ApiInitData?, val currency: Currency?, val startTime: Long? = null, val endTime: Long? = null) : BinanceApi(initData)


    class ping(initData: ApiInitData?) : BinanceApi(initData)
    class time(initData: ApiInitData?) : BinanceApi(initData)

    class exchangeInfo(initData: ApiInitData?) : BinanceApi(initData)
    class orderBookDepth(initData: ApiInitData?, val productId: String, val limit: Int? = null) : BinanceApi(initData)
    class recentTrades(initData: ApiInitData?, val productId: String, val limit: Int?) : BinanceApi(initData)
    class historicalTrades(initData: ApiInitData?, val productId: String, val limit: Int?, val fromTradeId: Long? = null) : BinanceApi(initData)
    class aggregatedTrades(initData: ApiInitData?, val productId: String, val fromTradeId: Long?, val startTime: Long? = null, val endTime: Long? = null, val limit: Long? = null) : BinanceApi(initData)
    class dayChangeStats(initData: ApiInitData?, val productId: String) : BinanceApi(initData)
    class bookTicker(initData: ApiInitData?, val productId: String?) : BinanceApi(initData)
    class allOrders(initData: ApiInitData?, val productId: String) : BinanceApi(initData)


    //More WAPI endpoints to add if needed:
    //AccountStatus
    //SystemStatus
    //DustLog
    //TradeFee
    //AssetDetail
    //


    override val method: Method
        get() {
            return when (this) {
                is account -> Method.GET
                is accountHistory -> Method.GET
                is products -> Method.GET

                is cancelOrder -> Method.DELETE
                is listOrders -> Method.GET
                is getOrder -> Method.GET
                is fills -> Method.GET

                is sendCrypto -> Method.POST
                is depositHistory -> Method.GET
                is withdrawHistory -> Method.GET
                is depositAddress -> Method.GET

                is ticker -> Method.GET
                is ping -> Method.GET
                is time -> Method.GET
                is candles -> Method.GET
                is exchangeInfo -> Method.GET
                is orderBookDepth -> Method.GET
                is recentTrades -> Method.GET
                is historicalTrades -> Method.GET
                is aggregatedTrades -> Method.GET
                is dayChangeStats -> Method.GET
                is bookTicker -> Method.GET
                is allOrders -> Method.GET
                is accounts -> Method.GET
                is orderLimit -> Method.POST
                is orderMarket -> Method.POST
                is orderStop -> Method.POST
            }
        }

    enum class ApiType {
        REST,
        WAPI;
    }
    val apiType: ApiType
        get() {
            return when (this) {
                is sendCrypto -> ApiType.WAPI
                is depositHistory -> ApiType.WAPI
                is withdrawHistory -> ApiType.WAPI
                is depositAddress -> ApiType.WAPI
                else -> ApiType.REST
            }
        }

    override val path: String
        get() {
            var tempPath = when (apiType) {
                ApiType.REST -> "/api/"
                ApiType.WAPI -> "/wapi/"
            }
            tempPath += when (this) {
                is accountHistory -> "/accounts/$accountId/ledger"
                is products -> "/products"

                is sendCrypto -> "v3/withdraw.html"
                is depositHistory -> "v3/depositHistory.html"
                is withdrawHistory -> "v3/withdrawHistory.html"
                is depositAddress -> "v3/depositAddress.html"

                is ping -> "v1/ping"
                is time -> "v1/closeTime"

                is candles -> "v1/klines"
                is exchangeInfo -> "v1/exchangeInfo"
                is orderBookDepth -> "v1/depth"
                is recentTrades -> "v1/trades"
                is historicalTrades -> "v1/historicalTrades"
                is aggregatedTrades -> "v1/aggTrades"
                is dayChangeStats -> "v3/ticker/24hr"
                is ticker -> "v3/ticker/price"
                is bookTicker -> "v3/ticker/bookTicker"

                //TODO: take off the test
                is orderLimit -> "v3/order/test"
                is orderMarket -> "v3/order/test"
                is orderStop -> "v3/order/test"
                is getOrder -> "v3/order"
                is cancelOrder -> "v3/order"

                is listOrders -> "v3/openOrders"
                is accounts -> "v3/account"
                is fills -> "v3/myTrades"

                is account -> "/accounts/$accountId"
                is allOrders -> "v3/allOrders"


            }
            return tempPath
        }

    override val params: List<Pair<String, Any?>>?
        get() {
            val paramList = mutableListOf<Pair<String, String>>()
            when (this) {
                is orderBookDepth -> {
                    paramList.add(Pair("symbol", productId))
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    return paramList.toList()
                }
                is recentTrades -> {
                    paramList.add(Pair("symbol", productId))
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    return paramList.toList()
                }
                is historicalTrades -> {
                    paramList.add(Pair("symbol", productId))
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    if (fromTradeId != null) {
                        paramList.add(Pair("fromId", fromTradeId.toString()))
                    }
                    return paramList.toList()
                }
                is aggregatedTrades -> {
                    paramList.add(Pair("symbol", productId))
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    if (fromTradeId != null) {
                        paramList.add(Pair("fromId", fromTradeId.toString()))
                    }
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    return paramList.toList()
                }
                is candles -> {
                    paramList.add(Pair("symbol", tradingPair.idForExchange(binanceExchange)))
                    paramList.add(Pair("interval", interval))
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))
                    }
                    return paramList.toList()
                }
                is ticker -> {
                    return listOf(Pair("symbol", tradingPair.idForExchange(binanceExchange)))
                }
                is bookTicker -> {
                    if (productId != null) {
                        paramList.add(Pair("symbol", productId))
                    }
                    return paramList.toList()
                }
                is dayChangeStats -> {
                    return listOf(Pair("symbol", productId))
                }
                is getOrder -> {

                    paramList.add(Pair("symbol", productId))
                    paramList.add(Pair("orderId", orderId.toString()))

                    paramList.add(Pair("timestamp", Date().time.toString()))
                    return paramList.toList()
                }
                is cancelOrder -> {
                    paramList.add(Pair("symbol", productId))
                    paramList.add(Pair("orderId", orderId.toString()))

                    paramList.add(Pair("timestamp", Date().time.toString()))
                    return paramList.toList()
                }
                is listOrders -> {
//                    if (tradingPair != null) {
//                        paramList.add(Pair("symbol", tradingPair.idForExchange(binanceExchange)))
//                    }
                    paramList.add(Pair("timestamp", Date().time.toString()))
                    return paramList.toList()

                }
                is allOrders -> {
                    paramList.add(Pair("symbol", productId))

                    paramList.add(Pair("timestamp", Date().time.toString()))
                    return paramList.toList()
                }
                is accounts -> {
                    paramList.add(Pair("timestamp", Date().time.toString()))
                    return paramList.toList()
                }


                is fills -> {
                    paramList.add(Pair("symbol", tradingPair.idForExchange(binanceExchange)))
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    if (fromTradeId != null) {
                        paramList.add(Pair("fromId", fromTradeId.toString()))

                    }
                    if (limit != null) {
                        paramList.add(Pair("limit", limit.toString()))

                    }
                    paramList.add(Pair("timestamp", Date().time.toString()))

                    return paramList.toList()
                }

                is depositHistory -> {
                    if (currency != null) {
                        paramList.add(Pair("asset", currency.toString()))
                    }
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    paramList.add(Pair("timestamp", Date().time.toString()))

                    return paramList.toList()
                }
                is withdrawHistory -> {
                    if (currency != null) {
                        paramList.add(Pair("asset", currency.toString()))
                    }
                    if (startTime != null) {
                        paramList.add(Pair("startTime", startTime.toString()))
                    }
                    if (endTime != null) {
                        paramList.add(Pair("endTime", endTime.toString()))
                    }
                    paramList.add(Pair("timestamp", Date().time.toString()))

                    return paramList.toList()
                }
                is depositAddress -> {
                    paramList.add(Pair("asset", currency.toString()))

                    if (status != null) {
                        paramList.add(Pair("startTime", status.toString()))
                    }
                    paramList.add(Pair("timestamp", Date().time.toString()))

                    return paramList.toList()
                }
                else -> return null
            }
        }
    private val fullPath: String
        get() {
            params?.let { params ->
                return if (params.isEmpty()) {
                    path
                } else {
                    var fullPath = "$path?"
                    for ((index, param) in params.withIndex()) {
                        if (index > 0) {
                            fullPath += "&"
                        }
                        fullPath += "${param.first}=${param.second}"
                    }
                    fullPath
                }
            } ?: run {
                return path
            }
        }

    private fun basicOrderParams(tradeSide: TradeSide, tradeType: TradeType, productId: String, quantity: Double): JSONObject {
        val json = JSONObject()
        json.put("symbol", productId)
        json.put("side", tradeSide.toString())
        json.put("type", tradeType.toString())
        json.put("quantity", quantity.toString())

        json.put("timestamp", Date().time.toString())
        return json
    }

    private val body: String
        get() {
            when (this) {
                is orderLimit -> {
                    val json = basicOrderParams(tradeSide, TradeType.LIMIT, productId, quantity)


                    json.put("price", price.toString())
                    json.put("timeInForce", timeInForce.toString())
                    json.put("price", "$price")
                    json.put("timestamp", Date().time.toString())


                    return json.toString()
                }
                is orderMarket -> {
                    //can add either size or funds, for now lets do funds
                    val json = basicOrderParams(tradeSide, TradeType.MARKET, productId, quantity)
                    return json.toString()
                }
                is orderStop -> {
                    //can add either size or funds, for now lets do funds
                    val json = basicOrderParams(tradeSide, TradeType.STOP, productId, quantity)

                    json.put("stopPrice", stopPrice.toString())
                    return json.toString()
                }
                is sendCrypto -> {
                    val json = JSONObject()
                    json.put("asset", currency.toString())
                    json.put("address", destinationAddress)
                    if (destAddressTag != null) {
                        json.put("addressTag", destAddressTag)
                    }
                    json.put("amount", amount)

                    if (name != null) {
                        json.put("name", name)
                    }

                    return json.toString()
                }
                else -> return ""
            }
        }

    override val headers: Map<String, String>?
        get() {
            var headers: MutableMap<String, String> = mutableMapOf()
            val credentials = credentials
            if (credentials != null) {
                val timestamp = (Date().timeInSeconds()).toString()
                val message = if (this is fills || this is listOrders) {
                    timestamp + method + fullPath + body
                } else {
                    timestamp + method + path + body
                }
                var hash = ""
                try {
                    val secretDecoded: ByteArray? = Base64.decode(credentials.apiSecret, 0)

                    val sha256HMAC = Mac.getInstance("HmacSHA256")
                    val secretKey = SecretKeySpec(secretDecoded, "HmacSHA256")
                    sha256HMAC.init(secretKey)
                    hash = Base64.encodeToString(sha256HMAC.doFinal(message.toByteArray()), 0)

                } catch (e: Exception) {
                    println("API Secret Hashing Error")
                }

                headers = mutableMapOf(Pair("CB-ACCESS-KEY", credentials.apiKey), Pair("CB-ACCESS-PASSPHRASE", credentials.apiPassPhrase), Pair("CB-ACCESS-SIGN", hash), Pair("CB-ACCESS-TIMESTAMP", timestamp))

            }

            if (method == Method.POST) {
                headers["Content-Type"] = "application/json"
            }
            return headers
        }


    enum class TimeInForce {
        GoodTilCancelled,
        GoodTilTime,
        ImmediateOrCancel,
        FirstOrKill;

        override fun toString(): String {
            return when (this) {
                GoodTilCancelled -> "GTC"
                GoodTilTime -> "GTT"
                ImmediateOrCancel -> "IOC"
                FirstOrKill -> "FOK"
            }
        }
        companion object {
            fun forString(string: String?) : TimeInForce? {
                if (string == null) {
                    return null
                }
                for (value in TimeInForce.values()) {
                    if (value.toString() == string) {
                        return value
                    }
                }
                return null
            }
        }
        fun label(): String {
            return when (this) {
                GoodTilCancelled -> "Good Til Cancelled"
                GoodTilTime -> "Good Til Time"
                ImmediateOrCancel -> "Immediate Or Cancel"
                FirstOrKill -> "First Or Kill"
            }
        }
    }

    enum class ErrorMessage {
        //Log in:
        Forbidden,
        InvalidApiKey,
        InvalidPassphrase,
        InvalidApiSignature,
        MissingApiSignature,
        MissingApiKey,

        //Permissions:

        //Trade:
        BuyAmountTooSmallBtc,
        BuyAmountTooSmallEth,
        BuyAmountTooSmallBch,
        BuyAmountTooSmallLtc,

        BuyAmountTooLargeBtc,
        BuyAmountTooLargeEth,
        BuyAmountTooLargeBch,
        BuyAmountTooLargeLtc,

        PriceTooAccurate,
        InsufficientFunds,

        //Transfer:
        TransferAmountTooLow,
        InvalidCryptoAddress;

        override fun toString(): String {
            //TODO: refactor
            return when (this) {
                Forbidden -> "Forbidden"
                InvalidApiKey -> "Invalid API Key"
                InvalidPassphrase -> "Invalid Passphrase"
                InvalidApiSignature -> "invalid signature"
                MissingApiSignature -> "CB-ACCESS-SIGN header is required"
                MissingApiKey -> "CB-ACCESS-KEY header is required"

                BuyAmountTooSmallBtc -> "size is too small. Minimum size is 0.001"
                BuyAmountTooSmallEth -> "size is too small. Minimum size is 0.01"
                BuyAmountTooSmallBch -> "size is too small. Minimum size is 0.01"
                BuyAmountTooSmallLtc -> "size is too small. Minimum size is 0.1"
                BuyAmountTooLargeBtc -> "size is too large. Maximum size is 70"
                BuyAmountTooLargeEth -> "size is too large. Maximum size is 700"
                BuyAmountTooLargeBch -> "size is too large. Maximum size is 350"
                BuyAmountTooLargeLtc -> "size is too large. Maximum size is 4000"

                PriceTooAccurate -> "price is too accurate. Smallest unit is 0.01000000"
                InsufficientFunds -> "Insufficient funds"

                TransferAmountTooLow -> "amount must be a positive number"
                InvalidCryptoAddress -> "invalid crypto_address"
            }
        }

        companion object {
            fun forString(string: String) : ErrorMessage? {
                val errorMessage = ErrorMessage.values().find { errorMessage -> errorMessage.toString() == string }
                if (errorMessage == null) {
                    if (string.contains("is below the minimum", true) && string.contains("required to send on-blockchain.", true)) {
                        return TransferAmountTooLow
                    }
                }
                return errorMessage
            }
        }
    }

}