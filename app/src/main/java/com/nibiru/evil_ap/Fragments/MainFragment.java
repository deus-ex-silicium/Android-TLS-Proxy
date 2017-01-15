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

import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.proxy.ProxyService;

public class MainFragment extends Fragment implements View.OnClickListener {
    /************************************* CLASS FIELDS *******************************************/
    protected final String TAG = getClass().getSimpleName();
    private OnMainFragmentInteraction mListener;
    private EditText et;
    private EditText et2;
    private CheckBox cb;
    private BroadcastReceiver mApReceiver;

    /************************************** CLASS METHODS *****************************************/
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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_main, container, false);
        Button bt = (Button) v.findViewById(R.id.button);
        bt.setOnClickListener(this);
        et = (EditText) v.findViewById(R.id.editText);
        et2 = (EditText) v.findViewById(R.id.editText2);
        cb = (CheckBox) v.findViewById(R.id.checkBox);
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked){
                        et2.setVisibility(View.VISIBLE);
                    }
                    else{
                        et2.setVisibility(View.INVISIBLE);
                        et2.setText("");
                    }
                }
            }
        );
        return v;
    }

    public void setBtnUI(boolean ApOn) {
        try {
            Button btn = (Button) getView().findViewById(R.id.button);
            if (ApOn) {
                btn.setBackgroundResource(R.drawable.greenontest);
                mListener.enableTabLayout();
                et.setFocusable(false);
                et2.setFocusable(false);
                cb.setEnabled(false);
            }
            else if(!ApOn){
                et.setFocusable(true);
                et2.setFocusable(true);
                et.setFocusableInTouchMode(true);
                et2.setFocusableInTouchMode(true);
                cb.setEnabled(true);
                btn.setBackgroundResource(R.drawable.greenoff);
                mListener.disableTabLayout();

            }
        } catch (NullPointerException e) {
            Log.e(TAG, "findViewById null pointer!");
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.button:
                if (!mListener.isApOn()) {
                    if(cb.isChecked()) {
                        mListener.onApPressed(et.getText().toString(), et2.getText().toString());
                    }
                    else{
                        mListener.onApPressed(et.getText().toString(), null);
                    }
                    Log.e(TAG, et.getText().toString());
                    setBtnUI(true);
                } else if(mListener.isApOn()){
                    mListener.onApPressed("","");
                    setBtnUI(false);
                }
                break;
        }
    }

    private class ApReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            //something about AP changed or notification was pressed
            if (mListener == null) return;
            String action = intent.getAction();
            if (action.equals("android.net.wifi.WIFI_AP_STATE_CHANGED")) {
                setBtnUI(mListener.isApOn());
            }
        }
    }

/******************************** Fragment Stuff **************************************************/
    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     */
    public interface OnMainFragmentInteraction {
        //return true is Ap was turn on, false otherwise
        boolean onApPressed(String SSID, String pass);
        boolean isApOn();
        void enableTabLayout();
        void disableTabLayout();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        // This makes sure that the container activity has implemented
        // the callback interface. If not, it throws an exception
        if (context instanceof OnMainFragmentInteraction) {
            mListener = (OnMainFragmentInteraction) context;
            // set up broadcast receiver and register filter
            mApReceiver = new ApReceiver();
            IntentFilter filter = new IntentFilter();
            filter.addAction("android.net.wifi.WIFI_AP_STATE_CHANGED");
            getActivity().getApplicationContext().registerReceiver(mApReceiver, filter);
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteraction interface");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        getActivity().getApplicationContext().unregisterReceiver(mApReceiver);
        mListener = null;
    }

}
