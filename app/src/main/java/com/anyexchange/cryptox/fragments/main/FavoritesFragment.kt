package com.anyexchange.cryptox.fragments.main

import android.arch.lifecycle.LifecycleOwner

class FavoritesFragment : MarketFragment(), LifecycleOwner {

    companion object {
        fun newInstance(): FavoritesFragment
        {
            return FavoritesFragment()
        }
    }

    override val onlyShowFavorites = true


}