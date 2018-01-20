package com.jmnbehar.gdax.Activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentManager
import android.support.v4.app.NotificationCompat
import android.support.v4.view.GravityCompat
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.gdax.Classes.*
import com.jmnbehar.gdax.Fragments.*
import com.jmnbehar.gdax.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.jetbrains.anko.support.v4.onRefresh
import org.jetbrains.anko.toast


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null

    enum class FragmentType {
        BTC_CHART,
        BCH_CHART,
        ETH_CHART,
        LTC_CHART,
        ACCOUNT,
        SEND,
        ALERTS,
        SETTINGS,
        TRADE,
        PRICES;


        override fun toString() : String {
            return when (this) {
                BTC_CHART -> "BTC"
                BCH_CHART -> "BCH"
                ETH_CHART -> "ETH"
                LTC_CHART -> "LTC"
                ACCOUNT -> "ACCOUNT"
                SEND -> "SEND"
                ALERTS -> "ALERTS"
                SETTINGS -> "SETTINGS"
                TRADE -> "TRADE"
                PRICES -> "PRICES"
            }
        }

    }

    companion object {
        var currentFragment: RefreshFragment? = null
        lateinit var apiProductList: List<ApiProduct>
        lateinit private var fragmentManager: FragmentManager
        lateinit var swipeRefreshLayout: SwipeRefreshLayout

        var btcChartFragment: ChartFragment? = null
        var ethChartFragment: ChartFragment? = null
        var ltcChartFragment: ChartFragment? = null
        var bchChartFragment: ChartFragment? = null

        var accountsFragment: AccountsFragment? = null

        var sendFragment: SendFragment? = null

        var alertsFragment: AlertsFragment? = null

        var settingsFragment: RedFragment? = null

        var pricesFragment: PricesFragment? = null

        fun newIntent(context: Context, result: String): Intent {
            val intent = Intent(context, MainActivity::class.java)

            val gson = Gson()

            val unfilteredApiProductList: List<ApiProduct> = gson.fromJson(result, object : TypeToken<List<ApiProduct>>() {}.type)
            apiProductList = unfilteredApiProductList.filter {
                s -> s.quote_currency == "USD"// && s.base_currency != "BCH"
            }
//            apiProductList = gson.fromJson(result, object : TypeToken<List<ApiProduct>>() {}.type)

            return intent
        }

        fun setSupportFragmentManager(fragmentManager: FragmentManager) {
            this.fragmentManager = fragmentManager
        }


        fun goToNavigationId(navigationId: Int, context: Context) {
            when (navigationId) {
                R.id.nav_btc -> {
                    val btcAccount = Account.btcAccount
                    if (btcAccount != null) {
                        goToFragment(FragmentType.BTC_CHART)
                    } else {
                        Account.getAccountInfo { goToFragment(FragmentType.BTC_CHART) }
                    }
                }
                R.id.nav_eth -> {
                    val ethAccount = Account.ethAccount
                    if (ethAccount != null) {
                        goToFragment(FragmentType.ETH_CHART)
                    } else {
                        Account.getAccountInfo { goToFragment(FragmentType.ETH_CHART) }
                    }
                }
                R.id.nav_ltc -> {
                    val ltcAccount = Account.ltcAccount
                    if (ltcAccount != null) {
                        goToFragment(FragmentType.LTC_CHART)
                    } else {
                        Account.getAccountInfo { goToFragment(FragmentType.LTC_CHART) }
                    }
                }
                R.id.nav_bch -> {
                    val ltcAccount = Account.bchAccount
                    if (ltcAccount != null) {
                        goToFragment(FragmentType.BCH_CHART)
                    } else {
                        Account.getAccountInfo { goToFragment(FragmentType.BCH_CHART) }
                    }
                }
                R.id.nav_accounts -> {
                    goToFragment(FragmentType.ACCOUNT)
                }
                R.id.nav_send -> {
                    goToFragment(FragmentType.SEND)
                }
                R.id.nav_alerts -> {
                    goToFragment(FragmentType.ALERTS, context = context)
                }
                R.id.nav_settings -> {
                    goToFragment(FragmentType.SETTINGS)
                }
                R.id.nav_home -> {
                    goToFragment(FragmentType.PRICES)
                }
            }
        }

        fun goToFragment(fragmentType: FragmentType, context: Context? = null) {
            val fragment = when (fragmentType) {

                FragmentType.BTC_CHART -> if (btcChartFragment != null ) { btcChartFragment } else {
                    //TODO: confirm account is not null
                    val account = Account.btcAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.BCH_CHART -> if (bchChartFragment != null ) { bchChartFragment } else {
                    //TODO: confirm account is not null
                    //TODO: switch to actual bch at some point
                    val account = Account.bchAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.ETH_CHART -> if (ethChartFragment != null ) { ethChartFragment } else {
                    //TODO: confirm account is not null
                    val account = Account.ethAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.LTC_CHART -> if (ltcChartFragment != null ) { ltcChartFragment } else {
                    //TODO: confirm account is not null
                    val account = Account.ltcAccount!!
                    ChartFragment.newInstance(account)
                }
                FragmentType.ACCOUNT -> if (accountsFragment != null ) { accountsFragment } else {
                    AccountsFragment.newInstance()
                }
                FragmentType.SEND -> if (sendFragment != null ) { sendFragment } else {
                    SendFragment.newInstance()
                }
                FragmentType.ALERTS -> if (alertsFragment != null ) { alertsFragment } else {
                    AlertsFragment.newInstance(context!!)
                }
                FragmentType.SETTINGS -> if (settingsFragment != null ) { settingsFragment } else {
                    RedFragment.newInstance()
                }
                FragmentType.PRICES -> if (pricesFragment != null ) { pricesFragment } else {
                    PricesFragment.newInstance()
                }
                FragmentType.TRADE -> {
                    println("Do not use this function for tradeFragments")
                    null
                }
            }
            if (fragment != null) {
                val tag = fragmentType.toString()
                goToFragment(fragment, tag)
            } else {
                println("Error switching fragments")
            }
        }

        fun goToFragment(fragment: RefreshFragment, tag: String) {
            currentFragment = fragment
            if (Companion.fragmentManager.fragments.isEmpty()) {
                Companion.fragmentManager
                        .beginTransaction()
                        .add(R.id.fragment_container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
            } else {
                Companion.fragmentManager
                        .beginTransaction()
                        .replace(R.id.fragment_container, fragment, tag)
                        .addToBackStack(tag)
                        .commit()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
        setSupportFragmentManager(supportFragmentManager)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        createNotificationChannel(Constants.alertChannelId, "Alerts", "Alerts go here")

        swipeRefreshLayout = swipe_refresh_layout
        swipeRefreshLayout.onRefresh {
            if (currentFragment != null) {
                currentFragment?.refresh { endRefresh() }
            } else {
                swipeRefreshLayout.isRefreshing = false
            }
        }

        if (savedInstanceState == null) {
            getCandles {
                loopThroughAlerts()
                runAlarms()
                Account.getAccountInfo { goToFragment(FragmentType.PRICES) }
            }
        }
    }

    fun endRefresh() {
        swipeRefreshLayout.isRefreshing = false
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()

            val fragmentManager = supportFragmentManager
            if (fragmentManager.backStackEntryCount > 0) {
                val fragmentTag = fragmentManager.getBackStackEntryAt(fragmentManager.backStackEntryCount - 1).name
                currentFragment = fragmentManager.findFragmentByTag(fragmentTag) as RefreshFragment
                currentFragment?.refresh { endRefresh() }
            } else {
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                intent.putExtra(Constants.exit, true)
                startActivity(intent)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun getCandles(time: Int = TimeInSeconds.oneDay, onComplete: () -> Unit) {
        if (Product.listSize == 0) {
            for (product in apiProductList) {
                Candle.getCandles(product.id, time, { candleList ->
                    val newProduct = Product(product, candleList)
                    Product.addToList(newProduct)
                    if (Product.listSize == apiProductList.size) {
                        onComplete()
                    }
                })
            }
        }
    }

    fun updatePrices(onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
        for (account in Account.list) {
            GdaxApi.ticker(account.product.id).executeRequest { result ->
                when (result) {
                    is Result.Failure -> {
                        toast("Error!: ${result.error}")
                    }
                    is Result.Success -> {
                        val ticker: ApiTicker = Gson().fromJson(result.value, object : TypeToken<ApiTicker>() {}.type)
                        val price = ticker.price.toDoubleOrNull()
                        if (price != null) {
                            account.product.price = price
                        }
                        tickersUpdated++
                        if (tickersUpdated == accountListSize) {
                            onComplete()
                        }
                    }
                }
            }
        }
    }

    private fun runAlarms() {
        val handler = Handler()

        val runnable = Runnable {
            updatePrices {
                loopThroughAlerts()
                runAlarms()
            }
        }

        //TODO: add variable time checking, and run on launch
        //TODO: (ideally run on system launch)
//        handler.postDelayed(runnable, (TimeInSeconds.fifteenMinutes * 1000).toLong())
        handler.postDelayed(runnable, (TimeInSeconds.oneMinute * 1000).toLong())
    }

    fun loopThroughAlerts() {
        val prefs = Prefs(this)
        val alerts = prefs.alerts
        for (alert in alerts) {
            if (!alert.hasTriggered) {
                var currentPrice = Account.forCurrency(alert.currency)?.product?.price
                if (alert.triggerIfAbove && (currentPrice != null) && (currentPrice >= alert.price)) {
                    triggerAlert(alert)
                } else if (!alert.triggerIfAbove && (currentPrice != null) && (currentPrice <= alert.price)) {
                    triggerAlert(alert)
                }
            }
        }
    }

    private fun triggerAlert(alert: Alert) {
        val prefs = Prefs(this)
        prefs.removeAlert(alert)

        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val notificationString = "${alert.currency.toString()} is $overUnder ${alert.price}"
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)

        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, Constants.alertChannelId)
                .setContentText(notificationString)
                .setAutoCancel(true)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setSound(defaultSoundUri)
                .setContentIntent(pendingIntent)


        notificationManager?.notify(0, notificationBuilder.build())
    }

    private fun createNotificationChannel(id: String, name: String,
                                          description: String) {
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val importance = NotificationManager.IMPORTANCE_LOW
        val channel = NotificationChannel(id, name, importance)

        channel.description = description
        channel.enableLights(true)
        channel.lightColor = Color.RED
        channel.enableVibration(true)
        channel.vibrationPattern =
                longArrayOf(100, 200, 300, 400, 500, 400, 300, 200, 400)
        notificationManager?.createNotificationChannel(channel)
    }


    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        goToNavigationId(item.itemId, this)

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

}
