package com.nibiru.evilap.fragments

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import android.view.*
import com.nibiru.evilap.EventBusRx
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R
import com.nibiru.evilap.adapters.HostsAdapter
import io.reactivex.disposables.Disposable
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
    private lateinit var disposable: Disposable
    /**************************************CLASS METHODS*******************************************/
    companion object { // never use fragment constructors with args, AOS will not use them
        fun newInstance(): FragmentNetwork = FragmentNetwork()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context == null) return
        if(context is OnFragmentInteractionListener)
            mListener = context
        disposable = EventBusRx.INSTANCE.toObserverable().subscribe({
            Log.d(TAG, "$it")
            mHostList.add(it)
            mAdapter.notifyItemChanged(mHostList.size)
        })
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
        disposable.dispose()
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

    interface OnFragmentInteractionListener {
        fun getCurrentClients(): List<Pair<String,String>>
    }

}