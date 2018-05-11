package com.nibiru.evilap.fragments

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R
import com.nibiru.evilap.RxEventBus
import kotlinx.android.synthetic.main.fragment_action_center.view.*


class FragmentActionCenter: android.support.v4.app.Fragment(){
    /**************************************CLASS FIELDS********************************************/
    private val TAG = javaClass.simpleName
    private var mListener: OnFragmentInteractionListener? = null
    /**************************************CLASS METHODS*******************************************/
    companion object { // never use fragment constructors with args, AOS will not use them
        fun newInstance(): FragmentActionCenter = FragmentActionCenter()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if(context == null) return
        if(context is OnFragmentInteractionListener)
            mListener = context
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val v= inflater.inflate(R.layout.fragment_action_center, container, false)
        v.sArpSpoof.setOnClickListener {
            RxEventBus.INSTANCE.send2BackEnd(EvilApService.EventArpSpoof(v.sArpSpoof.isChecked))
        }
        v.sHttpProxy.setOnClickListener {
            RxEventBus.INSTANCE.send2BackEnd(EvilApService.EventHttpProxy(v.sHttpProxy.isChecked))
        }
        v.sCaptivePortal.setOnClickListener {
            RxEventBus.INSTANCE.send2BackEnd(EvilApService.EventCaptivePortal(v.sCaptivePortal.isChecked))
        }
        return v
    }

    interface OnFragmentInteractionListener {

    }
}