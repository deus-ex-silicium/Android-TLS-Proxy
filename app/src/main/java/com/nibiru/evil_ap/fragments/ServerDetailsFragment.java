package com.nibiru.evil_ap.fragments;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.adapters.serverDetails_adapter;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.log.DatabaseManager;
import com.nibiru.evil_ap.log.LogEntry;

import java.util.ArrayList;

public class ServerDetailsFragment extends Fragment implements IMVP.RequiredViewOps{

    private OnFragmentInteractionListener mListener;
    private View rootView;
    private ListView logs_listView;
    private IMVP.PresenterOps mPresenter;
    private String serverLocal;
    private serverDetails_adapter customAdapter;
    private Client clientLocal;
    private ArrayList<LogEntry> logList;
    LinearLayout ll;
    public ServerDetailsFragment() {
        // Required empty public constructor
    }
    public ServerDetailsFragment(String server, Client client){
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
        customAdapter = new serverDetails_adapter(getContext(),
                R.layout.fragment_server_details_item, logList,this.getActivity());
        logs_listView.setAdapter(customAdapter);
        // Inflate the layout for this fragment
        return rootView;
    }
    public ArrayList<LogEntry> getServerDetails(Client client){
        ArrayList<LogEntry> le = new ArrayList<>();
        if(mPresenter.getClientLog(clientLocal).size()!=0){
        for (LogEntry e:mPresenter.getClientLog(clientLocal)
             ) {
            if(e.getHost().equals(serverLocal)){
                le.add(e);
            }
        }}
        else{
            le.add(new LogEntry(0,"No entries","No entries","No entries"));
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

    @Override
    public void showToast(String msg) {

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
        IMVP.PresenterOps getPresenter();
    }
}
