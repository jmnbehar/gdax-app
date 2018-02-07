package com.jmnbehar.gdax.Classes

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Created by josephbehar on 12/28/17.
 */

class Prefs (context: Context) {
    private val FILE_NAME = "com.jmnbehar.gdax.prefs"
    private val PASSPHRASE = "passphrase"
    private val API_KEY = "api_key"
    private val API_SECRET = "api_secret"
    private val SAVE_API_INFO = "save_api_info"
    private val SAVE_PASSPHRASE = "save_passphrase"
    private val ALERTS = "alerts"
    private val AUTOLOGIN = "should_autologin"
    private val SHOW_TRADE_CONFIRM = "show_trade_confirm"
    private val SHOW_SEND_CONFIRM = "show_send_confirm"
    private val STASHED_PRODUCTS = "stashed_products"
    private val STASHED_ORDERS = "stashed_orders"
    private val STASHED_FILLS = "stashed_fills"
    private val DARK_MODE = "dark_mode"
    private val IS_FIRST_TIME = "is_first_time"


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
        get() = prefs.getBoolean(DARK_MODE, false)
        set(value) = prefs.edit().putBoolean(DARK_MODE, value).apply()

    var shouldSaveApiInfo: Boolean
        get() = prefs.getBoolean(SAVE_API_INFO, false)
        set(value) {
            prefs.edit().putBoolean(SAVE_API_INFO, value).apply()
            if (!value) {
                shouldSavePassphrase = false
            }
        }

    var shouldSavePassphrase: Boolean
        get() = prefs.getBoolean(SAVE_PASSPHRASE, false)
        set(value) = prefs.edit().putBoolean(SAVE_PASSPHRASE, value).apply()

    var alerts: Set<Alert>
        get() = prefs.getStringSet(ALERTS, setOf<String>()).map { s -> Alert.fromString(s) }.toSet()
        set(value) = prefs.edit().putStringSet(ALERTS, value.map { a -> a.toString() }.toSet()).apply()

    var stashedProducts: List<Product>
        get() = prefs.getStringSet(STASHED_PRODUCTS, setOf<String>()).map { s -> Product.fromString(s) }
        set(value) = prefs.edit().putStringSet(STASHED_PRODUCTS, value.map { a -> a.toString() }.toSet()).apply()

    fun stashOrders(orderListString: String) {
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

    fun stashFills(fillListJson: String) {
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

