package com.nibiru.evilap.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R
import com.nibiru.evilap.adapters.HostsAdapter
import kotlinx.android.synthetic.main.fragment_network.view.*


class FragmentNetwork: android.support.v4.app.Fragment(){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var mListener: OnFragmentInteractionListener? = null
    private var lbm: LocalBroadcastManager? = null
    private val ClientModeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.i(TAG, "Got intent, action = " + intent?.action)
        }
    }
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: HostsAdapter
    private var mHostList: ArrayList<Pair<String,String>> = ArrayList()
    /**************************************CLASS METHODS*******************************************/
    companion object { // never use fragment constructors with args, AOS will not use them
        fun newInstance(): FragmentNetwork = FragmentNetwork()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context == null) return
        if(context is OnFragmentInteractionListener)
            mListener = context
        lbm = LocalBroadcastManager.getInstance(context)

    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        setHasOptionsMenu(true)
        mLinearLayoutManager = LinearLayoutManager(context)
        mAdapter = HostsAdapter(mHostList)
        view.rvHosts.layoutManager = mLinearLayoutManager
        view.rvHosts.adapter = mAdapter
        return view
    }

    override fun onStart() {
        super.onStart()
        if(mHostList.size!=0) return
        val fu = mListener?.getCurrentClients() ?: return
        mHostList.addAll(fu)
        mAdapter.notifyItemInserted(mHostList.size)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.network, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_scan -> {
            val executeIntent = Intent(EvilApService.ACTION_SCAN_ACTIVE)
            executeIntent.setClass(context, EvilApService::class.java)
            context?.startService(executeIntent)

            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun runOnUiThread(f: () -> Unit){
        val handler = Handler()
        val r = Runnable { f() }
        handler.post(r)
    }

    interface OnFragmentInteractionListener {
        fun getCurrentClients(): List<Pair<String,String>>
    }
}