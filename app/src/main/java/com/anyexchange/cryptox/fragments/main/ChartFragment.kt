package com.anyexchange.cryptox.fragments.main

import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleOwner
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.widget.SwipeRefreshLayout
import android.view.*
import com.github.kittinunf.result.Result
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartGestureListener
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import com.anyexchange.cryptox.classes.*
import com.anyexchange.cryptox.R
import kotlinx.android.synthetic.main.fragment_chart.view.*
import com.github.kittinunf.fuel.core.FuelError
import com.github.mikephil.charting.listener.ChartTouchListener
import org.jetbrains.anko.*
import android.view.MotionEvent
import android.widget.*
import com.anyexchange.cryptox.views.LockableScrollView
import com.anyexchange.cryptox.activities.MainActivity
import com.anyexchange.cryptox.adapters.ChartBalanceListViewAdapter
import com.anyexchange.cryptox.adapters.FillListViewAdapter
import com.anyexchange.cryptox.adapters.OrderListViewAdapter
import com.anyexchange.cryptox.adapters.spinnerAdapters.TradingPairSpinnerAdapter
import com.anyexchange.cryptox.api.AnyApi
import com.anyexchange.cryptox.api.CBProApi
import com.anyexchange.cryptox.classes.Currency
import com.github.mikephil.charting.data.CandleEntry
import kotlinx.android.synthetic.main.fragment_chart.*
import java.util.*


/**
 * Created by anyexchange on 11/5/2017.
 */
class ChartFragment : RefreshFragment(), OnChartValueSelectedListener, OnChartGestureListener, LifecycleOwner {
    private lateinit var inflater: LayoutInflater

    private var lockableScrollView: LockableScrollView? = null

    private var orderListLabel: TextView? = null
    private var orderListView: ListView? = null

    private var fillListLabel: TextView? = null
    private var fillListView: ListView? = null

    private var tradingPairSpinner: Spinner? = null

    private var candles = listOf<Candle>()

    private var candleChart: PriceCandleChart? = null
    private var lineChart: PriceLineChart? = null

    private var currencyNameTextView: TextView? = null

    private var priceTextView: TextView? = null

    private var accountIcon: ImageView? = null
    private var balancesListView: ListView? = null

    private var highLabelTextView: TextView? = null
    private var highTextView: TextView? = null
    private var lowLabelTextView: TextView? = null
    private var lowTextView: TextView? = null

    private var openLabelTextView: TextView? = null
    private var openTextView: TextView? = null
    private var closeLabelTextView: TextView? = null
    private var volumeLabelTextView: TextView? = null
    private var volumeTextView: TextView? = null

    private var buyButton: Button? = null
    private var sellButton: Button? = null

    private var timespanRadioGroup: RadioGroup? = null

    private var historyTabLayout: TabLayout? = null

    private var tradeFragment: TradeFragment? = null

    private var blockRefresh = false
    private var didTouchTradingPairSpinner = false
    private var skipNextOrderFillCheck = false

    val timespan: Timespan
        get() = viewModel.timeSpan

    val chartStyle: ChartStyle
        get() = viewModel.chartStyle

    val tradingPair: TradingPair?
        get() = viewModel.tradingPair

    private val quoteCurrency: Currency
        get() = viewModel.tradingPair?.quoteCurrency ?: Currency.USD

    companion object {
        var product: Product = Product.dummyProduct

        var currency: Currency
            get() = product.currency
            set(value) {
                Product.map[value.id]?.let {
                    product = it
                } ?: run {
                    //TODO: don't change
                }
            }
    }

    private lateinit var viewModel: ChartViewModel
    class ChartViewModel : ViewModel() {
        var timeSpan = Timespan.DAY
        var chartStyle = ChartStyle.Line
        var tradingPair: TradingPair? = null
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_chart, container, false)

        viewModel = ViewModelProviders.of(this).get(ChartViewModel::class.java)
        showDarkMode(rootView)

        setHasOptionsMenu(true)
        lockPortrait = false
        this.inflater = inflater

//        val tradingPairStr = savedInstanceState?.getString(CHART_TRADING_PAIR) ?: ""
//        val chartStyleStr  = savedInstanceState?.getString(CHART_STYLE) ?: ""
//        val timespanLong   = savedInstanceState?.getLong(CHART_TIMESPAN) ?: 0
//        viewModel.tradingPair = TradingPair(tradingPairStr)
//        viewModel.chartStyle = ChartStyle.forString(chartStyleStr)
//        viewModel.timespan = Timespan.forLong(timespanLong)

        setupSwipeRefresh(rootView.swipe_refresh_layout as SwipeRefreshLayout)

        val tradingPairs: List<TradingPair> = if (product.tradingPairs.isNotEmpty()) {
            product.tradingPairs.sortTradingPairs()
        } else {
            listOf()
        }

        viewModel.tradingPair = tradingPairs.firstOrNull()

        candles = product.candlesForTimespan(timespan, tradingPair)

        lineChart = rootView.chart_line_chart
        candleChart = rootView.chart_candle_chart

        val granularity = Candle.granularityForTimespan(timespan)
        val tradingPair = tradingPair ?: TradingPair(Exchange.CBPro, product.currency, Currency.USD)
        lineChart?.configure(candles, granularity, timespan, tradingPair, true, DefaultDragDirection.Horizontal) {
            swipeRefreshLayout?.isEnabled = false
            lockableScrollView?.scrollToTop(800)
            lockableScrollView?.scrollLocked = true
        }
        lineChart?.setOnChartValueSelectedListener(this)
        lineChart?.onChartGestureListener = this

        candleChart?.configure(candles, currency, true, DefaultDragDirection.Horizontal) {
            swipeRefreshLayout?.isEnabled = false
            lockableScrollView?.scrollToTop(800)
            lockableScrollView?.scrollLocked = true
        }
        candleChart?.setOnChartValueSelectedListener(this)
        candleChart?.onChartGestureListener = this

        currencyNameTextView = rootView.txt_chart_name

        priceTextView = rootView.txt_chart_price

        buyButton = rootView.btn_chart_buy

        highLabelTextView = rootView.txt_chart_high_label
        highTextView = rootView.txt_chart_high
        lowLabelTextView = rootView.txt_chart_low_label
        lowTextView = rootView.txt_chart_low

        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            balancesListView = rootView.list_chart_balances

            accountIcon = rootView.img_chart_account_icon

            sellButton = rootView.btn_chart_sell

            orderListLabel = rootView.txt_chart_orders_label
            orderListView = rootView.list_chart_orders

            fillListLabel = rootView.txt_chart_fills_label
            fillListView = rootView.list_chart_fills

            lockableScrollView = rootView.lockscroll_chart

        } else {
            openLabelTextView = rootView.txt_chart_open_label
            openTextView = rootView.txt_chart_open
            closeLabelTextView =  rootView.txt_chart_close_label

            volumeLabelTextView = rootView.txt_chart_volume_label
            volumeTextView = rootView.txt_chart_volume
        }

        context?.let {
            buyButton?.setOnClickListener {
                buySellButtonOnClick(tradingPair.exchange.isLoggedIn(), TradeSide.BUY)
            }
            sellButton?.setOnClickListener {
                buySellButtonOnClick(tradingPair.exchange.isLoggedIn(), TradeSide.SELL)
            }

            val tradingPairAdapter = TradingPairSpinnerAdapter(it, tradingPairs)
            tradingPairAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            tradingPairSpinner = rootView.spinner_chart_trading_pair
            tradingPairSpinner?.adapter = tradingPairAdapter
            val tradingPairListener = object : AdapterView.OnItemSelectedListener, View.OnTouchListener {
                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    didTouchTradingPairSpinner = true
                    return false
                }
                override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                    if (lifecycle.currentState == Lifecycle.State.RESUMED && didTouchTradingPairSpinner) {
                        val tempTradingPairIndex = product.tradingPairs.indexOf(tradingPair)
                        viewModel.tradingPair = tradingPairSpinner?.selectedItem as? TradingPair
                        showProgressSpinner()
                        miniRefresh({
                            toast(R.string.chart_update_error)
                            tradingPairSpinner?.setSelection(tempTradingPairIndex)
                            didTouchTradingPairSpinner = false

                            dismissProgressSpinner()
                        }, {
                            dismissProgressSpinner()
                        })
                    }
                }
                override fun onNothingSelected(parent: AdapterView<*>) {}
            }
            tradingPairSpinner?.onItemSelectedListener = tradingPairListener
            tradingPairSpinner?.setOnTouchListener(tradingPairListener)
        }

        timespanRadioGroup = rootView.rgroup_chart_timespans

        rootView.rbtn_chart_timespan_hour.text = resources.getString(R.string.chart_timespan_1h)
        rootView.rbtn_chart_timespan_hour.setOnClickListener {
            setChartTimespan(Timespan.HOUR)
        }
        rootView.rbtn_chart_timespan_day.text = resources.getString(R.string.chart_timespan_1d)
        rootView.rbtn_chart_timespan_day.setOnClickListener {
            setChartTimespan(Timespan.DAY)
        }
        rootView.rbtn_chart_timespan_week.text = resources.getString(R.string.chart_timespan_1w)
        rootView.rbtn_chart_timespan_week.setOnClickListener {
            setChartTimespan(Timespan.WEEK)
        }
        rootView.rbtn_chart_timespan_month.text = resources.getString(R.string.chart_timespan_1m)
        rootView.rbtn_chart_timespan_month.setOnClickListener {
            setChartTimespan(Timespan.MONTH)
        }
        rootView.rbtn_chart_timespan_year.text = resources.getString(R.string.chart_timespan_1y)
        rootView.rbtn_chart_timespan_year.setOnClickListener {
            setChartTimespan(Timespan.YEAR)
        }

        return rootView
    }

    private var blockNextProductChange = false
    override fun onResume() {
        super.onResume()
        val tradingPairs = product.tradingPairs.sortTradingPairs()
        val index = tradingPairs.indexOf(tradingPair)
        if (index != -1) {
            tradingPairSpinner?.setSelection(index)
        }

        currencyNameTextView?.text = currency.fullName
        setPercentChangeText(timespan)
        checkTimespanButton()
        updateChartStyle()
        highLabelTextView?.visibility = View.GONE
        highTextView?.visibility = View.GONE
        lowLabelTextView?.visibility = View.GONE
        lowTextView?.visibility = View.GONE

        openLabelTextView?.visibility = View.GONE
        openTextView?.visibility = View.GONE
        closeLabelTextView?.visibility = View.GONE
        volumeLabelTextView?.visibility = View.GONE
        volumeTextView?.visibility = View.GONE

        blockNextProductChange = true

        val currencyList = Product.map.keys.map { Currency(it) }
        showNavSpinner(currency, currencyList) { selectedCurrency ->
            if (!blockNextProductChange) {
                Product.map[selectedCurrency.id]?.let {
                    switchProduct(it)
                }
            }
            blockNextProductChange = false
        }

        context?.let {
            updateHistoryListsFromStashes(it)
            product.defaultTradingPair?.let { defaultTradingPair ->
                checkOrdersAndFills(defaultTradingPair, it)
            }
            skipNextOrderFillCheck = true
        }
        if (currency.type != Currency.Type.CRYPTO) {
            setButtonsAndBalanceText(product)
            switchProduct(product)
        } else {
            val mainActivity = activity as? MainActivity

            val selectedCurrency = mainActivity?.navSpinner?.selectedItem as? Currency
            currency = if (selectedCurrency != null) {
                selectedCurrency
            } else {
                System.out.println("Account reset to BTC")
                Currency.BTC
            }
            setButtonsAndBalanceText(product)
            switchProduct(product)
        }

        autoRefresh = Runnable {
            if (!blockRefresh) {
                miniRefresh({ }, { })
            }
            handler.postDelayed(autoRefresh, TimeInMillis.tenSeconds)
            blockRefresh = false
        }
        handler.postDelayed(autoRefresh, TimeInMillis.tenSeconds)
        dismissProgressSpinner()
        refresh { endRefresh() }
    }

    override fun onPause() {
        handler.removeCallbacks(autoRefresh)
        super.onPause()
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        val shouldShowOptions = lifecycle.isCreatedOrResumed
        menu.setGroupVisible(R.id.group_chart_style, shouldShowOptions)
    }

    override fun onCreateOptionsMenu(menu: Menu?, inflater: MenuInflater?) {
        inflater?.inflate(R.menu.chart_menu, menu)
        setOptionsMenuTextColor(menu)

        super.onCreateOptionsMenu(menu, inflater)
        if (chartStyle == ChartStyle.Line) {
            menu?.findItem(R.id.chart_style_line)?.isChecked = true
        } else {
            menu?.findItem(R.id.chart_style_candle)?.isChecked = true
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.chart_style_line   -> viewModel.chartStyle = ChartStyle.Line
            R.id.chart_style_candle -> viewModel.chartStyle = ChartStyle.Candle
        }
        item.isChecked = true

        showProgressSpinner()
        miniRefresh({
            dismissProgressSpinner()
        }, {
            updateChartStyle()
            dismissProgressSpinner()
        })
        return false
    }

    private fun updateChartStyle() {
        when(chartStyle) {
            ChartStyle.Line -> {
                lineChart?.visibility = View.VISIBLE
                    candleChart?.visibility = View.GONE
                }
                ChartStyle.Candle -> {
                    lineChart?.visibility = View.GONE
                    candleChart?.visibility = View.VISIBLE
            }
        }
    }

    private fun buySellButtonOnClick(isLoggedIn: Boolean, tradeSide: TradeSide) {
        val isAnyXProActive = if (context != null) {
            Prefs(context!!).isAnyXProActive
        } else {
            false
        }
        if (!isLoggedIn) {
            toast(R.string.toast_please_login_message)
        } else if (!isAnyXProActive && CBProApi.credentials?.isVerified == null) {
            (activity as? MainActivity)?.let {
                it.goToVerify{ didVerify ->
                    if (didVerify) {
                        goToTradeFragment(tradeSide)
                        it.setDrawerMenu()
                    }
                }
            } ?: run {
                toast(R.string.toast_please_verify_message)
            }
        } else if (!isAnyXProActive && CBProApi.credentials?.isVerified == false) {
            toast(R.string.toast_missing_permissions_message)
        } else {
            goToTradeFragment(tradeSide)
        }
    }
    private fun goToTradeFragment(tradeSide: TradeSide) {
        if (tradeFragment == null) {
            tradeFragment = TradeFragment.newInstance(tradeSide)
        } else {
            TradeFragment.tradeSide = tradeSide
        }
        (activity as? MainActivity)?.goToFragment(tradeFragment!!, FragmentType.TRADE.toString())
    }

    private fun switchProduct(newProduct: Product) {
        product = newProduct
        blockRefresh = true
        didTouchTradingPairSpinner = false

        val tradingPairs = product.tradingPairs.sortTradingPairs()
        val relevantTradingPair = tradingPairs.find { it.quoteCurrency == tradingPair?.quoteCurrency }

        viewModel.tradingPair = relevantTradingPair ?: tradingPairs.firstOrNull()

        candles = newProduct.candlesForTimespan(timespan, tradingPair)
        //TODO: make sure account has all valid info
        switchProductCandlesCheck(product)
    }

    private fun checkOrdersAndFills(tradingPair: TradingPair, context: Context) {
        val prefs = Prefs(context)
        val exchange = tradingPair.exchange
        val stashedFills = prefs.getStashedFills(currency)
        val stashedOrders = prefs.getStashedOrders(currency)

        val dateOrdersLastStashed = prefs.getDateOrdersLastStashed(exchange)
        val dateFillsLastStashed  = prefs.getDateFillsLastStashed(tradingPair, exchange)
        val nowInSeconds = Calendar.getInstance().timeInSeconds()

        if (dateOrdersLastStashed + TimeInMillis.oneHour > nowInSeconds) {
            Order.getAndStashList(apiInitData, currency, { //OnFailure:
                updateFills(currency, stashedOrders, stashedFills)
            }) { newOrderList -> // OnSuccess:
                updateFills(currency, newOrderList,  stashedFills)
            }
        } else if (dateFillsLastStashed + TimeInMillis.fiveMinutes > nowInSeconds) {
            updateFills(currency, stashedOrders, stashedFills)
        } else if (dateFillsLastStashed + TimeInMillis.oneDay > nowInSeconds && stashedOrders.isNotEmpty()) {
            updateFills(currency, stashedOrders, stashedFills)
        }
    }

    private fun updateFills(currency: Currency, orderList: List<Order>, stashedFills: List<Fill>) {
        Fill.getAndStashList(apiInitData, currency, {
            context?.let {
                if (lifecycle.isCreatedOrResumed) {
                    updateHistoryLists(it, orderList, stashedFills)
                }
            }
        }) { apiFillList ->
            context?.let {
                if (lifecycle.isCreatedOrResumed) {
                    updateHistoryLists(it,orderList, apiFillList)
                }
            }
        }
    }

    private fun areCandlesUpToDate(timespan: Timespan): Boolean {
        val nowInSeconds = Calendar.getInstance().timeInSeconds()
        val candles = product.candlesForTimespan(timespan, tradingPair)
        val lastCandleTime = candles.lastOrNull()?.closeTime ?: 0
        val nextCandleTime = lastCandleTime + Candle.granularityForTimespan(timespan)
        return candles.isNotEmpty() && (nextCandleTime > nowInSeconds)
    }

    private fun switchProductCandlesCheck(product: Product) {
        if (areCandlesUpToDate(timespan)) {
            completeSwitchProduct(product)
        } else {
            showProgressSpinner()
            miniRefresh({   //onFailure
                //Even if miniRefresh fails here, switch anyways
                dismissProgressSpinner()
                candles = product.candlesForTimespan(timespan, tradingPair)
                completeSwitchProduct(product)
            }, {    //success
                dismissProgressSpinner()
                candles = product.candlesForTimespan(timespan, tradingPair)
                completeSwitchProduct(product)
            })
        }
    }
    private fun completeSwitchProduct(product: Product) {
        blockRefresh = false
        lockableScrollView?.scrollToTop(200)

        val price = product.priceForQuoteCurrency(quoteCurrency)
        priceTextView?.text = price.format(quoteCurrency)

        context?.let { context ->
            val tradingPairs = Companion.product.tradingPairs.sortTradingPairs()
            val tradingPairSpinnerAdapter = TradingPairSpinnerAdapter(context, tradingPairs)
            tradingPairSpinner?.adapter = tradingPairSpinnerAdapter

            val index = tradingPairs.indexOf(tradingPair)
            if (index != -1) {
                tradingPairSpinner?.setSelection(index)
            }

            product.defaultTradingPair?.let { tradingPair ->
                addCandlesToActiveChart(candles, tradingPair)
                setPercentChangeText(timespan)
                currencyNameTextView?.text = product.currency.fullName
                setButtonsAndBalanceText(product)

                if (!skipNextOrderFillCheck) {
                    updateHistoryListsFromStashes(context)
                    checkOrdersAndFills(tradingPair, context)
                }
                skipNextOrderFillCheck = false
            }
            return@let
        }
    }

    private fun setButtonsAndBalanceText(product: Product) {
        context?.let {
            val currency = product.currency
            setButtonColors()
            if (Exchange.isAnyLoggedIn()) {
                balancesListView?.visibility = View.VISIBLE
                val accounts = product.accounts.values.toList()
                val quoteCurrency = tradingPair?.quoteCurrency ?: Account.defaultFiatCurrency
                balancesListView?.adapter = ChartBalanceListViewAdapter(it,  accounts, quoteCurrency)
                balancesListView?.setHeightBasedOnChildren(0, 0)

                currency.iconId?.let { iconId ->
                    accountIcon?.visibility = View.VISIBLE
                    accountIcon?.setImageResource(iconId)
                } ?: run {
                    accountIcon?.visibility = View.GONE
                }
                updateBalancesList()
                orderListLabel?.visibility = View.VISIBLE
                orderListView?.visibility = View.VISIBLE
                fillListLabel?.visibility = View.VISIBLE
                fillListView?.visibility = View.VISIBLE
            } else {
                accountIcon?.visibility = View.GONE
                balancesListView?.visibility = View.GONE

                orderListLabel?.visibility = View.GONE
                orderListView?.visibility = View.GONE
                fillListLabel?.visibility = View.GONE
                fillListView?.visibility = View.GONE
            }
        }
    }

    private fun setButtonColors() {
        context?.let { context ->
            val buttonColors = currency.colorStateList(context)
            buyButton?.backgroundTintList = buttonColors
            sellButton?.backgroundTintList = buttonColors
            val buttonTextColor = currency.buttonTextColor(context)
            buyButton?.textColor = buttonTextColor
            sellButton?.textColor = buttonTextColor
            val tabColor = currency.colorPrimary(context)
            historyTabLayout?.setSelectedTabIndicatorColor(tabColor)
        }
    }

    private fun checkTimespanButton() {
        when (timespan) {
            Timespan.HOUR -> rbtn_chart_timespan_hour.isChecked = true
            Timespan.DAY ->  rbtn_chart_timespan_day.isChecked = true
            Timespan.WEEK -> rbtn_chart_timespan_week.isChecked = true
            Timespan.MONTH -> rbtn_chart_timespan_month.isChecked = true
            Timespan.YEAR -> rbtn_chart_timespan_year.isChecked = true
//            Timespan.ALL -> timespanButtonAll.isChecked = true
        }
    }

    private fun setChartTimespan(newTimespan: Timespan) {
        checkTimespanButton()
        val tempTimespan = timespan
        if (tempTimespan != newTimespan) {
            timespanRadioGroup?.isEnabled = false
            viewModel.timeSpan = newTimespan
            showProgressSpinner()
            if (areCandlesUpToDate(timespan)) {
                candles = product.candlesForTimespan(timespan, tradingPair)
                val price = product.priceForQuoteCurrency(quoteCurrency)
                completeMiniRefresh(price, candles) {
                    dismissProgressSpinner()
                    timespanRadioGroup?.isEnabled = true
                }
            } else {
                miniRefresh({
                    toast(R.string.chart_update_error)
                    viewModel.timeSpan = tempTimespan
                    timespanRadioGroup?.isEnabled = true
                    dismissProgressSpinner()
                }, {
                    checkTimespanButton()
                    dismissProgressSpinner()
                    timespanRadioGroup?.isEnabled = true
                })
            }
        }
    }

    private fun setPercentChangeText(timespan: Timespan) {
        val percentChange = product.percentChange(timespan, quoteCurrency)
        txt_chart_change_or_date?.text = percentChange.percentFormat()
        txt_chart_change_or_date?.textColor = if (percentChange >= 0) {
            Color.GREEN
        } else {
            Color.RED
        }
    }

    private fun cancelOrder(order: Order) {
        order.cancel(apiInitData, {  }) {
            if (lifecycle.isCreatedOrResumed) {
                var orders = (orderListView?.adapter as OrderListViewAdapter).orders
                orders = orders.filter { o -> o.id != order.id }
                context?.let { context ->
                    updateHistoryLists(context, orders)
                }
                toast(R.string.chart_order_cancelled)
            }
        }
    }

    override fun onValueSelected(entry: Entry, h: Highlight) {
        val time = entry.data as? Long
        priceTextView?.text = entry.y.toDouble().format(quoteCurrency)
        txt_chart_change_or_date.text = time?.toStringWithTimespan(timespan)
        context?.let {
            if (Prefs(it).isDarkModeOn) {
                txt_chart_change_or_date.textColor = Color.WHITE
            } else {
                txt_chart_change_or_date.textColor = Color.BLACK
            }
        }
        if (tradingPair?.quoteCurrency?.type != Currency.Type.FIAT &&
                resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            currencyNameTextView?.visibility = View.GONE
        }

        if (chartStyle == ChartStyle.Candle && entry is CandleEntry) {

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE || tradingPair?.quoteCurrency?.isFiat == true) {
                highLabelTextView?.visibility = View.VISIBLE
                highTextView?.visibility = View.VISIBLE
                lowLabelTextView?.visibility = View.VISIBLE
                lowTextView?.visibility = View.VISIBLE

                openLabelTextView?.visibility = View.VISIBLE
                openTextView?.visibility = View.VISIBLE
                closeLabelTextView?.visibility = View.VISIBLE

                volumeLabelTextView?.visibility = View.VISIBLE
                volumeTextView?.visibility = View.VISIBLE
            }

            if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                highTextView?.typeface = Typeface.MONOSPACE
                lowTextView?.typeface = Typeface.MONOSPACE
                openTextView?.typeface = Typeface.MONOSPACE
                volumeTextView?.typeface = Typeface.MONOSPACE
                priceTextView?.typeface = Typeface.MONOSPACE
                currencyNameTextView?.visibility = View.INVISIBLE
            }

            highTextView?.text = entry.high.toDouble().format(quoteCurrency)
            lowTextView?.text = entry.low.toDouble().format(quoteCurrency)
            openTextView?.text = entry.open.toDouble().format(quoteCurrency)

            volumeTextView?.text = entry.volume.volumeFormat()
        }
    }

    override fun onNothingSelected() {
        priceTextView?.text = product.priceForQuoteCurrency(quoteCurrency).format(quoteCurrency)
        setPercentChangeText(timespan)
        when (chartStyle) {
            ChartStyle.Line -> lineChart?.highlightValues(arrayOf<Highlight>())
            ChartStyle.Candle -> candleChart?.highlightValues(arrayOf<Highlight>())
        }
        currencyNameTextView?.visibility = View.VISIBLE

        highLabelTextView?.visibility = View.GONE
        highTextView?.visibility = View.GONE
        lowLabelTextView?.visibility = View.GONE
        lowTextView?.visibility = View.GONE

        openLabelTextView?.visibility = View.GONE
        openTextView?.visibility = View.GONE
        closeLabelTextView?.visibility = View.GONE
        volumeLabelTextView?.visibility = View.GONE
        volumeTextView?.visibility = View.GONE

        //format these:
        highTextView?.typeface = Typeface.DEFAULT
        lowTextView?.typeface = Typeface.DEFAULT
        openTextView?.typeface = Typeface.DEFAULT
        volumeTextView?.typeface = Typeface.DEFAULT
        priceTextView?.typeface = Typeface.DEFAULT
    }

    override fun onChartGestureStart(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) { }

    override fun onChartGestureEnd(me: MotionEvent, lastPerformedGesture: ChartTouchListener.ChartGesture) {
        swipeRefreshLayout?.isEnabled = true
        lockableScrollView?.scrollLocked = false
        onNothingSelected()
    }
    override fun onChartLongPressed(me: MotionEvent) {
        swipeRefreshLayout?.isEnabled = false
        lockableScrollView?.scrollLocked = true
    }
    override fun onChartDoubleTapped(me: MotionEvent) { }
    override fun onChartSingleTapped(me: MotionEvent) { }
    override fun onChartFling(me1: MotionEvent, me2: MotionEvent, velocityX: Float, velocityY: Float) { }
    override fun onChartScale(me: MotionEvent, scaleX: Float, scaleY: Float) { }
    override fun onChartTranslate(me: MotionEvent, dX: Float, dY: Float) { }

    override fun refresh(onComplete: (Boolean) -> Unit) {
        val onFailure = { result: Result.Failure<String, FuelError> ->
            if (context != null) {
                toast(resources.getString(R.string.error_generic_message, result.errorMessage))
            }
            onComplete(false)
        }

        val context = context

        if (context != null && Exchange.isAnyLoggedIn()) {
            /* Refresh does 2 things, it updates the chart, account info first
             * then candles etc in mini refresh, while simultaneously updating history info
            */

            var updatedAccounts = 0
            var failedAccounts = 0
            val accounts = product.accounts.values
            for (account in accounts) {
                AnyApi(apiInitData).updateAccount(account, {
                    updatedAccounts++
                    failedAccounts++
                    if (failedAccounts >= accounts.size) {
                        onFailure(it)
                    } else if (lifecycle.isCreatedOrResumed && updatedAccounts >= accounts.size) {
                        updateBalancesList()
                        miniRefresh(onFailure) {
                            onComplete(true)
                        }
                    }
                }) {
                    updatedAccounts++
                    if (lifecycle.isCreatedOrResumed && updatedAccounts >= accounts.size) {
                        updateBalancesList()
                        miniRefresh(onFailure) {
                            onComplete(true)
                        }
                    }
                }
            }

            var filteredOrders: List<Order>? = null
            var filteredFills: List<Fill>? = null
            Order.getAndStashList(apiInitData, currency, onFailure) { orderList ->
                if (lifecycle.isCreatedOrResumed) {
                    filteredOrders = orderList
                    filteredFills?.let {
                        updateHistoryLists(context, orderList, it)
                    }
                }
            }
            Fill.getAndStashList(apiInitData, currency, onFailure) { fillList ->
                if (lifecycle.isCreatedOrResumed) {
                    filteredFills = fillList
                    filteredOrders?.let {
                        updateHistoryLists(context, it, fillList)
                    }
                }
            }
        } else {
            miniRefresh(onFailure) {
                onComplete(true)
            }
        }
    }

    private fun updateHistoryListsFromStashes(context: Context) {
        val prefs = Prefs(context)
        val stashedFills = prefs.getStashedFills(currency)
        val combinedFills = stashedFills.combineFills()
        val stashedOrders = prefs.getStashedOrders(currency)
        updateHistoryLists(context, stashedOrders, combinedFills)
    }

    private fun updateHistoryLists(context: Context, orderList: List<Order>, fillList: List<Fill>? = null) {
        orderListView?.adapter = OrderListViewAdapter(context, orderList, resources,
                { order -> //Order On Click:
                    order.showExtraInfo = !order.showExtraInfo
                    (orderListView?.adapter as? OrderListViewAdapter)?.notifyDataSetChanged()
                    orderListView?.setHeightBasedOnChildren()
                } , { order -> //Order Cancel:
                    //confirmCancelOrder(order)
                    cancelOrder(order)
        })
        orderListView?.setHeightBasedOnChildren()

        if (fillList != null) {
            val sortedFills = fillList.sortedBy { it.time }.reversed()
            fillListView?.adapter = FillListViewAdapter(context, sortedFills, resources) { fill ->
                fill.showExtraInfo = !fill.showExtraInfo
                (fillListView?.adapter as? FillListViewAdapter)?.notifyDataSetChanged()
                fillListView?.setHeightBasedOnChildren()
            }
            fillListView?.setHeightBasedOnChildren()
        }
    }

    private fun miniRefresh(onFailure: (result: Result.Failure<String, FuelError>) -> Unit, onComplete: () -> Unit) {
        val tradingPairTemp = tradingPair
        if (currency.type == Currency.Type.FIAT || tradingPairTemp == null) {
            onComplete()
        } else {
            product.updateCandles(timespan, tradingPairTemp, apiInitData,  onFailure) {
                if (lifecycle.isCreatedOrResumed) {
                    if (tradingPairTemp == tradingPair) {
                        candles = product.candlesForTimespan(timespan, tradingPair)
                        tradingPair?.let { tradingPair ->
                            AnyApi(apiInitData).ticker(tradingPair, onFailure) {
                                if (lifecycle.isCreatedOrResumed) {
                                    val price = product.priceForQuoteCurrency(quoteCurrency)
                                    completeMiniRefresh(price, candles, onComplete)
                                }
                            }
                        } ?: run {
                            val price = candles.lastOrNull()?.close ?: 0.0
                            completeMiniRefresh(price, candles, onComplete)
                        }
                    } else {
                        val error = Result.Failure<String, FuelError>(FuelError(Exception()))
                        onFailure(error)
                    }
                }
            }
        }
    }

    private fun updateBalancesList() {
        (balancesListView?.adapter as? ChartBalanceListViewAdapter)?.quoteCurrency = tradingPair?.quoteCurrency ?: Account.defaultFiatCurrency
        (balancesListView?.adapter as? ChartBalanceListViewAdapter)?.accounts = product.accounts.values.toList()
        (balancesListView?.adapter as? ChartBalanceListViewAdapter)?.notifyDataSetChanged()
        balancesListView?.setHeightBasedOnChildren(0, 0)
    }

    private fun completeMiniRefresh(price: Double, candles: List<Candle>, onComplete: () -> Unit) {
        priceTextView?.text = price.format(quoteCurrency)
        updateBalancesList()
        val tradingPair = tradingPair ?: TradingPair(Exchange.CBPro, product.currency, Currency.USD)
        addCandlesToActiveChart(candles, tradingPair)
        setPercentChangeText(timespan)
        checkTimespanButton()
        onComplete()
    }

    private fun addCandlesToActiveChart(candles: List<Candle>, tradingPair: TradingPair) {
        val granularity = Candle.granularityForTimespan(timespan)
        when (chartStyle) {
            ChartStyle.Line   ->   lineChart?.addCandles(candles, granularity, timespan, tradingPair)
            ChartStyle.Candle -> candleChart?.addCandles(candles, currency)
        }
    }
}
