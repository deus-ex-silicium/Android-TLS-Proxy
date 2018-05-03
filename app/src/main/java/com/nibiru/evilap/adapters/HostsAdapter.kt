package com.nibiru.evilap.adapters

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.nibiru.evilap.EvilApService
import com.nibiru.evilap.R.layout.rv_host_item_row
import com.nibiru.evilap.RxEventBus
import com.nibiru.evilap.inflate
import kotlinx.android.synthetic.main.rv_host_item_row.view.*

// https://www.raywenderlich.com/170075/android-recyclerview-tutorial-kotlin
class HostsAdapter(private val hosts: ArrayList<EvilApService.Host>) : RecyclerView.Adapter<HostsAdapter.HostHolder>()  {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HostHolder{
        val inflatedView = parent.inflate(rv_host_item_row, false)
        return HostHolder(inflatedView)
    }

    override fun getItemCount(): Int = hosts.size

    override fun onBindViewHolder(holder: HostHolder, position: Int) {
        val hostItem = hosts[position]
        holder.bindHost(hostItem)
    }

    class HostHolder(v: View) : RecyclerView.ViewHolder(v), View.OnClickListener {
        private var view: View = v
        private lateinit var host: EvilApService.Host

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            Log.d("HostsAdapter", "CLICK!")
            view.rvCheckBox.isChecked = !view.rvCheckBox.isChecked
            host.present = view.rvCheckBox.isChecked
            RxEventBus.INSTANCE.busCheckedHosts.onNext(host)
        }

        fun bindHost(host: EvilApService.Host) {
            this.host = host
            view.rvHostIP.text = host.ip
            view.rvHostMAC.text = host.mac
        }
    }


}