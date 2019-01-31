package com.anyexchange.anyx.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.list_row_product.view.*
import android.widget.ImageView
import android.widget.TextView

/**
 * Created by anyexchange on 11/12/2017.
 */

class ProductListViewAdapter(var inflater: LayoutInflater?, var productList: List<Product>, var onClick: (Product) -> Unit, var onLongPress: (View, Product) -> Unit) : BaseAdapter() {
    var size = 20

    init {
        if (productList.size < size) {
            size = productList.size
        }
    }
    companion object {
        const val sizeChangeAmount = 5
    }

    override fun getCount(): Int {
        return size
    }

    fun increaseSize() {
        if (size < productList.size) {
            if ((size + sizeChangeAmount) <= productList.size) {
                size += sizeChangeAmount
            } else {
                size = productList.size
            }
            notifyDataSetChanged()
        }
    }

    override fun getItem(i: Int): Any {
        return i
    }

    override fun getItemId(i: Int): Long {
        return i.toLong()
    }


    internal class ViewHolder {
        var productNameText: TextView? = null
        var priceText: TextView? = null
        var productIcon: ImageView? = null
    }

    override fun getView(i: Int, convertView: View?, viewGroup: ViewGroup): View {
        val viewHolder: ViewHolder?
        val outputView: View
        if (convertView == null) {
            viewHolder = ViewHolder()

            val vi = viewGroup.inflate(R.layout.list_row_product)

            viewHolder.productNameText = vi.txt_product_name
            viewHolder.priceText = vi.txt_product_price
            viewHolder.productIcon = vi.img_product_icon

            vi.tag = viewHolder
            outputView = vi
        } else {
            viewHolder = convertView.tag as ViewHolder
            outputView = convertView
        }

        if (i >= productList.size) {
            return outputView
        }
        val product = productList[i]

        //TODO: someday add ability to select values here
        product.currency.iconId?.let {
            viewHolder.productIcon?.visibility = View.VISIBLE
            viewHolder.productIcon?.setImageResource(it)
        } ?: run {
            viewHolder.productIcon?.visibility = View.INVISIBLE
        }

        viewHolder.productNameText?.text = product.currency.toString()

        val quoteCurrency = product.defaultTradingPair?.quoteCurrency ?: Account.defaultFiatCurrency

        viewHolder.priceText?.visibility = View.VISIBLE

        viewHolder.priceText?.text = product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)

        outputView.setOnLongClickListener {
            onLongPress(it, product)
            notifyDataSetChanged()
            true
        }
        outputView.setOnClickListener { onClick(product) }

        return outputView
    }
}