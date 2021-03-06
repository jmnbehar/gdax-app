package com.anyexchange.cryptox.fragments.main

import android.arch.lifecycle.LifecycleOwner
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.AdapterView
import android.widget.ListView
import android.widget.PopupMenu
import com.anyexchange.cryptox.adapters.ProductListViewAdapter
import com.anyexchange.cryptox.classes.*
import com.anyexchange.cryptox.R
import com.anyexchange.cryptox.activities.MainActivity
import com.anyexchange.cryptox.api.AnyApi
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.fragment_market.view.*

/**
 * Created by anyexchange on 11/5/2017.
 */
open class MarketFragment : RefreshFragment(), LifecycleOwner {
    private var listView: ListView? = null
    lateinit var inflater: LayoutInflater
    open val onlyShowFavorites = false

    companion object {
        var resetHomeListeners = { }
    }

    private val productList: List<Product>
        get() {
            return if (onlyShowFavorites) {
                val context = context
                if (context != null && Prefs(context).sortFavoritesAlphabetical) {
                    Product.map.values.filter { it.isFavorite }.toList().sortProductsAlphabetical()
                } else {
                    Product.map.values.filter { it.isFavorite }.toList().sortProducts()
                }
            } else {
                Product.map.values.toList().sortProductsAlphabetical()
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_market, container, false)
        listView = rootView.list_products
        this.inflater = inflater

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        listView?.adapter = ProductListViewAdapter(inflater, productList, onlyShowFavorites)

        listView?.onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ ->
            val product = (listView?.adapter as ProductListViewAdapter).productList[pos]
            (activity as MainActivity).goToChartFragment(product.currency)
        }
        listView?.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, view, pos, _ ->
            val product = (listView?.adapter as ProductListViewAdapter).productList[pos]
            setIsFavorite(view, product)
            (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()
            true
        }
        listView?.setOnScrollListener(object : LazyLoader() {
            override fun loadMore(view: AbsListView, firstVisibleItem: Int, visibleItemCount: Int, totalItemCount: Int) {
                (listView?.adapter as ProductListViewAdapter).increaseSize()
            }
        })
        shouldHideSpinner = false

        dismissProgressSpinner()
        return rootView
    }

    private fun setIsFavorite(view: View, product: Product) {
        context?.let {
            val popup = PopupMenu(it, view)
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
                    completeRefresh()
                } else {
                    favoritesUpdateListener?.favoritesUpdated()
                }
                true
            }
            popup.show()
        }
    }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        refresh(false, onComplete)
    }
    fun refresh(fullRefresh: Boolean, onComplete: (Boolean) -> Unit) {
        val onFailure: (result: Result.Failure<String, FuelError>) -> Unit = { result ->
            toast("Error: ${result.errorMessage}")
            onComplete(false)
        }
        swipeRefreshLayout?.isRefreshing = true
        if (onlyShowFavorites) {
            var productsUpdated = 0
            val time = Timespan.DAY
            val favoriteProducts = Product.favorites()
            val count = favoriteProducts.count()
            for (product in favoriteProducts) {
                //always check multiple exchanges?
                product.defaultTradingPair?.let { tradingPair ->
                    product.updateCandles(time, tradingPair, apiInitData, {
                        //OnFailure
                    }) {
                        //OnSuccess
                        if (lifecycle.isCreatedOrResumed) {
                            productsUpdated++
                            if (productsUpdated == count) {
                                //update Favorites Tab
                                if (fullRefresh) {
                                    refreshCompleteListener?.refreshComplete()
                                }
                                completeRefresh()
                                onComplete(true)
                            }
                        }
                    }
                } ?: run {
                    onFailure(Result.Failure(FuelError(Exception())))
                }
            }
        } else {
            AnyApi(apiInitData).updateAllTickers(onFailure) {
                //Complete Market Refresh
                if (fullRefresh) {
                    refreshCompleteListener?.refreshComplete()
                }
                favoritesUpdateListener?.favoritesUpdated()
                completeRefresh()
                onComplete(true)
            }
        }
    }

    fun completeRefresh() {
        endRefresh()
        (listView?.adapter as? ProductListViewAdapter)?.productList = productList
//        (listView?.adapter as ProductListViewAdapter).notifyDataSetChanged()

        context?.let {
            Prefs(it).stashProducts()
        }
        (listView?.adapter as? ProductListViewAdapter)?.notifyDataSetChanged()
        listView?.invalidateViews()
        listView?.refreshDrawableState()

//        val run = Runnable {
//            //reload content
//            (listView?.adapter as? ProductListViewAdapter)?.notifyDataSetChanged()
//            listView?.invalidateViews()
//            listView?.refreshDrawableState()
//        }
//        activity?.runOnUiThread(run)
    }

    private var favoritesUpdateListener: FavoritesUpdateListener? = null
    interface FavoritesUpdateListener {
        fun favoritesUpdated()
    }
    fun setFavoritesListener(listener: FavoritesUpdateListener) {
        this.favoritesUpdateListener = listener
    }


    private var refreshCompleteListener: RefreshCompleteListener? = null
    interface RefreshCompleteListener {
        fun refreshComplete()
    }
    fun setRefreshListener(listener: RefreshCompleteListener) {
        this.refreshCompleteListener = listener
    }

    override fun onResume() {
        shouldHideSpinner = false
        super.onResume()
        resetHomeListeners()
        if (onlyShowFavorites) {
            autoRefresh = Runnable {
                if (!skipNextRefresh) {
                    refresh(true) { }
                }
                skipNextRefresh = false

                handler.postDelayed(autoRefresh, TimeInMillis.twoMinutes)
            }
            handler.postDelayed(autoRefresh, TimeInMillis.twoMinutes)
        }
        refresh(false) { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}
