package com.nibiru.evilap.adapters

import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.View
import android.view.ViewGroup
import com.nibiru.evilap.R.layout.rv_host_item_row
import com.nibiru.evilap.inflate
import kotlinx.android.synthetic.main.rv_host_item_row.view.*

// https://www.raywenderlich.com/170075/android-recyclerview-tutorial-kotlin
class HostsAdapter(private val hosts: ArrayList<Pair<String, String>>) : RecyclerView.Adapter<HostsAdapter.HostHolder>()  {
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
        private var host: Pair<String, String>? = null

        init {
            v.setOnClickListener(this)
        }

        override fun onClick(v: View) {
            Log.d("HostsAdapter", "CLICK!")
        }

        fun bindHost(host: Pair<String, String>) {
            this.host = host
            view.rvHostIP.text = host.first
            view.rvHostMAC.text = host.second
        }

        companion object {
            private val PHOTO_KEY = "PHOTO"
        }
    }


}