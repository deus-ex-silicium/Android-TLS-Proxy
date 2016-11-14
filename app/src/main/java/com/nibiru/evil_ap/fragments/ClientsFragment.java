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

import com.nibiru.evil_ap.Client;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.adapters.clients_adapter;

import java.util.ArrayList;


public class ClientsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private OnFragmentInteractionListener mListener;
    private ListView clients_listView;
    private clients_adapter customAdapter;
    private View rootView;
    SwipeRefreshLayout mySwipeRefreshLayout;
    private ArrayList<Client> clientsList;
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
        clientsList = getClients();
        customAdapter = new clients_adapter(getActivity().getApplicationContext(), R
                .layout.list_item_clients, clientsList,this.getActivity());
        mySwipeRefreshLayout = (SwipeRefreshLayout) rootView.findViewById(R.id.swipeLayout);
        mySwipeRefreshLayout.setOnRefreshListener(this);
        clients_listView.setAdapter(customAdapter);
        Log.e("adapter",customAdapter.getItem(0).getIp());
        Log.e("LIST!: ",clientsList.get(0).getIp());


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
    public ArrayList<Client> getClients(){
        Client c = new Client("23.23.12.53","fdgdsf2");
        Client c2 = new Client("23.23.12.54","fdgddssf2");
        ArrayList<Client> k = new ArrayList<Client>();
        for(int i = 0; i < 20 ; i ++) {
            k.add(c);
            k.add(c2);
        }
        return k;
    }
    public ArrayList<Client> getClientss(){
        Client c = new Client("23.23.12.51","fdgdsf2");
        Client c2 = new Client("23.23.12.52","fdgddssf2");
        ArrayList<Client> k = new ArrayList<Client>();
        for(int i = 0; i < 20 ; i ++) {
            k.add(c);
            k.add(c2);
        }
        return k;
    }
    @Override
    public void onRefresh() {
        Log.e("Refresh", "SMASHING!");
        clientsList = getClientss();
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
