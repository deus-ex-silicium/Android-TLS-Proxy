package com.nibiru.evil_ap.fragments;

import android.media.Image;
import android.support.v4.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.R;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment {
    /**************************************CLASS FIELDS********************************************/
    private final static String TAG = "MainFragment";
    private Context ctx;
    private OnFragmentInteractionListener mListener;
    /**************************************CLASS METHODS*******************************************/
    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = getActivity().getApplicationContext();
        // only for marshmallow and newer versions, we need user to explicitly grant us WRITE_SETTINGS
        // permissions to be able to change hotspot configuration
        //TODO: what about other versions ? FIX NEEDED
        //http://stackoverflow.com/questions/32083410/cant-get-write-settings-permission/32083622#32083622
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.System.canWrite(ctx)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + ctx.getPackageName()));
            startActivity(intent);
        }
        //setBtnUI(Ap.isApOn(ctx));

        //Register BroadcastReceiver, filer specific intents
        ctx.registerReceiver(new ApBroadcastReceiver(),
                new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        return v;
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

    public void setBtnUI(boolean ApOn) {
        try {
            Button btn = (Button) getView().findViewById(R.id.button);
            if (ApOn)
                btn.setBackgroundResource(R.drawable.onoffon);
            else
                btn.setBackgroundResource(R.drawable.onoff);
        }
        catch (NullPointerException e){
            Log.e(TAG, "findViewById null pointer!");
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

    private class ApBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //something about AP changed so update the UI button
            boolean isApOn = Ap.isApOn(ctx);
            setBtnUI(isApOn);
        }
    }
}
