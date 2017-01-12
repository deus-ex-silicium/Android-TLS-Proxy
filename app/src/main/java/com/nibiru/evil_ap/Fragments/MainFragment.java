package com.nibiru.evil_ap.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;

import com.nibiru.evil_ap.R;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link OnMainFragmentInteraction} interface
 * to handle interaction events.
 * Use the {@link MainFragment#//newInstance} factory method to
 * create an instance of this fragment.
 */
public class MainFragment extends Fragment implements View.OnClickListener {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    protected final String TAG = getClass().getSimpleName();
    private OnMainFragmentInteraction mListener;
    private BroadcastReceiver mApChangeReceiver;
    private EditText et;
    private EditText et2;

    /***************************************
     * CLASS METHODS
     ******************************************/
    public MainFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // only for marshmallow and newer versions, we need user to explicitly grant us
        // WRITE_SETTINGS permissions to be able to change hotspot configuration
        //TODO: what about other versions ? FIX NEEDED
        //http://stackoverflow.com/questions/32083410/cant-get-write-settings-permission/32083622#32083622
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.System.canWrite(getContext())) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getContext().getPackageName()));
            startActivity(intent);
        }
    }

    @Override //unregister broadcast receiver for WIFI_AP_STATE_CHANGED
    public void onStop() {
        super.onStop();
        getContext().unregisterReceiver(mApChangeReceiver);
    }

    @Override //register broadcast receiver for WIFI_AP_STATE_CHANGED
    public void onStart() {
        super.onStart();
        mApChangeReceiver = new ApBroadcastReceiver();
        getContext().registerReceiver(mApChangeReceiver,
                new IntentFilter("android.net.wifi.WIFI_AP_STATE_CHANGED"));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        Button bt = (Button) v.findViewById(R.id.button);
        bt.setOnClickListener(this);
        et = (EditText) v.findViewById(R.id.editText);
        et2 = (EditText) v.findViewById(R.id.editText2);
        CheckBox cb = (CheckBox) v.findViewById(R.id.checkBox);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        et2.setVisibility(View.VISIBLE);
                    }
                    else{
                        et2.setVisibility(View.INVISIBLE);
                    }
                }
            }
        );
        return v;
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
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                if (mListener.onApPressed(et.getText().toString(), et2.getText().toString())) {
                    et.setFocusable(false);
                    et2.setFocusable(false);
                    setBtnUI(true);
                } else {
                    et.setFocusable(true);
                    et2.setFocusable(true);
                    setBtnUI(false);
                }
                break;
        }
    }

    private class ApBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //something about AP changed so update the UI button
            if (mListener == null) return;
            setBtnUI(mListener.isApOn());
            //TODO: what about iptables rules and redirection?
        }
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
    public interface OnMainFragmentInteraction {
        //return true is Ap was turn on, false otherwise
        boolean onApPressed(String SSID, String pass);

        boolean isApOn();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (context instanceof OnMainFragmentInteraction) {
            mListener = (OnMainFragmentInteraction) context;
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
