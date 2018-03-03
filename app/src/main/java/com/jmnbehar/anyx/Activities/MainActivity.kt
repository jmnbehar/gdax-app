package com.jmnbehar.anyx.Activities

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.graphics.ColorFilter
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.support.design.widget.NavigationView
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.Spinner
import com.github.kittinunf.fuel.core.FuelError
import com.github.kittinunf.result.Result
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jmnbehar.anyx.Adapters.AlertListViewAdapter
import com.jmnbehar.anyx.Classes.*
import com.jmnbehar.anyx.Fragments.Main.*
import com.jmnbehar.anyx.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import org.jetbrains.anko.indeterminateProgressDialog
import org.jetbrains.anko.toast
import se.simbio.encryption.Encryption


class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var notificationManager: NotificationManager? = null
    lateinit var spinnerNav: Spinner
    var defaultSpinnerColorFilter: ColorFilter? = null

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
        HOME,
        OTHER;


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
                HOME -> "HOME"
                OTHER -> "OTHER"
            }
        }

    }

    companion object {
        var currentFragment: RefreshFragment? = null

        var btcChartFragment: ChartFragment? = null
        var ethChartFragment: ChartFragment? = null
        var ltcChartFragment: ChartFragment? = null
        var bchChartFragment: ChartFragment? = null

        var accountsFragment: AccountsFragment? = null

        var sendFragment: SendFragment? = null

        var alertsFragment: AlertsFragment? = null

        var settingsFragment: SettingsFragment? = null

        var marketFragment: MarketFragment? = null

        var progressDialog: ProgressDialog? = null

        fun newIntent(context: Context): Intent {
            return Intent(context, MainActivity::class.java)
        }


    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)
        val nullMessage: CharSequence? = null
        progressDialog = indeterminateProgressDialog(nullMessage)
        progressDialog?.dismiss()

        spinnerNav = toolbar_spinner
        defaultSpinnerColorFilter = spinnerNav.background.colorFilter
        spinnerNav.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                val selectedItem = parent.getItemAtPosition(position) as String ?: ""
                val selectedCurrency = Currency.fromString(selectedItem)
                goToChartFragment(selectedCurrency)
            }
            override fun onNothingSelected(parent: AdapterView<*>) { }
        }
        //TODO: show spinner later
        spinnerNav.visibility = View.GONE

        val prefs = Prefs(this)

        if (Account.list.size > 0) {
            goHome()
        } else if (!prefs.shouldAutologin) {
            returnToLogin()
        } else {
            signIn()
        }
    }

//    override fun onResume() {
//        super.onResume()
//    }

   // private fun goHome(onFailure: (result: Result.Failure<String, FuelError>) -> Unit) {
   private fun goHome() {
        loopThroughAlerts()
        runAlerts()
        goToFragment(FragmentType.HOME)
    }


    private fun signIn() {
        val prefs = Prefs(this)

        val apiKey = prefs.apiKey
        val apiSecret = prefs.apiSecret
        val passphraseEncrypted  = prefs.passphrase

        val iv = ByteArray(16)
        val encryption = Encryption.getDefault(apiKey, apiSecret + Constants.salt, iv)
        val passphrase = encryption.decryptOrNull(passphraseEncrypted)
        if ((apiKey != null) && (apiSecret != null) && (passphrase != null)) {
            val isApiKeyValid = prefs.isApiKeyValid(apiKey)
            GdaxApi.credentials = GdaxApi.ApiCredentials(apiKey, apiSecret, passphrase, isApiKeyValid)
            progressDialog?.show()
            GdaxApi.accounts().getAllAccountInfo(this, { _ ->
                toast("Error!")
                progressDialog?.dismiss()
                returnToLogin()
            }, {
                progressDialog?.dismiss()
                goHome()
            })
        } else {
            returnToLogin()
        }
    }


    private fun returnToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(Constants.logout, true)
        startActivity(intent)
        finish()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            //super.onBackPressed()

            if (supportFragmentManager.backStackEntryCount > 1) {
                supportFragmentManager.popBackStack()
                //val fragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 1).name
                val prevFragmentTag = supportFragmentManager.getBackStackEntryAt(supportFragmentManager.backStackEntryCount - 2).name

                currentFragment = supportFragmentManager.findFragmentByTag(prevFragmentTag) as RefreshFragment
            } else {
//                val intent = Intent(this, LoginActivity::class.java)
//                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
//                intent.putExtra(Constants.exit, true)
//                startActivity(intent)
                finishAffinity()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        // menuInflater.inflate(R.menu.main, menu)
        return false
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

    fun updatePrices(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        var tickersUpdated = 0
        val accountListSize = Account.list.size
        for (account in Account.list) {
            GdaxApi.ticker(account.product.id).executeRequest(onFailure) { result ->
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

    private fun runAlerts() {
        val handler = Handler()

        val runnable = Runnable {
            updatePrices( { /* do nothing*/ }, {
                loopThroughAlerts()
                runAlerts()
                if (currentFragment is AlertsFragment) {
                    val prefs = Prefs(this)
                    (currentFragment as AlertsFragment).alertAdapter?.alerts = prefs.alerts.toList()
                    (currentFragment as AlertsFragment).alertAdapter?.notifyDataSetChanged()
                }
            })
        }

        //TODO: add variable time checking
        //TODO: (ideally run on system launch)
        handler.postDelayed(runnable, (TimeInSeconds.halfMinute * 1000))
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
        val CHANNEL_ID = "Price_Alerts"
        if (notificationManager == null) {
            notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create the NotificationChannel, but only on API 26+ because
                // the NotificationChannel class is new and not in the support library
                val name = getString(R.string.channel_name)
                val description = getString(R.string.channel_description)
                val importance = NotificationManagerCompat.IMPORTANCE_DEFAULT
                val channel = NotificationChannel(CHANNEL_ID, name, importance)
                channel.description = description
                // Register the channel with the system
                notificationManager?.createNotificationChannel(channel)
            }
        }


        val overUnder = when(alert.triggerIfAbove) {
            true  -> "over"
            false -> "under"
        }
        val intent = Intent(this, this.javaClass)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationTitle = "${alert.currency.fullName} price alert"
        val notificationText = "${alert.currency} is $overUnder ${alert.price.fiatFormat()}"
        val priceAlertGroupTag = "PriceAlert"

        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.anyx_fg)
                .setContentTitle(notificationTitle)
                .setContentText(notificationText)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setGroup(priceAlertGroupTag)
//                .setSound(defaultSoundUri)

        val notificationTag = "PriceAlert_" + alert.currency.toString() + "_" + alert.price
        notificationManager?.notify(notificationTag, 0, notificationBuilder.build())

        val prefs = Prefs(this)
        prefs.removeAlert(alert)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        val fragmentType = when (item.itemId) {
            R.id.nav_send -> FragmentType.SEND
            R.id.nav_alerts -> FragmentType.ALERTS
            R.id.nav_settings -> FragmentType.SETTINGS
            R.id.nav_home -> FragmentType.HOME
            else -> FragmentType.HOME
        }
        val currentFragmentType = when (currentFragment) {
            is SendFragment -> FragmentType.SEND
            is AlertsFragment -> FragmentType.ALERTS
            is SettingsFragment -> FragmentType.SETTINGS
            is HomeFragment -> FragmentType.HOME
            is ChartFragment -> FragmentType.BTC_CHART  //TODO: refine
            is TradeFragment -> FragmentType.TRADE
            else -> FragmentType.OTHER
        }
        if (fragmentType != currentFragmentType) {
            if ((fragmentType == FragmentType.SEND) && (GdaxApi.credentials?.isValidated != true)) {
                //do nothing
                toast("Feature not yet available - stay tuned!")
            } else {
                goToFragment(fragmentType)
            }
        }
        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }

    fun goToChartFragment(currency: Currency) {
        when (currency) {
            Currency.BTC -> goToFragment(FragmentType.BTC_CHART)
            Currency.BCH -> goToFragment(FragmentType.BCH_CHART)
            Currency.ETH -> goToFragment(FragmentType.ETH_CHART)
            Currency.LTC -> goToFragment(FragmentType.LTC_CHART)
            Currency.USD -> {}
        }
    }
    fun goToFragment(fragmentType: FragmentType) {
        val prefs = Prefs(this)
        val fragment = when (fragmentType) {
            FragmentType.BTC_CHART -> if (btcChartFragment != null ) { btcChartFragment } else {
                //TODO: confirm account is not null
                val account = Account.btcAccount!!
                ChartFragment.newInstance(account)
            }
            FragmentType.BCH_CHART -> if (bchChartFragment != null ) { bchChartFragment } else {
                val account = Account.bchAccount!!
                ChartFragment.newInstance(account)
            }
            FragmentType.ETH_CHART -> if (ethChartFragment != null ) { ethChartFragment } else {
                val account = Account.ethAccount!!
                ChartFragment.newInstance(account)
            }

            FragmentType.LTC_CHART -> if (ltcChartFragment != null ) { ltcChartFragment } else {
                val account = Account.ltcAccount!!
                ChartFragment.newInstance(account)
            }
            FragmentType.ACCOUNT -> if (accountsFragment != null ) { accountsFragment } else {
                AccountsFragment.newInstance()
            }
            FragmentType.SEND -> if (sendFragment != null ) { sendFragment } else {
                //TODO: don't go to send frragment if not logged in
                SendFragment.newInstance()
            }
            FragmentType.ALERTS -> if (alertsFragment != null ) { alertsFragment } else {
                AlertsFragment.newInstance(this)
            }
            FragmentType.SETTINGS -> if (settingsFragment != null ) { settingsFragment } else {
                SettingsFragment.newInstance()
            }
            FragmentType.HOME -> if (marketFragment != null ) {
                marketFragment
            } else {
                //TODO: think about this
                //MarketFragment.newInstance()
                HomeFragment.newInstance()
            }
            FragmentType.TRADE -> {
                println("Do not use this function for tradeFragments")
                null
            }
            FragmentType.OTHER -> null
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
        if (supportFragmentManager.backStackEntryCount == 0) {
//            if (Companion.fragmentManager.fragments.isEmpty()) {
            supportFragmentManager
                    .beginTransaction()
                    .add(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        } else {
            supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment, tag)
                    .addToBackStack(tag)
                    .commitAllowingStateLoss()
        }
    }


}