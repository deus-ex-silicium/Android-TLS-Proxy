package com.nibiru.evilap.fragments

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R
import com.nibiru.evilap.RxEventBus
import com.nibiru.evilap.adapters.HostsAdapter
import io.reactivex.disposables.Disposable
import kotlinx.android.synthetic.main.fragment_network.*
import kotlinx.android.synthetic.main.fragment_network.view.*


// https://www.toptal.com/android/functional-reactive-android-rxjava
// https://stackoverflow.com/questions/26140026/service-fragment-communication
class FragmentNetwork: android.support.v4.app.Fragment(){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var mListener: OnFragmentInteractionListener? = null
    private lateinit var mLinearLayoutManager: LinearLayoutManager
    private lateinit var mAdapter: HostsAdapter
    private var mHostList: ArrayList<EvilApService.Host> = ArrayList()
    private var disposable: Disposable? = null
    /**************************************CLASS METHODS*******************************************/
    companion object { // never use fragment constructors with args, AOS will not use them
        fun newInstance(): FragmentNetwork = FragmentNetwork()
    }

    private fun setupEventBus(){
        if (disposable != null && !disposable!!.isDisposed) return
        disposable = RxEventBus.INSTANCE.getFrontEndObservable().subscribe({
            Log.d(TAG, "$it")
            when(it) {
                is EvilApService.EventScannedHosts -> {
                    mHostList.addAll(it.hosts)
                    mAdapter.notifyItemChanged(mHostList.size)
                    rvSwipeRef.isRefreshing = false
                }
            }

        })
    }

    private fun onSettingsDialog(){
        if(context==null) return
        val dialogBuilder = AlertDialog.Builder(context!!)
        val dialogView = this.layoutInflater.inflate(R.layout.dialog_scanner_settings, null)
        dialogBuilder.setView(dialogView)
        dialogBuilder.setTitle("Scanner settings")
        //TODO: scanner settings
        dialogBuilder.setPositiveButton("Save") { dialog, whichButton ->

        }
        dialogBuilder.setNegativeButton("Cancel") { dialog, whichButton ->

        }
        dialogBuilder.create().show()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context == null) return
        if(context is OnFragmentInteractionListener)
            mListener = context
        setupEventBus()
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
        disposable?.dispose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState);
        // If we have a saved state then we can restore it now
        if (savedInstanceState == null) return
        mHostList = savedInstanceState.getSerializable("mHostList") as ArrayList<EvilApService.Host>
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_network, container, false)
        setHasOptionsMenu(true)
        mLinearLayoutManager = LinearLayoutManager(context)
        mAdapter = HostsAdapter(mHostList)
        view.rvHosts.layoutManager = mLinearLayoutManager
        view.rvHosts.adapter = mAdapter
        view.rvSwipeRef.setOnRefreshListener { doScan() }
        return view
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putSerializable("mHostList", mHostList)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.network_fragment, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem) = when (item.itemId) {
        R.id.menu_scan -> {
            doScan()
            true
        }
        R.id.menu_settings -> {
            onSettingsDialog()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    private fun doScan(){
        Log.d(TAG, "REFRESHING:${rvSwipeRef.isRefreshing}")
        rvSwipeRef.isRefreshing = true
        mAdapter.notifyItemRangeChanged(0, mHostList.size)
        mHostList.clear()
        RxEventBus.INSTANCE.send2BackEnd(EvilApService.EventActiveScan("ARP"))
    }

    interface OnFragmentInteractionListener {

    }

}