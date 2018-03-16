package com.jmnbehar.anyx.Adapters

import android.os.Bundle
import android.support.v4.app.FragmentStatePagerAdapter
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import com.jmnbehar.anyx.Classes.RefreshFragment
import com.jmnbehar.anyx.Fragments.Main.*


/**
 * Created by josephbehar on 2/17/18.
 */

// Since this is an object collection, use a FragmentStatePagerAdapter,
// and NOT a FragmentPagerAdapter.
class WithdrawPagerAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {

    override fun getItem(i: Int): Fragment {
        when (i) {
            0 -> {
                val fragment = WithdrawCoinbaseFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
            1 -> {
                val fragment = DepositBankFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
            else -> {
                //do something here
                val fragment = DepositCoinbaseFragment()
                val args = Bundle()
                args.putInt(RefreshFragment.ARG_OBJECT, i + 1)
                fragment.arguments = args
                return fragment
            }
        }
    }

    override fun getCount(): Int {
        return 2
    }

    override fun getPageTitle(position: Int): CharSequence {
        return when (position) {
            0 -> "Coinbase"
            1 -> "Bank Account"
            else -> "Screen " + (position + 1)
        }
    }
}