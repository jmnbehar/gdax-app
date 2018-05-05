package com.anyexchange.anyx.fragments.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.anyexchange.anyx.activities.ScanActivity
import com.anyexchange.anyx.classes.*
import com.anyexchange.anyx.R
import kotlinx.android.synthetic.main.fragment_send.view.*
import org.jetbrains.anko.sdk25.coroutines.onClick
import org.jetbrains.anko.support.v4.alert
import org.jetbrains.anko.support.v4.toast
import org.jetbrains.anko.textColor

/**
 * Created by anyexchange on 11/5/2017.
 */
class SendFragment : RefreshFragment() {


    private lateinit var inflater: LayoutInflater
    private lateinit var titleText: TextView

    private lateinit var currencyTabLayout: TabLayout

    private lateinit var amountEditText: EditText
    private lateinit var amountUnitText: TextView
    private lateinit var amountLabelText: TextView

    private lateinit var destinationEditText: EditText
    private lateinit var destinationLabelText: TextView

    private lateinit var twoFactorEditText: EditText
    private lateinit var twoFactorLabelText: TextView
    private lateinit var twoFactorButton: Button

    private lateinit var scanButton: ImageButton

    private lateinit var warning1TextView: TextView
    private lateinit var warning2TextView: TextView

    private lateinit var sendButton: Button

    var currency = Currency.BTC

    companion object {
        lateinit var currency: Currency
        fun newInstance(): SendFragment {
            return SendFragment()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val rootView = inflater.inflate(R.layout.fragment_send, container, false)

        this.inflater = inflater

        titleText = rootView.txt_send_name

        currencyTabLayout = rootView.tabl_send_currency

        amountLabelText = rootView.txt_send_amount_label
        amountEditText = rootView.etxt_send_amount
        amountUnitText = rootView.txt_send_amount_unit

        destinationLabelText = rootView.txt_send_destination_label
        destinationEditText = rootView.etxt_send_destination

        twoFactorLabelText = rootView.txt_send_two_factor_label
        twoFactorEditText = rootView.etxt_send_two_factor
        twoFactorButton = rootView.btn_send_two_factor

        scanButton = rootView.btn_send_destination_camera

        warning1TextView = rootView.txt_send_warning
        warning2TextView = rootView.txt_send_warning_2

        sendButton = rootView.btn_send

        titleText.text = "Send" //currency.toString()

        switchCurrency()

        currencyTabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when(tab.position) {
                    0 -> switchCurrency(Currency.BTC)
                    1 -> switchCurrency(Currency.ETH)
                    2 -> switchCurrency(Currency.BCH)
                    3 -> switchCurrency(Currency.LTC)
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        scanButton.onClick { getAddressFromCamera() }

//        radioButtonBtc.isChecked = true
//        radioButtonBtc.setOnClickListener {
//            switchCurrency(Currency.BTC)
//        }
//        radioButtonEth.setOnClickListener {
//            switchCurrency(Currency.ETH)
//        }
//        radioButtonLtc.setOnClickListener {
//            switchCurrency(Currency.LTC)
//        }

        val prefs = Prefs(context!!)

        sendButton.setOnClickListener {
            val amount = amountEditText.text.toString()
            val destination = destinationEditText.text.toString()
            if (amount.isBlank()) {
                toast("Please enter amount to send")
            } else if (destination.isBlank()) {
                toast("Please enter destination address")
            } else if (prefs.shouldShowSendConfirmModal) {
                alert {
                    title = "Send $amount $currency to $destination"

                    positiveButton("Confirm") { submitSend() }
                    negativeButton("Cancel") { }
                }.show()
            } else {
                submitSend()
            }
        }

        doneLoading()

        return rootView
    }

    private fun submitSend() {
        val amount = amountEditText.text.toString().toDoubleOrZero()
        val destination = destinationEditText.text.toString()

        val min = currency.minSendAmount

        if (amount >= min) {
            GdaxApi.sendCrypto(amount, currency, destination).executePost(
                    { result -> //failure
                        val response = result.error.response.data

                        var i = 0
                        val len = response.size
                        while (i < len) {
                            response[i] = java.lang.Byte.parseByte(response[i].toString())
                            i++
                        }

                        var responseDataStr = String(response)
                        responseDataStr = responseDataStr.removePrefix("{\"message\":\"")
                        responseDataStr = responseDataStr.removeSuffix("\"}")
                        val errorString = when(responseDataStr) {
                            "invalid crypto_address" -> "Error: Invalid $currency address"
                            else -> GdaxApi.defaultPostFailure(result)
                        }
                        toast (errorString)
                    },
                    { _ -> //success
                        toast("success")
                    })
        } else {
            toast("error! Trying to send less than minimum which is $min")
//            TransferHub.sendToPayment(10.0, Currency.USD)
        }
    }

//    override fun onResume() {
//        super.onResume()
////        showNavSpinner(ChartFragment.account?.currency) { selectedCurrency ->
////            currency = selectedCurrency
//////                refresh { }
////            switchCurrency()
////        }
//    }

    //TODO: add refresh

    private fun getAddressFromCamera() {
        activity?.let { activity ->
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                // Permission is not granted
                // Ask for camera permission
                ActivityCompat.requestPermissions(activity,
                        arrayOf(Manifest.permission.CAMERA),
                        666)
            } else {
                val intent = Intent(activity, ScanActivity::class.java)
                startActivityForResult(intent, 2)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        var barcode = data.extras.getString("BarCode")

        if (barcode.equals("")) {
            toast("Address not found")
        } else {
            //TODO: parse more advanced qr codes
            destinationEditText.setText(barcode)
        }
    }

    private fun switchCurrency(currency: Currency = this.currency) {
        this.currency = currency

        amountUnitText.text = currency.toString()
        destinationLabelText.text = "Destination ($currency address)"

        context?.let { context ->
            val buttonColors = currency.colorStateList(context)
            val buttonTextColor = currency.buttonTextColor(context)
            sendButton.backgroundTintList = buttonColors
            sendButton.textColor = buttonTextColor

            val tabAccentColor = currency.colorAccent(activity!!)
            currencyTabLayout.setSelectedTabIndicatorColor(tabAccentColor)

            when (currency) {
                Currency.BTC -> {
                    warning1TextView.setText(R.string.send_warning_1_btc)
                    warning2TextView.setText(R.string.send_warning_2_btc)
                }
                Currency.ETH -> {
                    warning1TextView.setText(R.string.send_warning_1_eth)
                    warning2TextView.setText(R.string.send_warning_2_eth)
                }
                Currency.BCH -> {
                    warning1TextView.setText(R.string.send_warning_1_bch)
                    warning2TextView.setText(R.string.send_warning_2_bch)
                }
                Currency.LTC -> {
                    warning1TextView.setText(R.string.send_warning_1_ltc)
                    warning2TextView.setText(R.string.send_warning_2_ltc)
                }
            }
        }
    }

}