package com.anyexchange.cryptox.classes

import android.widget.AbsListView


abstract class LazyLoader(private val threshold: Int = DEFAULT_THRESHOLD) : AbsListView.OnScrollListener {

    private var loading = true
    private var previousTotal = 0

    override fun onScrollStateChanged(view: AbsListView, scrollState: Int) {}

    override fun onScroll(view: AbsListView, firstVisibleItem: Int,
                          visibleItemCount: Int, totalItemCount: Int) {
        if (loading) {
            if (totalItemCount > previousTotal) {
                // the loading has finished
                loading = false
                previousTotal = totalItemCount
            }
        }

        // check if the List needs more data
        if (!loading && firstVisibleItem + visibleItemCount >= totalItemCount - threshold) {
            loading = true

            // List needs more data. Go fetch !!
            loadMore(view, firstVisibleItem,
                    visibleItemCount, totalItemCount)
        }
    }

    // Called when the user is nearing the end of the ListView
    // and the ListView is ready to add more items.
    abstract fun loadMore(view: AbsListView, firstVisibleItem: Int,
                          visibleItemCount: Int, totalItemCount: Int)

    companion object {
        private const val DEFAULT_THRESHOLD = 5
    }
}