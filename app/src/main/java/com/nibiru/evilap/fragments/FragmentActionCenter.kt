package com.nibiru.evilap.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R
import kotlinx.android.synthetic.main.fragment_action_center.view.*


class FragmentActionCenter: android.support.v4.app.Fragment(){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var lbm: LocalBroadcastManager? = null
    private val ClientModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "Got intent, action = " + intent?.action)
        }
    }
    /**************************************CLASS METHODS*******************************************/
    companion object { // never use fragment constructors with args, AOS will not use them
        fun newInstance(): FragmentActionCenter = FragmentActionCenter()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context == null) return
        lbm = LocalBroadcastManager.getInstance(context)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v= inflater.inflate(R.layout.fragment_action_center, container, false)
        v.bDnsSniff.setOnClickListener { _ ->
            Log.d(TAG, "DNS SNIFF")
            val executeIntent = Intent(EvilApService.ACTION_DNS_SNIFF)
            executeIntent.setClass(context, EvilApService::class.java)
            context?.startService(executeIntent)
        }
        return v
    }
}