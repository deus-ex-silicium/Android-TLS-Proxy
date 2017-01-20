package com.nibiru.evil_ap.fragments;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.adapters.serverDetails_adapter;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.LogEntry;

import java.util.ArrayList;

public class ServerDetailsFragment extends Fragment{

    private OnFragmentInteractionListener mListener;
    private View rootView;
    private ListView logs_listView;
    private IMVP.PresenterOps mPresenter;
    private String serverLocal;
    private serverDetails_adapter customAdapter;
    private Client clientLocal;
    private ArrayList<LogEntry> logList;

    public ServerDetailsFragment() {
        // Required empty public constructor
    }
    public void initialize(String server, Client client){
        serverLocal = server;
        clientLocal = client;
    }
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }
    public void onResume(){
        super.onResume();
        getServerDetails(clientLocal);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_server_details, container, false);
        logs_listView = (ListView) rootView.findViewById(R.id.DetailList);
        logs_listView.setVerticalScrollBarEnabled(true);
        final ListView logs_listView = (ListView) rootView.findViewById(R.id.DetailList);
        logList = getServerDetails(clientLocal);
        customAdapter = new serverDetails_adapter(getActivity().getApplicationContext(),
                R.layout.fragment_server_details_item, logList,this.getActivity());
        logs_listView.setAdapter(customAdapter);
        // Inflate the layout for this fragment
        return rootView;
    }
    private ArrayList<LogEntry> getServerDetails(Client client){
        ArrayList<LogEntry> le = new ArrayList<>();
        if(mPresenter.getClientLog(client).size()!=0){
        for (LogEntry e:mPresenter.getClientLog(client)
             ) {
            if(e.getHost().equals(serverLocal)){
                le.add(e);
            }
        }}
        else{
            le.add(new LogEntry(0,"No entries","No entries","POST\n entries"));
            le.add(new LogEntry(0,"No entries","No entries","GET\n entries"));
        }

        return le;
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
            mPresenter = mListener.getPresenter();

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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnFragmentInteractionListener {
        void onFragmentInteraction(Uri uri);
        IMVP.PresenterOps getPresenter();
    }
}
