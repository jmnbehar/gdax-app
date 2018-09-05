package com.anyexchange.anyx.fragments.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.adapters.spinnerAdapters.RelatedAccountSpinnerAdapter
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_transfer.view.*
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class TransferFragment : RefreshFragment() {

    private lateinit var inflater: LayoutInflater

    private lateinit var titleText: TextView

    private lateinit var transferDetailsLayout: LinearLayout

    private lateinit var interactiveLayout: LinearLayout

    private lateinit var sourceAccountsLabelTxt: TextView
    private lateinit var sourceAccountsSpinner: Spinner
    private lateinit var sourceAccountText: TextView

    private lateinit var transferMaxButton: Button

    private lateinit var amountLabelText: TextView
    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView

    private lateinit var infoText: TextView
    private lateinit var destAccountsSpinner: Spinner
    private lateinit var destBalanceText: TextView

    private lateinit var submitTransferButton: Button

    private var coinbaseAccounts: List<Account.CoinbaseAccount> = listOf()

    private var sourceAccounts: MutableList<BaseAccount> = mutableListOf()
    private val sourceAccount: BaseAccount?
        get() {
            val spinnerSelection = sourceAccountsSpinner.selectedItem as? BaseAccount
            return spinnerSelection ?: sourceAccounts.firstOrNull()
        }
    private var destAccounts:   List<BaseAccount?> = mutableListOf()
    private val destAccount: BaseAccount?
        get() {
            return if (destAccounts.size > 1) {
                destAccountsSpinner.selectedItem as? BaseAccount ?: destAccounts.firstOrNull()
            } else {
                destAccounts.firstOrNull()
            }
        }

    var currency: Currency
        get() = ChartFragment.currency
        set(value) { ChartFragment.currency = value }

    companion object {
        fun newInstance(): TransferFragment {
            return TransferFragment()
        }

        val hasRelevantData: Boolean
            get() {
                val coinbaseAccounts = Account.cryptoAccounts.mapNotNull { account -> account.coinbaseAccount }
                return (coinbaseAccounts.isNotEmpty() && Account.paymentMethods.isNotEmpty())
            }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_transfer, container, false)
        setupSwipeRefresh(rootView.swipe_refresh_layout)

        this.inflater = inflater
        val activity = activity!!
        titleText = rootView.txt_transfer_title

        transferDetailsLayout = rootView.layout_transfer_details
        interactiveLayout = rootView.layout_transfer_interactive_layout

        amountLabelText = rootView.txt_transfer_amount_label
        amountEditText = rootView.etxt_transfer_amount
        amountUnitText = rootView.txt_transfer_amount_unit

        transferMaxButton = rootView.btn_transfer_max

        sourceAccountsLabelTxt = rootView.txt_transfer_account_label
        sourceAccountsSpinner = rootView.spinner_transfer_accounts
        sourceAccountText = rootView.txt_transfer_account_info

        destAccountsSpinner = rootView.spinner_transfer_destination_accounts
        infoText = rootView.txt_transfer_info
        destBalanceText = rootView.txt_transfer_destination_info

        submitTransferButton = rootView.btn_transfer_submit_transfer

        val relatedAccountSpinnerAdapter = RelatedAccountSpinnerAdapter(activity, sourceAccounts)

        sourceAccountsSpinner.adapter = relatedAccountSpinnerAdapter
        sourceAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (sourceAccounts.size > position) {
                    sourceAccountSelected()
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }
        destAccountsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                setInfoAndButtons()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        transferMaxButton.setOnClickListener {
            sourceAccount?.balance?.let { balance ->
                amountEditText.setText(balance.btcFormatShortened())
            }
        }

        submitTransferButton.setOnClickListener {
            submitTransfer()
        }

        return rootView
    }

    override fun onResume() {
        super.onResume()

        val relevantCurrencies = Account.fiatAccounts.map { it.currency }.toMutableList()
        relevantCurrencies.addAll(Currency.cryptoList)

        showNavSpinner(currency, relevantCurrencies) { selectedCurrency ->
            currency = selectedCurrency
            switchCurrency(selectedCurrency)
        }

        titleText.text = getString(R.string.transfer_in_title)

        coinbaseAccounts = Account.cryptoAccounts.mapNotNull { account -> account.coinbaseAccount }

        val fiatCoinbaseAccount = Account.defaultFiatAccount?.coinbaseAccount
        if (fiatCoinbaseAccount != null) {
            coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
        }

        amountUnitText.text = currency.toString()

        switchCurrency(currency)

        dismissProgressSpinner()
    }

    private var isRefreshing = false
    override fun refresh(onComplete: (Boolean) -> Unit) {
        if (!isRefreshing) {
            isRefreshing = true
            var didUpdateCBPro = false
            var didUpdateCoinbase = false
            var didUpdatePaymentMethods = false
            CBProApi.accounts(apiInitData).updateAllAccounts({
                toast(R.string.toast_coinbase_pro_site_error)
                isRefreshing = false
                onComplete(false)
            }) {
                didUpdateCBPro = true
                if (didUpdateCoinbase && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            }
            CBProApi.coinbaseAccounts(apiInitData).linkToAccounts({
                toast(R.string.toast_coinbase_site_error)
                isRefreshing = false
                onComplete(false)
            }, {
                coinbaseAccounts = Account.cryptoAccounts.mapNotNull { account -> account.coinbaseAccount }
                val fiatCoinbaseAccount = Account.defaultFiatAccount?.coinbaseAccount
                if (fiatCoinbaseAccount != null) {
                    coinbaseAccounts = coinbaseAccounts.plus(fiatCoinbaseAccount)
                }
                coinbaseAccounts = coinbaseAccounts.filter { account -> account.balance > 0 }
                didUpdateCoinbase = true
                if (didUpdateCBPro && didUpdatePaymentMethods) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
            CBProApi.paymentMethods(apiInitData).get({
                onComplete(false)
            }, { result ->
                Account.paymentMethods = result
                didUpdatePaymentMethods = true
                if (didUpdateCBPro && didUpdateCoinbase) {
                    completeRefresh(onComplete)
                    isRefreshing = false
                }
            })
        }
    }

    private fun submitTransfer() {
        val amountString = amountEditText.text.toString()
        val amount = amountString.toDoubleOrZero()

        if (amount <= 0) {
            showPopup(R.string.transfer_amount_error)
        } else {
            when (sourceAccount) {
                is Account.CoinbaseAccount -> {
                    val coinbaseAccount = sourceAccount as Account.CoinbaseAccount
                    if (amount > coinbaseAccount.balance) {
                        showPopup(R.string.transfer_funds_error)
                    } else {
                        showProgressSpinner()
                        CBProApi.getFromCoinbase(apiInitData, amount, currency, coinbaseAccount.id).executePost( { result ->
                            val errorMessage = CBProApi.ErrorMessage.forString(result.errorMessage)
                            if (amount > 0 && errorMessage == CBProApi.ErrorMessage.TransferAmountTooLow) {
                                showPopup(R.string.transfer_amount_low_error)
                            } else {
                                showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                            }
                            dismissProgressSpinner()
                        } , { _ ->
                            toast(R.string.transfer_received_message)
                            amountEditText.setText("")

                            refresh { _ -> dismissProgressSpinner() }
                        })
                    }
                }
                is Account.PaymentMethod -> {
                    val paymentMethod = sourceAccount as Account.PaymentMethod
                    if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                        showPopup(R.string.transfer_funds_error)
                    } else {
                        showProgressSpinner()
                        CBProApi.getFromPayment(apiInitData, amount, currency, paymentMethod.id).executePost( { result ->
                            showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                            dismissProgressSpinner()
                        } , { _ ->
                            toast(R.string.transfer_received_message)
                            amountEditText.setText("")

                            refresh { _ -> dismissProgressSpinner() }
                        })
                    }

                }
                is Account -> {
                    when (destAccount) {
                        is Account.CoinbaseAccount -> {
                            val coinbaseAccount = destAccount as Account.CoinbaseAccount
                            val cbproAccount = Account.forCurrency(currency)

                            if (amount > cbproAccount?.availableBalance ?: 0.0) {
                                showPopup(R.string.transfer_funds_error)
                            } else {
                                showProgressSpinner()
                                CBProApi.sendToCoinbase(apiInitData, amount, currency, coinbaseAccount.id).executePost({ result ->
                                    showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                                    dismissProgressSpinner()
                                }, { _ ->
                                    toast(R.string.transfer_sent_message)
                                    amountEditText.setText("")

                                    refresh { dismissProgressSpinner() }
                                })
                            }
                        }
                        is Account.PaymentMethod -> {
                            val paymentMethod = destAccount as Account.PaymentMethod
                            if (paymentMethod.balance != null && amount > paymentMethod.balance) {
                                showPopup(R.string.transfer_funds_error)
                            } else {
                                showProgressSpinner()
                                CBProApi.sendToPayment(apiInitData, amount, currency, paymentMethod.id).executePost( { result ->
                                    showPopup(resources.getString(R.string.error_generic_message, result.errorMessage))
                                    dismissProgressSpinner()
                                }, { _ ->
                                    toast(R.string.transfer_sent_message)
                                    amountEditText.setText("")
                                    refresh { dismissProgressSpinner() }
                                })
                            }
                        }
                    }
                }
                else -> {
                    showPopup(R.string.error_message)
                }
            }
        }
    }

    private fun completeRefresh(onComplete: (Boolean) -> Unit) {
        if (isVisible) {
            transferDetailsLayout.visibility = View.VISIBLE
            amountUnitText.text = currency.toString()
            switchCurrency(currency)
        }
        onComplete(true)
    }


    private fun switchCurrency(currency: Currency) {
        ChartFragment.currency = currency
        amountEditText.setText("")


        val tempRelevantAccounts: MutableList<BaseAccount> = coinbaseAccounts.filter { account -> account.currency == currency }.toMutableList()
        Account.forCurrency(currency)?.let {
            tempRelevantAccounts.add(it)
        }
        if (currency.isFiat) {
            tempRelevantAccounts.addAll(Account.paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_withdraw && pm.apiPaymentMethod.currency == currency.toString() })
            //uncomment this if we want to have deposit in transfer page:
//        } else {
//            tempRelevantAccounts.add(Account.ExternalAccount(currency))
        }
        sourceAccounts = tempRelevantAccounts

        when (sourceAccounts.size) {
            0 -> {
                sourceAccountText.text = resources.getString(R.string.transfer_coinbase_account_empty, currency.toString())
                sourceAccountText.visibility = View.VISIBLE
                sourceAccountsSpinner.visibility = View.GONE
            }
            1 -> {
                sourceAccountText.text = sourceAccount.toString()
                sourceAccountText.visibility = View.VISIBLE
                sourceAccountsSpinner.visibility = View.GONE
            }
            else -> {
                context?.let {
                    sourceAccountsSpinner.adapter = RelatedAccountSpinnerAdapter(it, sourceAccounts)
                    sourceAccountText.visibility = View.GONE
                    sourceAccountsSpinner.visibility = View.VISIBLE
                }
            }
        }

        setDestAccounts()

        setInfoAndButtons()
    }

    private fun sourceAccountSelected() {
        val relevantAccount = Account.forCurrency(currency)
        if (sourceAccount is Account.ExternalAccount) {
            Account.forCurrency(currency)?.coinbaseAccount?.let {
                if (relevantAccount?.depositInfo?.address == null) {
                    CBProApi.depositAddress(apiInitData, it.id).get({ _ ->
                        toast("Error")
                        sourceAccountsSpinner.setSelection(0)
                    }) { depositInfo ->
                        relevantAccount?.depositInfo = depositInfo
                        setDestAccounts()
                        setInfoAndButtons()
                    }
                }
            } ?: run {
                toast("Error")
                sourceAccountsSpinner.setSelection(0)
            }
        } else {
            setDestAccounts()
            setInfoAndButtons()
        }
    }

    private fun setDestAccounts() {
        val cbproAccount = Account.forCurrency(currency)
        destAccounts = when(sourceAccount) {
            is Account.CoinbaseAccount -> listOf(cbproAccount)
            is Account.PaymentMethod ->  listOf(cbproAccount)
            is Account -> {
                val tempDestAccounts: MutableList<BaseAccount> = coinbaseAccounts.filter { account -> account.currency == currency }.toMutableList()
                if (currency.isFiat) {
                    tempDestAccounts.addAll(Account.paymentMethods.filter { pm -> pm.apiPaymentMethod.allow_withdraw && pm.apiPaymentMethod.currency == currency.toString() })
                }
                tempDestAccounts.toList()
            }
            is Account.ExternalAccount -> listOf(cbproAccount)
            else  -> listOf()
        }
        when (destAccounts.size) {
            0 -> {
                destBalanceText.text = resources.getString(R.string.transfer_no_destinations_text)
                destBalanceText.visibility = View.VISIBLE
                destAccountsSpinner.visibility = View.GONE
            }
            1 -> {
                destBalanceText.text = destAccount.toString()
                destBalanceText.visibility = View.VISIBLE
                destAccountsSpinner.visibility = View.GONE
            }
            else -> {
                val destAccountsTemp = destAccounts.filterNotNull()
                context?.let {
                    destAccountsSpinner.adapter = RelatedAccountSpinnerAdapter(it, destAccountsTemp)
                    destBalanceText.visibility = View.GONE
                    destAccountsSpinner.visibility = View.VISIBLE
                }
            }
        }
        amountUnitText.text = currency.toString()
    }

    private fun setInfoAndButtons() {
        infoText.visibility = View.VISIBLE
        interactiveLayout.visibility = View.VISIBLE
        when (sourceAccount) {
            is Account.CoinbaseAccount -> {
                setInteractiveLayoutEnabled((sourceAccount?.balance ?: 0.0) > 0.0)
                infoText.setText(R.string.transfer_coinbase_info)
            }
            is Account.PaymentMethod -> {
                infoText.setText(R.string.transfer_bank_info)
            }
            is Account -> {
                setInteractiveLayoutEnabled((sourceAccount?.balance ?: 0.0) > 0.0)
                when (destAccount) {
                    is Account.CoinbaseAccount -> infoText.setText(R.string.transfer_coinbase_info)
                    is Account.PaymentMethod -> infoText.setText(R.string.transfer_bank_info)
                }
            }
            is Account.ExternalAccount -> {
                val account = destAccount as Account
                interactiveLayout.visibility = View.GONE
                infoText.text = "Deposit address: " + (account.depositInfo?.address ?: "null")
            }
            else -> {
                interactiveLayout.visibility = View.GONE
                infoText.visibility = View.GONE
            }
        }

        context?.let { context ->
            amountUnitText.text = currency.toString()

            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)

            transferMaxButton.backgroundTintList = buttonColors
            submitTransferButton.backgroundTintList = buttonColors

            transferMaxButton.textColor = buttonTextColor
            submitTransferButton.textColor = buttonTextColor
        }
    }

    private fun setInteractiveLayoutEnabled(enabled: Boolean) {
        submitTransferButton.isEnabled = enabled
    }
}