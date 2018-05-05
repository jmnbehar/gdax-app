package com.anyexchange.anyx.classes

import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Handler
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.view.View
import android.widget.AdapterView
import com.anyexchange.anyx.adapters.NavigationSpinnerAdapter
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.fragment_chart.view.*
import org.jetbrains.anko.backgroundColor
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.onRefresh

/**
 * Created by anyexchange on 1/15/2018.
 */

open class RefreshFragment: Fragment() {
    val handler = Handler()
    var autoRefresh: Runnable? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var skipNextRefresh: Boolean = false

    companion object {
        val ARG_OBJECT = "object"
    }

    override fun onResume() {
        super.onResume()
        if (!skipNextRefresh) {
            refresh {
                endRefresh()
            }
        }
        skipNextRefresh = false
        showDarkMode()
        if (activity is com.anyexchange.anyx.activities.MainActivity) {
            (activity as com.anyexchange.anyx.activities.MainActivity).spinnerNav.background.setColorFilter(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
            (activity as com.anyexchange.anyx.activities.MainActivity).spinnerNav.visibility = View.GONE
            (activity as com.anyexchange.anyx.activities.MainActivity).toolbar.title = "AnyX"
        }
    }

    fun showNavSpinner(defaultSelection: Currency?, onItemSelected: (currency: Currency) -> Unit) {
        val mainActivity = (activity as com.anyexchange.anyx.activities.MainActivity)
        mainActivity.toolbar.title = ""
        mainActivity.spinnerNav.background.colorFilter = mainActivity.defaultSpinnerColorFilter
        mainActivity.spinnerNav.isEnabled = true
        mainActivity.spinnerNav.visibility = View.VISIBLE
        mainActivity.spinnerNav.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                if (parent.getItemAtPosition(position) is Currency) {
                    val selectedItem = parent.getItemAtPosition(position) as Currency
                    onItemSelected(selectedItem)
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        if (defaultSelection != null) {
            val spinnerList = (mainActivity.spinnerNav.adapter as NavigationSpinnerAdapter).currencyList
            val currentIndex = spinnerList.indexOf(defaultSelection)
            mainActivity.spinnerNav.setSelection(currentIndex)
        }
    }

    fun doneLoading() {
        if (activity is com.anyexchange.anyx.activities.MainActivity) {
            (activity as com.anyexchange.anyx.activities.MainActivity).dismissProgressBar()
        }
    }


    fun showPopup(string: String, positiveAction: () -> Unit, negativeText: String? = null, negativeAction: () -> Unit = {}) {
        alert {
            title = string
            positiveButton("OK") { positiveAction() }
            if (negativeText != null) {
                negativeButton(negativeText) { negativeAction }
            }
        }.show()
    }

    fun showDarkMode(newView: View? = null) {
        val backgroundView = newView ?: view
        val activity = activity
        if (activity != null) {
            val prefs = Prefs(activity)
            if (prefs.isDarkModeOn) {
                backgroundView?.backgroundColor = Color.TRANSPARENT
                activity.setTheme(R.style.AppThemeDark)
            } else {
                backgroundView?.backgroundColor = Color.WHITE
                activity.setTheme(R.style.AppThemeLight)
            }
        }
    }


    //TODO: use the other one
    fun setupSwipeRefresh(rootView: View) {
        swipeRefreshLayout = rootView.swipe_refresh_layout
        swipeRefreshLayout?.onRefresh {
            refresh { endRefresh() }
        }
    }

//    fun setupSwipeRefresh(swipeRefreshLayout: SwipeRefreshLayout) {
//        this.swipeRefreshLayout = swipeRefreshLayout
//        this.swipeRefreshLayout?.onRefresh {
//            refresh { endRefresh() }
//        }
//    }


    open fun refresh(onComplete: (Boolean) -> Unit) {   //The boolean indicates whether or not refresh was successful
        skipNextRefresh = false
        onComplete(true)
    }

    fun endRefresh() {
        swipeRefreshLayout?.isRefreshing = false
    }
}