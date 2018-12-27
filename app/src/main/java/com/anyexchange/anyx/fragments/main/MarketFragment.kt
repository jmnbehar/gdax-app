package com.anyexchange.anyx.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ListView
import android.widget.PopupMenu
import com.anyexchange.anyx.adapters.ProductListViewAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import com.anyexchange.anyx.activities.MainActivity
import kotlinx.android.synthetic.main.fragment_market.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
open class MarketFragment : RefreshFragment(), LifecycleOwner {
    private var listView: ListView? = null

    lateinit var inflater: LayoutInflater

    open val onlyShowFavorites = false


    companion object {
        var updateAccountsFragment = { }
        var updateFavoritesFragment = { }

        fun newInstance(): MarketFragment
        {
            return MarketFragment()
        }
    }

    private val productList: List<Product>
        get() {
            return if (onlyShowFavorites) {
                Product.map.values.filter { it.isFavorite }.toList().sortProducts()
            } else {
                Product.map.values.toList().alphabeticalProducts()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_market, container, false)
        listView = rootView.list_products
        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val onClick = lambda@ { product: Product ->
            (activity as MainActivity).goToChartFragment(product.currency)
        }

        listView?.adapter = ProductListViewAdapter(inflater, productList, onlyShowFavorites, onClick) { view, product ->
            setIsFavorite(view, product)
        }
//        listView?.setHeightBasedOnChildren()

        dismissProgressSpinner()
        return rootView
    }

    private fun setIsFavorite(view: View, product: Product) {
        val popup = PopupMenu(activity, view)
        //Inflating the Popup using xml file
        popup.menuInflater.inflate(R.menu.product_popup_menu, popup.menu)
        popup.menu.findItem(R.id.setFavorite).isVisible = !product.isFavorite
        popup.menu.findItem(R.id.removeFavorite).isVisible = product.isFavorite

        popup.setOnMenuItemClickListener { item: MenuItem? ->
            when (item?.itemId) {
                R.id.setFavorite -> {
                    product.isFavorite = true
                }
                R.id.removeFavorite -> {
                    product.isFavorite = false
                }
            }
            if (onlyShowFavorites) {
                (listView?.adapter as ProductListViewAdapter).productList = productList
                (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
            } else {
                updateFavoritesFragment()

            }
            true
        }
        popup.show()
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (onlyShowFavorites) {
            HomeFragment.refresh(1, onComplete)
        } else {
            HomeFragment.refresh(0, onComplete)
        }
    }

    fun completeRefresh() {
        (listView?.adapter as ProductListViewAdapter).productList = productList
        (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
    }
}
