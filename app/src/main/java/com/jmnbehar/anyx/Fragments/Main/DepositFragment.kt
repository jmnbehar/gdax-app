package com.jmnbehar.anyx.Fragments.Main

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jmnbehar.anyx.Adapters.DepositPagerAdapter
import com.jmnbehar.anyx.Adapters.HomePagerAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.fragment_home.view.*

/**
 * Created by jmnbehar on 11/5/2017.
 */
class DepositFragment : RefreshFragment() {

    lateinit var inflater: LayoutInflater
    var collectionPagerAdapter: DepositPagerAdapter? = null

    companion object {
        fun newInstance(): DepositFragment
        {
            return DepositFragment()
        }
        var viewPager: LockableViewPager? = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_home, container, false)

        val prefs = Prefs(context!!)
        val tabLayout = rootView.home_tab_layout

        if (prefs.isDarkModeOn) {
            tabLayout.setTabTextColors(Color.LTGRAY, Color.WHITE)
        } else {
            tabLayout.setTabTextColors(Color.DKGRAY, Color.BLACK)
        }

        viewPager = rootView.home_view_pager

        collectionPagerAdapter = DepositPagerAdapter(childFragmentManager)
        viewPager?.adapter = collectionPagerAdapter

        return rootView
    }


    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }
}