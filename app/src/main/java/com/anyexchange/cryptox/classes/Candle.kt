package com.anyexchange.cryptox.classes

import java.util.*

data class Candle(
        val openTime: Long,
        val closeTime: Long,
        val low: Double,
        val high: Double,
        val open: Double,
        val close: Double,
        val volume: Double,
        val quoteAssetVolume: Double? = null,
        val tradeCount: Long? = null,
        val takerBuyBaseAssetVolume: Double? = null,
        val takerBuyQuoteAssetVolume: Double? = null)  {

//    val tradingPair: TradingPair? = null

     companion object {
         fun granularityForTimespan(timespan: Timespan) : Long {
             return when (timespan) {
                 Timespan.HOUR -> TimeInSeconds.oneMinute
                 Timespan.DAY -> TimeInSeconds.fiveMinutes
                 Timespan.WEEK -> TimeInSeconds.oneHour
                 Timespan.MONTH -> TimeInSeconds.sixHours
                 Timespan.YEAR -> TimeInSeconds.oneDay
//                 Timespan.ALL -> TimeInSeconds.oneDay
//                 TimeInSeconds.halfHour -> TimeInSeconds.oneMinute
//                 TimeInSeconds.sixHours -> TimeInSeconds.fiveMinutes
//                 TimeInSeconds.twoWeeks -> TimeInSeconds.oneHour
             }
         }
     }
}

fun MutableList<Candle>.addingCandles(newList: List<Candle>) : MutableList<Candle> {
    val previouslyEmpty = this.isEmpty()
    this.addAll(0, newList)
    //TODO: assess whether or not distinctby is a big waste of time
    return if (previouslyEmpty) {
        this
    } else {
        val distinctList = this.distinctBy { it.closeTime }
        distinctList.toMutableList()
    }
}

fun MutableList<Candle>.sortCandles() : MutableList<Candle> {
    return this.asSequence().sortedWith(compareBy({ it.closeTime }, { it.close })).toMutableList()
}

fun List<Candle>.filledInBlanks(granularity: Long) : List<Candle> {
    val tempCandles = this.toMutableList()
    var addedCandles = 0
    for ((index, candle) in this.withIndex()) {
        val missingTimeToNextCandle = if (index < size - 2) {
            //fill in blanks in between candles
            this[index + 1].closeTime - candle.closeTime - granularity
        } else if (index == size - 1){
            //flatline at end
            val now = Date().timeInSeconds()
            val nextCandleTime = candle.closeTime + granularity
            now - nextCandleTime
        } else {
            //leave the 2nd to last candle alone
            0
        }
        if (missingTimeToNextCandle > 0) {
            val missingCandles = (missingTimeToNextCandle / granularity).toInt()
            for (i in 1..missingCandles) {
                addedCandles++
                val closeTime = candle.closeTime + (granularity * i)
                val openTime = candle.openTime + (granularity * i)
                val fillCandle = Candle(openTime, closeTime, candle.close, candle.close, candle.close, candle.close, 0.0)
                tempCandles.add(index + addedCandles, fillCandle)
            }
        }
    }
    return tempCandles
}

fun List<Candle>.compositeCandles(targetCandleCount: Int) : List<Candle> {
    val compositeCandles = mutableListOf<Candle>()
    val compositeFactor: Int = (size / targetCandleCount) - 1
    var i = 0
    var low = 0.0
    var high = 0.0
    var open = 0.0
    var volume = 0.0
    var openTime = 0L
    for ((index, candle) in this.withIndex()) {
        if (i == 0) {
            openTime = candle.openTime
            low = candle.low
            high = candle.high
            open = candle.open
            volume = candle.volume
        } else {
            if (candle.low < low) {
                low = candle.low
            }
            if (candle.high > high) {
                high = candle.high
            }
            volume += candle.volume
        }
        if (i >= compositeFactor || index == size - 1) {
            compositeCandles.add(Candle(openTime, candle.closeTime, low, high, open, candle.close, volume))
            i = -1
        }
        i++
    }
    return compositeCandles
}