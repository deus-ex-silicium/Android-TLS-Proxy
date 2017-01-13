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
import java.util.List;

public class ServerItemFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener,
        IMVP.RequiredViewOps {

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
    /**************************************CLASS METHODS*******************************************/
    public ServerItemFragment() {
        // Required empty public constructor
    }
    public ServerItemFragment(Client client) {
        clientLocal = client;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        rootView = inflater.inflate(R.layout.fragment_serveritem_list, container, false);
        server_listView = (ListView) rootView.findViewById(R.id.Serverlist);
        final ListView server_listView = (ListView) rootView.findViewById(R.id.Serverlist);
        serverList = getClientServers();
        customAdapter = new server_adapter(getActivity().getApplicationContext(),
                R.layout.fragment_serveritem, serverList, this.getActivity(), clientLocal);
        mySwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        server_listView.setAdapter(customAdapter);
        // Inflate the layout for this fragment
        return rootView;
    }

    public ArrayList<String> getClientServers(){
        ArrayList<String> x = new ArrayList<>();
        if(mPresenter.getClientLog(clientLocal)!= null){
        List<LogEntry> le = mPresenter.getClientLog(clientLocal);
        for (LogEntry e:le
             ) {
            if(!x.contains(e.getHost())) {
                x.add(e.getHost());
            }
        }}
        else {
            x.add("joemonster.org");
        }
        x.add("joemonster.org");
        return x;
    }


    public void onRefresh() {
        Log.d(TAG, "Refreshing!");
        serverList = getClientServers();
        customAdapter = new server_adapter(getActivity().getApplicationContext(),
                R.layout.fragment_serveritem, serverList,this.getActivity(), clientLocal);
        server_listView.setAdapter(customAdapter);
        mySwipeRefreshLayout.setRefreshing(false);
    }

    @Override
    public void showToast(String msg) {

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
        ArrayList<String> getClientServers();
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
