package com.nibiru.evil_ap.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.Presenter;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.adapters.server_adapter;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.DatabaseManager;
import com.nibiru.evil_ap.log.LogEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ServerItemFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private ListView server_listView;
    private server_adapter customAdapter;
    private View rootView;
    private Client clientLocal;
    private onClientsFragmentInteraction mListener;
    private IMVP.PresenterOps mPresenter;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ArrayList<String> serverList;
    private ArrayList<String> serverListCount;
    /**************************************CLASS METHODS*******************************************/
    public ServerItemFragment() {
        // Required empty public constructor
    }

    public void initialize(Client client){
        clientLocal = client;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        rootView = inflater.inflate(R.layout.fragment_serveritem_list, container, false);
        server_listView = (ListView) rootView.findViewById(R.id.Serverlist);
        final ListView server_listView = (ListView) rootView.findViewById(R.id.Serverlist);
        serverListCount = new ArrayList<>();
        serverList = getClientServers();
        customAdapter = new server_adapter(getActivity().getApplicationContext(),
                R.layout.fragment_serveritem, serverList, serverListCount , this.getActivity(),
                clientLocal);
        mySwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        server_listView.setAdapter(customAdapter);
        // Inflate the layout for this fragment
        return rootView;
    }

    public ArrayList<String> getClientServers(){
        ArrayList<String> distinctHosts = new ArrayList<>();
        ArrayList<String> allHosts = new ArrayList<>();
        if(mPresenter.getClientLog(clientLocal).size()!= 0){
            allHosts = createHosts(mPresenter.getClientLog(clientLocal));
        List<LogEntry> le = mPresenter.getClientLog(clientLocal);
        for (LogEntry e:le
             ) {
            if(!distinctHosts.contains(e.getHost())) {
                distinctHosts.add(e.getHost());
                serverListCount.add("Count:"+ Collections.frequency(allHosts,e
                        .getHost()));
            }
        }}
        else {
            distinctHosts.add("No entries");
            serverListCount.add("Count: null");
        }
        return distinctHosts;
    }

    private ArrayList<String> createHosts (List<LogEntry> al){
        ArrayList<String> temp = new ArrayList<>();
        for (LogEntry le:al
             ) {
            temp.add(le.getHost());
        }
        return temp;
    }

    public void onRefresh() {
        Log.d(TAG, "Refreshing!");
        serverList = getClientServers();
        customAdapter = new server_adapter(getActivity().getApplicationContext(),
                R.layout.fragment_serveritem, serverList, serverListCount,this.getActivity(), clientLocal);
        server_listView.setAdapter(customAdapter);
        mySwipeRefreshLayout.setRefreshing(false);
    }

/******************************** Fragment Stuff **************************************************/
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface onClientsFragmentInteraction {
        IMVP.PresenterOps getPresenter();
    }
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onClientsFragmentInteraction) {
            mListener = (onClientsFragmentInteraction) context;
            mPresenter = mListener.getPresenter();
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteraction interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }
}
