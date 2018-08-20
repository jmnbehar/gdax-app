package com.anyexchange.anyx.classes


import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result

/**
 * Created by anyexchange on 12/20/2017.
 */
class Account(var product: Product, var apiAccount: ApiAccount) {
    val balance: Double
        get() = apiAccount.balance.toDoubleOrZero()

    val availableBalance: Double
        get() {
//        val holds = apiAccount.holds.toDoubleOrZero()
//        return balance - holds
            return apiAccount.available.toDoubleOrZero()
        }

    fun valueForQuoteCurrency(quoteCurrency: Currency) : Double {
        return balance * product.priceForQuoteCurrency(quoteCurrency)
    }
    val defaultValue: Double
        get() = balance * product.defaultPrice

    val id: String
        get() = apiAccount.id

    val currency: Currency
        get() = product.currency

    var coinbaseAccount: CoinbaseAccount? = null

    fun update(apiInitData: CBProApi.CBProApiInitData?, onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        CBProApi.account(apiInitData, id).get(onFailure) { apiAccount ->
            if (apiAccount != null) {
                this.apiAccount = apiAccount
            }
            onComplete()
        }
    }

    companion object {
        var cryptoAccounts = listOf<Account>()

        var fiatAccounts = listOf<Account>()

        val areAccountsOutOfDate: Boolean
            get() {
                val areAccountsMissing = Account.cryptoAccounts.size < Currency.cryptoList.size || Account.fiatAccounts.isEmpty()
                val areAccountsUnidentified = Account.cryptoAccounts.find { it.currency == Currency.USD } != null
                return areAccountsMissing || areAccountsUnidentified
            }

        //TODO: make this changeable
        val defaultFiatAccount: Account?
            get() = fiatAccounts.sortedBy { it.balance }.lastOrNull()

        val defaultFiatCurrency: Currency
            get() = defaultFiatAccount?.currency ?: Currency.USD

        var totalValue: Double = 0.0
            get() = Account.cryptoAccounts.map { a -> a.defaultValue }.sum() + Account.fiatAccounts.map { a -> a.defaultValue }.sum()

        fun forCurrency(currency: Currency): Account? {
            return if (currency.isFiat) {
                fiatAccounts.find { a -> a.product.currency == currency }
            } else {
                cryptoAccounts.find { a -> a.product.currency == currency }
            }
        }

        fun updateAllAccountsCandles(apiInitData: CBProApi.CBProApiInitData?, onFailure: (Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
            var candlesUpdated = 0
            for (account in cryptoAccounts) {
                val tradingPair = TradingPair(account.product.id)
                Product.updateAllProducts(apiInitData, onFailure) {
                    account.product.updateCandles(Timespan.DAY, tradingPair, apiInitData, onFailure) {
                        candlesUpdated++
                        if (candlesUpdated == cryptoAccounts.size) {
                            onComplete()
                        }
                    }
                }
            }
            if (cryptoAccounts.isEmpty()) {
                onComplete()
            }
        }
    }

    abstract class RelatedAccount {
        abstract val id: String
        abstract val balance: Double?
        abstract val currency: Currency
    }

    class CoinbaseAccount(apiCoinbaseAccount: ApiCoinbaseAccount) : RelatedAccount() {
        override val id: String = apiCoinbaseAccount.id
        override val balance: Double = apiCoinbaseAccount.balance.toDoubleOrZero()
        override val currency = Currency.forString(apiCoinbaseAccount.currency) ?: Currency.USD

        override fun toString(): String {
            //TODO: use string resources
            return if (currency.isFiat) {
                "Coinbase $currency Balance: ${balance.fiatFormat(defaultFiatCurrency)}"
            } else {
                "Coinbase $currency Balance: ${balance.btcFormatShortened()} $currency"
            }
        }
    }

    class PaymentMethod(val apiPaymentMethod: ApiPaymentMethod) : RelatedAccount() {
        override val id: String = apiPaymentMethod.id
        override val balance = apiPaymentMethod.balance?.toDoubleOrNull()
        override val currency = Currency.forString(apiPaymentMethod.currency) ?: Currency.USD

        override fun toString(): String {
            return apiPaymentMethod.name
        }
    }
}
