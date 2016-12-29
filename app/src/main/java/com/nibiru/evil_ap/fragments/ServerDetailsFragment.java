package com.nibiru.evil_ap.fragments;

import android.app.Fragment;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nibiru.evil_ap.R;

import java.util.ArrayList;

public class ServerDetailsFragment extends Fragment {

    private OnFragmentInteractionListener mListener;
    private ArrayList<String> server_arrayl = new ArrayList<>();
    private View rootView;
    private TextView displayDetails;
    private String serverLocal;
    private String clientLocal;
    LinearLayout ll;
    public ServerDetailsFragment() {
        // Required empty public constructor
    }
    public ServerDetailsFragment(String server, String client){
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
        displayDetails.setText(server_arrayl.get(0));
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        rootView = inflater.inflate(R.layout.fragment_server_details, container, false);
        displayDetails = (TextView) rootView.findViewById(R.id.displayDetails);
        // Inflate the layout for this fragment
        return rootView;
    }
    void getServerDetails(String client){
        server_arrayl.add("tempString");
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
