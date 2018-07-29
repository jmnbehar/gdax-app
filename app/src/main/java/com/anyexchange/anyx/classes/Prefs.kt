package com.anyexchange.anyx.classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by josephbehar on 12/28/17.
 */

private const val FILE_NAME = "com.anyexchange.gdax.prefs"  //do not rename
private const val PASSPHRASE = "passphrase"
private const val API_KEY = "api_key"
private const val API_SECRET = "api_secret"
private const val SAVE_API_INFO = "save_api_info"
private const val SAVE_PASSPHRASE = "save_passphrase"
private const val ALERTS = "alerts"
private const val AUTOLOGIN = "should_autologin"
private const val SHOW_TRADE_CONFIRM = "show_trade_confirm"
private const val SHOW_SEND_CONFIRM = "show_send_confirm"
private const val STASHED_PRODUCTS = "stashed_products"
private const val STASHED_ORDERS = "stashed_orders"
private const val STASHED_FILLS = "stashed_fills"
private const val DARK_MODE = "dark_mode"
private const val IS_FIRST_TIME = "is_first_time"
private const val IS_LOGGED_IN = "is_logged_in"
private const val UNPAID_FEES = "unpaid_fees_"
private const val APPROVED_API_KEYS = "approved_api_keys"
private const val REJECTED_API_KEYS = "rejected_api_keys"
private const val RAPID_PRICE_MOVES = "rapid_price_movement"
private const val PREFERRED_FIAT = "preferred_fiat"


private const val PRODUCT = "account_product_"
private const val ACCOUNT = "account_raw_"

class Prefs (var context: Context) {


    private val prefs: SharedPreferences = context.getSharedPreferences(FILE_NAME, 0)

    var isFirstTime: Boolean
        get() = prefs.getBoolean(IS_FIRST_TIME, true)
        set(value) = prefs.edit().putBoolean(IS_FIRST_TIME, value).apply()

    var passphrase: String?
        get() = prefs.getString(PASSPHRASE, null)
        set(value) = prefs.edit().putString(PASSPHRASE, value).apply()

    var apiKey: String?
        get() = prefs.getString(API_KEY, null)
        set(value) = prefs.edit().putString(API_KEY, value).apply()

    var apiSecret: String?
        get() = prefs.getString(API_SECRET, null)
        set(value) = prefs.edit().putString(API_SECRET, value).apply()

    var shouldAutologin: Boolean
        get() = prefs.getBoolean(AUTOLOGIN, true)
        set(value) = prefs.edit().putBoolean(AUTOLOGIN, value).apply()

    var shouldShowTradeConfirmModal: Boolean
        get() = prefs.getBoolean(SHOW_TRADE_CONFIRM, true)
        set(value) = prefs.edit().putBoolean(SHOW_TRADE_CONFIRM, value).apply()

    var shouldShowSendConfirmModal: Boolean
        get() = prefs.getBoolean(SHOW_SEND_CONFIRM, true)
        set(value) = prefs.edit().putBoolean(SHOW_SEND_CONFIRM, value).apply()

    var isDarkModeOn: Boolean
        get() = prefs.getBoolean(DARK_MODE, true)
        set(value) = prefs.edit().putBoolean(DARK_MODE, value).apply()

    var shouldSaveApiInfo: Boolean
        get() = prefs.getBoolean(SAVE_API_INFO, true)
        set(value) {
            prefs.edit().putBoolean(SAVE_API_INFO, value).apply()
            if (!value) {
                shouldSavePassphrase = false
            }
        }

    var isLoggedIn: Boolean
        get() = prefs.getBoolean(IS_LOGGED_IN, false)
        set(value) = prefs.edit().putBoolean(IS_LOGGED_IN, value).apply()

    var shouldSavePassphrase: Boolean
        get() = prefs.getBoolean(SAVE_PASSPHRASE, true)
        set(value) = prefs.edit().putBoolean(SAVE_PASSPHRASE, value).apply()

    var alerts: Set<Alert>
        get() = prefs.getStringSet(ALERTS, setOf<String>()).map { s -> Alert.fromString(s) }.toSet()
        set(value) = prefs.edit().putStringSet(ALERTS, value.map { a -> a.toString() }.toSet()).apply()

    var stashedProducts: List<Product>
        get() = prefs.getStringSet(STASHED_PRODUCTS, setOf<String>()).map { s -> Product.fromString(s) }
        set(value) = prefs.edit().putStringSet(STASHED_PRODUCTS, value.map { a -> a.toString() }.toSet()).apply()

    fun setRapidMovementAlerts(currency: Currency, isActive: Boolean) {
        val tempRapidMovementAlerts = rapidMovementAlerts.toMutableSet()
        if (!isActive && rapidMovementAlerts.contains(currency)) {
            tempRapidMovementAlerts.remove(currency)
        } else if (isActive && !rapidMovementAlerts.contains(currency)) {
            tempRapidMovementAlerts.add(currency)
        }
        rapidMovementAlerts = tempRapidMovementAlerts
    }

    var stashedFiatAccount: Account?
        get() {
            val gson = Gson()
            val fiatString = prefs.getString(PREFERRED_FIAT, Currency.USD.toString())
            val accountString = prefs.getString(ACCOUNT + fiatString, "")
            if (accountString.isNotBlank()) {
                val apiAccount = gson.fromJson(accountString, ApiAccount::class.java)
                val fiatCurrency = Currency.forString(fiatString) ?: Currency.USD
                val product = Product.fiatProduct(fiatCurrency)
                return Account(product, apiAccount)
            }
            return null
        }
        set(value) {
            val gson = Gson()
            if (value != null) {
                val accountJson = gson.toJson(value.apiAccount) ?: ""
                prefs.edit().putString(ACCOUNT + value.currency.toString(), accountJson).apply()
            } else {
                //for each fiat currency:
                prefs.edit().putString(ACCOUNT + Currency.USD.toString(), null).apply()
                prefs.edit().putString(ACCOUNT + Currency.EUR.toString(), null).apply()

            }
        }


    var stashedAccountList: MutableList<Account>
        get() {
            val gson = Gson()
            val newAccountList = mutableListOf<Account>()
            for (currency in Currency.cryptoList) {
                val accountString = prefs.getString(ACCOUNT + currency.toString(), "")
                val productString = prefs.getString(PRODUCT + currency.toString(), "")
                if (accountString.isNotBlank() && productString.isNotBlank()) {
                    val apiAccount = gson.fromJson(accountString, ApiAccount::class.java)
                    var product: Product = try {
                        gson.fromJson(productString, Product::class.java)
                    } catch (e: Exception) {
                        gson.fromJson(productString, SimpleFiatProduct::class.java).toProduct()
                    }
                    val newAccount = Account(product, apiAccount)
                    newAccountList.add(newAccount)
                }
            }
            return newAccountList
        }
        set(value) {
            val gson = Gson()
            for (account in value) {
                val accountJson = gson.toJson(account.apiAccount) ?: ""
                val productJson = gson.toJson(account.product) ?: ""
                prefs.edit().putString(ACCOUNT + account.currency.toString(), accountJson)
                            .putString(PRODUCT + account.currency.toString(), productJson).apply()
            }

        }

    var rapidMovementAlerts: Set<Currency>
        get() = prefs.getStringSet(RAPID_PRICE_MOVES, setOf<String>()).mapNotNull { string -> Currency.forString(string) }.toSet()
        set(value) = prefs.edit().putStringSet(RAPID_PRICE_MOVES, value.map { currency -> currency.toString() }.toSet()).apply()

    fun addUnpaidFee(unpaidFee: Double, currency: Currency): Double {
        /* Keeps track of unpaid fees, returns true if unpaid fees total over the minimum fee.
         * If total unpaid fees are over the minimum send amount, send only the minimum send
         * amount to keep things consistant and then reset unpaid fees to 0.0
         */
        var totalUnpaidFees = prefs.getFloat(UNPAID_FEES + currency.toString(), 0.0f)
        totalUnpaidFees += unpaidFee.toFloat()
        prefs.edit().putFloat(UNPAID_FEES + currency.toString(), totalUnpaidFees).apply()
        return totalUnpaidFees.toDouble()
    }

    fun wipeUnpaidFees(currency: Currency) {
        prefs.edit().putFloat(UNPAID_FEES + currency.toString(), 0.0f).apply()
    }

    fun stashOrders(orderListString: String?) {
        prefs.edit().putString(STASHED_ORDERS, orderListString).apply()
    }
    fun getStashedOrders(productId: String) : List<ApiOrder> {
        val apiOrdersJson = prefs.getString(STASHED_ORDERS, null)
        return if (apiOrdersJson != null) {
            val apiOrderList: List<ApiOrder> = Gson().fromJson(apiOrdersJson, object : TypeToken<List<ApiOrder>>() {}.type)
            apiOrderList.filter { it.product_id == productId }
        } else {
            listOf()
        }
    }

    fun stashFills(fillListJson: String?) {
        prefs.edit().putString(STASHED_FILLS, fillListJson).apply()
    }
    fun getStashedFills(productId: String) : List<ApiFill> {
        val fillListJson = prefs.getString(STASHED_FILLS, null)
        return if (fillListJson != null) {
            val apiFillList: List<ApiFill> = Gson().fromJson(fillListJson, object : TypeToken<List<ApiFill>>() {}.type)
            apiFillList.filter { it.product_id == productId }
        } else {
            listOf()
        }
    }

    fun isApiKeyValid(apiKey: String) : Boolean? {
        val approvedApiKeys = prefs.getStringSet(APPROVED_API_KEYS, setOf<String>()).toMutableSet()
        val rejectedApiKeys = prefs.getStringSet(REJECTED_API_KEYS, setOf<String>()).toMutableSet()
        if (approvedApiKeys.contains(apiKey)) {
            return true
        } else if (rejectedApiKeys.contains(apiKey)) {
            return false
        } else {
            return null //testResult ?: false
        }
    }
    fun approveApiKey(apiKey: String) {
        val apiKeys = prefs.getStringSet(APPROVED_API_KEYS, setOf<String>()).toMutableSet()
        apiKeys.add(apiKey)
        prefs.edit().putStringSet(APPROVED_API_KEYS, apiKeys).apply()
        if (CBProApi.credentials?.apiKey == apiKey) {
            CBProApi.credentials?.isValidated = true
        }
    }
    fun rejectApiKey(apiKey: String) {
        val apiKeys = prefs.getStringSet(REJECTED_API_KEYS, setOf<String>()).toMutableSet()
        apiKeys.add(apiKey)
        prefs.edit().putStringSet(REJECTED_API_KEYS, apiKeys).apply()
        if (CBProApi.credentials?.apiKey == apiKey) {
            CBProApi.credentials?.isValidated = false
        }
    }

    fun addAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.add(alert)
        alerts = tempAlerts.toSet()
    }

    fun removeAlert(alert: Alert) {
        val tempAlerts = alerts.toMutableSet()
        tempAlerts.removeAlert(alert)
        alerts = tempAlerts.toSet()
    }
}

