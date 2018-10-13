package com.anyexchange.anyx.adapters

import android.content.Context
import android.content.res.Resources
import android.support.v4.content.res.ResourcesCompat
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_history.view.*
import org.jetbrains.anko.backgroundColor


/**
 * Created by anyexchange on 11/12/2017.
 */

class HistoryRecyclerViewAdapter(val context: Context, private val isOrderList: Boolean, ordersOrFills: List<Any>, var resources: Resources,
                                 private var orderOnClick: (Order) -> Unit = { }, private var fillOnClick: (Fill) -> Unit = { }) : RecyclerView.Adapter<HistoryRecyclerViewAdapter.HistoryViewHolder>() {
    var orders: List<Order> = listOf()
    var fills: List<Fill> = listOf()

    init {
        @Suppress("UNCHECKED_CAST")
        if (isOrderList) {
            orders = ordersOrFills as List<Order>
        } else {
            fills = ordersOrFills as List<Fill>
        }
    }

    override fun getItemCount(): Int {
        return if (isOrderList) {
            if (orders.isEmpty()) {
                1
            } else {
                orders.size
            }
        } else {
            if (fills.isEmpty()) {
                1
            } else {
                fills.size
            }
        }
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    class HistoryViewHolder(
            var view: View?,
            var colorView: ImageView?,
            var sideText: TextView?,
            var amountText: TextView?,
            var currencyText: TextView?,
            var priceText: TextView?,
            var tradeTypeText: TextView?
        ): RecyclerView.ViewHolder(view)

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(parent: ViewGroup,
                                    viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.list_row_history, parent, false) as View

//        view.layoutParams = LinearLayout.LayoutParams(Constraints.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)

        return HistoryViewHolder(view,
                view.img_history_icon,
                view.txt_history_side,
                view.txt_history_amount,
                view.txt_history_currency,
                view.txt_history_price,
                view.txt_history_trade_type)
    }

    private fun setViewsVisibility(viewHolder: HistoryViewHolder, setVisible: Boolean) {
        if (setVisible) {
            viewHolder.colorView?.visibility = View.VISIBLE
            viewHolder.sideText?.visibility = View.VISIBLE
            viewHolder.priceText?.visibility = View.VISIBLE
            viewHolder.currencyText?.visibility = View.VISIBLE
            viewHolder.tradeTypeText?.visibility = View.VISIBLE
        } else {
            viewHolder.colorView?.visibility = View.INVISIBLE
            viewHolder.sideText?.visibility = View.GONE
            viewHolder.priceText?.visibility = View.GONE
            viewHolder.currencyText?.visibility = View.GONE
            viewHolder.tradeTypeText?.visibility = View.GONE
        }
    }
    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: HistoryViewHolder, position: Int) {

        val tradeSide: TradeSide
        val price: Double
        val amount: Double
        val currency: Currency
        if (isOrderList) {
            if (orders.isEmpty()) {
                setViewsVisibility(viewHolder, false)
                viewHolder.amountText?.text = context.resources.getString(R.string.chart_history_no_orders)
                viewHolder.view?.setOnClickListener { }
                return
            }
            setViewsVisibility(viewHolder, true)
            val order = orders[position]
            tradeSide = order.side
            price = order.price
            val size = order.amount
            val filled = order.filledSize.toDoubleOrZero()
            val unfilledSize = size - filled
            amount = unfilledSize
            currency = order.tradingPair.baseCurrency
            val tradeType = TradeType.forString(order.type)
            viewHolder.sideText?.text = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_order_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_order_side_sell)
            }
            viewHolder.view?.setOnClickListener { orderOnClick(order) }

            viewHolder.tradeTypeText?.visibility = View.VISIBLE
            viewHolder.tradeTypeText?.text = context.resources.getString(R.string.chart_history_trade_type_label, tradeType)
        } else {
            if (fills.isEmpty()) {
                viewHolder.amountText?.text = context.resources.getString(R.string.chart_history_no_fills)
                setViewsVisibility(viewHolder, false)
                viewHolder.view?.setOnClickListener {}
                return
            }
            setViewsVisibility(viewHolder, true)
            viewHolder.tradeTypeText?.visibility = View.GONE
            val fill = fills[position]
            tradeSide = fill.side
            currency = fill.tradingPair.baseCurrency
            price = fill.price
            amount = fill.amount
            viewHolder.view?.setOnClickListener { fillOnClick(fill) }

            viewHolder.sideText?.text = when (tradeSide) {
                TradeSide.BUY -> context.resources.getString(R.string.chart_history_fill_side_buy)
                TradeSide.SELL -> context.resources.getString(R.string.chart_history_fill_side_sell)
            }
        }

        viewHolder.colorView?.backgroundColor = when (tradeSide) {
            TradeSide.BUY -> ResourcesCompat.getColor(resources, R.color.anyx_green, null)
            TradeSide.SELL -> ResourcesCompat.getColor(resources, R.color.anyx_red, null)
        }

        viewHolder.amountText?.text = amount.btcFormat()
        viewHolder.currencyText?.text = context.resources.getString(R.string.chart_history_currency_label, currency)
        viewHolder.priceText?.text = price.fiatFormat(Account.defaultFiatCurrency)
    }
}