package com.anyexchange.anyx.classes

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.support.v4.content.ContextCompat
import com.anyexchange.anyx.R

/**
 * Created by anyexchange on 1/19/2018.
 */

enum class KnownCurrency {
    BTC,
    BCH,
    ETH,
    ETC,
    LTC,
    USD,
    EUR,
    GBP;

    override fun toString() : String {
        return when (this) {
            BTC -> "BTC"
            BCH -> "BCH"
            ETH -> "ETH"
            ETC -> "ETC"
            LTC -> "LTC"
            USD -> "USD"
            EUR -> "EUR"
            GBP -> "GBP"
        }
    }

    val symbol : String
        get() {
            return when (this) {
                BTC -> "BTC"
                BCH -> "BCH"
                ETH -> "ETH"
                ETC -> "ETC"
                LTC -> "LTC"
                USD -> "$"
                EUR -> "€"
                GBP -> "£"
            }
        }

    val fullName : String
        get() = when (this) {
            BTC -> "Bitcoin"
            BCH -> "Bitcoin Cash"
            ETH -> "Ethereum"
            ETC -> "Ether Classic"
            LTC -> "Litecoin"
            USD -> "US Dollar"
            EUR -> "Euro"
            GBP -> "Pound sterling"
        }

    val minSendAmount : Double
        get() = when (this) {
            BTC -> .0001
            ETH -> .001
            ETC -> .001
            BCH -> .001
            LTC -> .1
            else -> 0.0
        }

    val iconId
        get() = when(this) {
            BTC -> R.drawable.icon_btc
            ETH -> R.drawable.icon_eth
            ETC -> R.drawable.icon_etc
            LTC -> R.drawable.icon_ltc
            BCH -> R.drawable.icon_bch
            USD -> R.drawable.icon_usd
            EUR -> R.drawable.icon_eur
            GBP -> R.drawable.icon_gbp
        }

    val isFiat : Boolean
        get() = when(this) {
            USD, EUR, GBP -> true
            else -> false
        }

//    private val startDate : Date
//        get() = when (this) {
//            BTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).closeTime
//            ETH -> GregorianCalendar(2015, Calendar.AUGUST, 6).closeTime
//            BCH -> GregorianCalendar(2017, Calendar.JULY, 1).closeTime
//            LTC -> GregorianCalendar(2013, Calendar.JANUARY, 1).closeTime
//            else -> GregorianCalendar(2013, Calendar.JANUARY, 1).closeTime
//        }
//
//    val lifetimeInSeconds : Long
//        get() {
//            val utcTimeZone = TimeZone.getTimeZone("UTC")
//            val now = Calendar.getInstance(utcTimeZone)
//            val nowTime = now.timeInSeconds()
//            val startTime = startDate.closeTime / 1000
//            return (nowTime - startTime)
//        }

    fun colorPrimary(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_dk)
                BCH -> ContextCompat.getColor(context, R.color.bch_dk)
                ETH -> ContextCompat.getColor(context, R.color.eth_dk)
                LTC -> ContextCompat.getColor(context, R.color.ltc_dk)
                ETC -> ContextCompat.getColor(context, R.color.etc_dk)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_light)
                BCH -> ContextCompat.getColor(context, R.color.bch_light)
                ETH -> ContextCompat.getColor(context, R.color.eth_light)
                LTC -> ContextCompat.getColor(context, R.color.ltc_light)
                ETC -> ContextCompat.getColor(context, R.color.etc_light)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorFade(context: Context): Int {
        return when (this) {
            BTC -> ContextCompat.getColor(context, R.color.btc_fade)
            BCH -> ContextCompat.getColor(context, R.color.bch_fade)
            ETH -> ContextCompat.getColor(context, R.color.eth_fade)
            LTC -> ContextCompat.getColor(context, R.color.ltc_fade)
            ETC -> ContextCompat.getColor(context, R.color.etc_fade)
            USD,
            EUR,
            GBP -> ContextCompat.getColor(context, R.color.white_fade)
        }
    }

    fun colorAccent(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                ETC -> ContextCompat.getColor(context, R.color.etc_accent)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.white)
            }
        } else {
            when (this) {
                BTC -> ContextCompat.getColor(context, R.color.btc_accent)
                BCH -> ContextCompat.getColor(context, R.color.bch_accent)
                ETH -> ContextCompat.getColor(context, R.color.eth_accent)
                LTC -> ContextCompat.getColor(context, R.color.ltc_accent)
                ETC -> ContextCompat.getColor(context, R.color.etc_accent)
                USD,
                EUR,
                GBP -> ContextCompat.getColor(context, R.color.black)
            }
        }
    }

    fun colorStateList(context: Context) : ColorStateList {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_dark, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_dark, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_dark, context.resources.newTheme())
                ETC -> context.resources.getColorStateList(R.color.etc_color_state_list_dark, context.resources.newTheme())
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_dark, context.resources.newTheme())
            }
        } else {
            when (this) {
                BTC -> context.resources.getColorStateList(R.color.btc_color_state_list_light, context.resources.newTheme())
                ETH -> context.resources.getColorStateList(R.color.eth_color_state_list_light, context.resources.newTheme())
                ETC -> context.resources.getColorStateList(R.color.etc_color_state_list_dark, context.resources.newTheme())
                BCH -> context.resources.getColorStateList(R.color.bch_color_state_list_light, context.resources.newTheme())
                LTC -> context.resources.getColorStateList(R.color.ltc_color_state_list_light, context.resources.newTheme())
                USD,
                EUR,
                GBP -> context.resources.getColorStateList(R.color.usd_color_state_list_light, context.resources.newTheme())
            }
        }
    }

    fun buttonTextColor(context: Context) : Int {
        val prefs = Prefs(context)
        return if (prefs.isDarkModeOn) {
            when (this) {
                BTC -> Color.BLACK
                ETH -> Color.WHITE
                ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.BLACK
                USD,
                EUR,
                GBP -> Color.WHITE
            }
        } else {
            when (this) {
                BTC -> Color.WHITE
                ETH -> Color.WHITE
                ETC -> Color.WHITE
                BCH -> Color.WHITE
                LTC -> Color.WHITE
                USD,
                EUR,
                GBP -> Color.BLACK
            }
        }
    }

    val developerAddress : String
        get() = when (this) {
        //CBPro Wallets (AnyX)
            BTC -> "3K63fgura9ctK3Wh6ofwyrTgCb4RrwWci6"
            ETH -> "0x6CDD817fdDAb3Ee5324e0Bb51b0f49f9d0Fd1247"
            ETC -> "0x6e459139E65B4589e3F91c86D11143dBBA4570cf"
            BCH -> "qztzaeg4axteayx7qngcdt2h72n2lw3asq50s50av8"
            LTC -> "MGnywyDCyBxGo58xnAeSS8RPLhpbenpuSD"
            USD,
            EUR,
            GBP -> "my irl address?"
        }


    val orderValue : Int
        get() {
            return when (this) {
                BTC -> 5
                ETH -> 4
                LTC -> 3
                BCH -> 2
                ETC -> 1
                EUR -> -1
                GBP -> -2
                USD -> -100
            }
        }

    companion object {
        val cryptoList = KnownCurrency.values().filter { !it.isFiat }

        fun forString(string: String?) : KnownCurrency? {
            return when (string) {
                "BTC" -> BTC
                "BCH" -> BCH
                "ETH" -> ETH
                "ETC" -> ETC
                "LTC" -> LTC
                "USD" -> USD
                "EUR" -> EUR
                "GBP" -> GBP
                else -> null
            }
        }
    }
}