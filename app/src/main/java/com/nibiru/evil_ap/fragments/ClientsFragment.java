package com.nibiru.evil_ap.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.adapters.clients_adapter;
import com.nibiru.evil_ap.log.Client;

import java.util.ArrayList;


public class ClientsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {
    /**************************************CLASS FIELDS********************************************/
    private final static String TAG = "ClientsFragment";
    private OnFragmentInteractionListener mListener;
    private ListView clients_listView;
    private clients_adapter customAdapter;
    private View rootView;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ArrayList<Client> clientsList;
    /**************************************CLASS METHODS*******************************************/
    public ClientsFragment() {
        // Required empty public constructor
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_clients, container, false);
        clients_listView = (ListView) rootView.findViewById(R.id.listk);
        final ListView clients_listView = (ListView) rootView.findViewById(R.id
                .listk);
        clientsList = getCurrentClients();
        customAdapter = new clients_adapter(getActivity().getApplicationContext(), R
                .layout.list_item_clients, clientsList,this.getActivity());
        mySwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        clients_listView.setAdapter(customAdapter);
        // Inflate the layout for this fragment
        return rootView;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public ArrayList<Client> getCurrentClients(){
        ArrayList<Client> clients = new ArrayList<>(10);
        ArrayList<String> output = Root.RunAsRootWithOutput("ip -4 neigh");
        for (String line : output) {
            String[] split = line.split(" +");
            //IP idx = 0 , MAC idx = 4, flags idx = 5
            if (split.length == 6 && (split[5].equals("REACHABLE") || split[5].equals("STALE")) ) {
                clients.add(new Client(split[0], split[4]));
            }
        }
        return clients;
    }
    @Override
    public void onRefresh() {
        Log.d(TAG, "Refreshing!");
        clientsList = getCurrentClients();
        customAdapter = new clients_adapter(getActivity().getApplicationContext(), R
                .layout.list_item_clients, clientsList,this.getActivity());
        clients_listView.setAdapter(customAdapter);
        mySwipeRefreshLayout.setRefreshing(false);
    }

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
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
