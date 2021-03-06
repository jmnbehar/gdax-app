package com.anyexchange.cryptox.classes

import com.anyexchange.cryptox.api.BinanceSymbol
import com.anyexchange.cryptox.api.CBProProduct

class TradingPair(val exchange: Exchange, val baseCurrency: Currency, val quoteCurrency: Currency, val id: String?, val orderTypes: List<TradeType>) {
    constructor(product: CBProProduct) : this(Exchange.CBPro, Currency(product.base_currency), Currency(product.quote_currency), product.id, listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))
    constructor(product: BinanceSymbol) : this(Exchange.Binance, Currency(product.baseAsset), Currency(product.quoteAsset), product.symbol, listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))
    constructor(exchange: Exchange, baseCurrency: Currency, quoteCurrency: Currency) : this(exchange, baseCurrency, quoteCurrency, "$baseCurrency-$quoteCurrency", listOf(TradeType.LIMIT, TradeType.MARKET, TradeType.STOP))


    companion object {
        fun tradingPairFromId(exchange: Exchange, id: String) : TradingPair? {
            val validTickerLengths = listOf(3, 2, 4, 5, 6)
            for (length in validTickerLengths) {
                if (id.length > length) {
                    val currencyString = id.substring(0, length)
                    Product.map[currencyString]?.let { product ->
                        val tradingPair = product.tradingPairs.find { it.idForExchange(exchange) == id }
                        if (tradingPair != null) {
                            return tradingPair
                        }
                    }
                }
            }
            return null
        }
    }
    fun idForExchange(exchange: Exchange) : String {
        return when (exchange) {
            Exchange.CBPro -> baseCurrency.toString() + "-" + quoteCurrency.toString()
            Exchange.Binance -> baseCurrency.toString() + quoteCurrency.toString()
        }
    }

    override fun toString(): String {
        return idForExchange(Exchange.CBPro)
    }

    override fun equals(other: Any?): Boolean {
        return if (other is TradingPair) {
            (other.baseCurrency == this.baseCurrency && other.quoteCurrency == this.quoteCurrency && other.exchange == this.exchange)
        } else {
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }
}