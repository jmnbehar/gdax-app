package com.anyexchange.cryptox.adapters

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.anyexchange.cryptox.classes.*
import com.anyexchange.cryptox.R
import kotlinx.android.synthetic.main.list_row_chart_balance.view.*

/**
 * Created by anyexchange on 11/12/2017.
 */

class ChartBalanceListViewAdapter(val context: Context, var accounts: List<Account>, var quoteCurrency: Currency) : BaseAdapter() {

    override fun getCount(): Int {
        return if (accounts.any { it.balance > 0 }) {
            accounts.size + 1
        } else {
            accounts.size
        }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }

    internal class ViewHolder {
        var iconView: ImageView? = null
        var balanceText: TextView? = null
        var exchangeText: TextView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_chart_balance)

            viewHolder.iconView = vi.img_balance_exchange_icon
            viewHolder.balanceText = vi.txt_balance
            viewHolder.exchangeText = vi.txt_balance_exchange

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        if(i < accounts.size) {
            val account = accounts[i]
            viewHolder.balanceText?.text =  account.balance.format(account.currency)
            viewHolder.exchangeText?.text = context.getString(R.string.chart_balance_account_balance, account.exchange.name)
            account.exchange.iconId.let {
                viewHolder.iconView?.visibility = View.GONE
                viewHolder.iconView?.setImageResource(it)
            } ?: run {
                viewHolder.iconView?.visibility = View.GONE
            }
        } else {
            val currency = accounts.first().currency
            val totalBalance = accounts.map { it.balance }.sum()
            val price = Product.map[currency.id]?.priceForQuoteCurrency(quoteCurrency) ?: 1.0
            val totalValue = totalBalance * price

            viewHolder.iconView?.visibility = View.GONE
            viewHolder.exchangeText?.text = if (i > 1) {
                context.getString(R.string.chart_balance_total_value)
            } else {
                context.getString(R.string.chart_balance_single_value)
            }
            viewHolder.balanceText?.text =  totalValue.format(quoteCurrency)
        }

        return outputView
    }
}