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
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.nibiru.evil_ap.MainActivity;
import com.nibiru.evil_ap.manager.Ap;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.proxy.ProxyService;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link MainFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link MainFragment#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements View.OnClickListener {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    private final static String TAG = "MainFragment";
    private Context ctx;
    private OnFragmentInteractionListener mListener;
    private EditText et;
    private EditText et2;
    /**************************************
     * CLASS METHODS
     *******************************************/
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
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
        Button bt = (Button) v.findViewById(R.id.button);
        bt.setOnClickListener(this);
        et = (EditText)v.findViewById(R.id.editText);
        et2 = (EditText)v.findViewById(R.id.editText2);
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
        } catch (NullPointerException e) {
            Log.e(TAG, "findViewById null pointer!");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                boolean isApOn = Ap.isApOn(getActivity().getApplicationContext());
                if(!isApOn){
                    if(et.getText().toString().equals("")||et2.getText().toString().equals(""))
                    {
                        Ap.turnOnAp("AP", "pa$$word", getActivity().getApplicationContext());
                    }
                    else
                    {
                        Ap.turnOnAp(et.getText().toString(),
                                et2.getText().toString(),
                                getActivity().getApplicationContext());
                    }
                    et.setFocusable(false);
                    et2.setFocusable(false);
                    //startService(new Intent(this, ProxyService.class));
                    //routingMan.redirectHTTP(rootMan, true);
                    //routingMan.redirectHTTPS(rootMan, true);
                    //routingMan.redirectDNS(rootMan, true);
                }
                else {
                    Ap.turnOffAp(getActivity().getApplicationContext());
                    getActivity().stopService(new Intent(getActivity().getApplicationContext(), ProxyService.class));
                    et.setFocusableInTouchMode(true);
                    et2.setFocusableInTouchMode(true);
                    //routingMan.redirectHTTP(rootMan, false);
                    //routingMan.redirectHTTPS(rootMan, false);
                    //routingMan.redirectDNS(rootMan, false);
                }
                break;
        }
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
