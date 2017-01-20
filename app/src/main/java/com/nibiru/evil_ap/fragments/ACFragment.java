package com.nibiru.evil_ap.fragments;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Switch;

import com.nibiru.evil_ap.ConfigTags;
import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.R;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class ACFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener {
    /**************************************CLASS FIELDS********************************************/
    protected final String TAG = getClass().getSimpleName();
    private onAcFragmentInteraction mListener;
    private IMVP.PresenterOps mPresenter;
    private LinearLayout mLayout;
    private LinearLayout mLayout2;
    private boolean layotPayloadflag = false;
    private boolean layoutImageflag = false;
    private ImageView iv;
    /**************************************CLASS METHODS*******************************************/
    public ACFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onStart(){
        super.onStart();
        checkSwitches();
    }

    @Override
    public void onResume(){
        super.onResume();
        checkSwitches();
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_achttp, container, false);
        Button b = (Button) v.findViewById(R.id.button_injectHTML);
        Button bb = (Button) v.findViewById(R.id.button_replaceImages);
        b.setOnClickListener(this);
        bb.setOnClickListener(this);
        Switch switchRedirectHTTP = (Switch) v.findViewById(R.id.switch1);
        Switch switchRedirectHTTPS = (Switch) v.findViewById(R.id.switchs);
        Switch switchInjectJS = (Switch) v.findViewById(R.id.switch2);
        Switch switchSSLStrip = (Switch) v.findViewById(R.id.switch3);
        Switch switchReplacImages = (Switch) v.findViewById(R.id.switch4);
        mLayout = (LinearLayout) v.findViewById(R.id.lin2dynamic);
        mLayout2 = (LinearLayout) v.findViewById(R.id.lin4dynamic);
        iv = new ImageView(v.getContext());
        switchRedirectHTTP.setOnCheckedChangeListener(this);
        switchRedirectHTTPS.setOnCheckedChangeListener(this);
        switchInjectJS.setOnCheckedChangeListener(this);
        switchSSLStrip.setOnCheckedChangeListener(this);
        switchReplacImages.setOnCheckedChangeListener(this);
        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_injectHTML:
                if (!layotPayloadflag) {
                    layotPayloadflag = true;
                    Log.e("E!", "inject");
                    final CheckBox cb = new CheckBox(view.getContext());
                    cb.setText("Payload1 - Alert dialog box message");
                    final EditText et = new EditText(view.getContext());
                    et.setText("Hello from Evil-AP!");
                    mLayout.addView(cb);
                    mLayout.addView(et);
                    /*CheckBox cb2 = new CheckBox(view.getContext());
                    cb2.setText("Payload2");
                    EditText et2 = new EditText(view.getContext());
                    EditText et3 = new EditText(view.getContext());
                    et2.setHint("IP");
                    et3.setHint("Port");*/
                    Button apply = new Button(view.getContext());
                    apply.setText("Apply");
                    apply.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            List<Pair<Integer, String>> payloads = new ArrayList<>();
                            if(cb.isChecked()){
                                Pair<Integer, String> p1 = new Pair<>(1, et.getText().toString());
                                payloads.add(p1);
                            }
                            mListener.onJsPayloadApply(payloads);
                            layotPayloadflag = !layotPayloadflag;
                            mLayout.removeAllViews();
                        }
                    });
                    /*mLayout.addView(cb2);
                    mLayout.addView(et2);
                    mLayout.addView(et3);*/
                    mLayout.addView(apply);
                    break;
                } else {
                    layotPayloadflag = false;
                    mLayout.removeAllViews();
                    break;
                }
            case R.id.button_replaceImages:
                if (!layoutImageflag) {
                    layoutImageflag = true;
                    String imgpath = mPresenter.getSharedPrefsString(ConfigTags.imgPath.toString());
                    if (imgpath == null || imgpath.equals("")) {
                        Log.e("SP is null - ", imgpath);

                    } else {
                        applyToImageView(mLayout2, iv, Uri.parse(imgpath));
                    }

                    Log.e("E!", "images");
                    Button chooseImage_button = new Button(view.getContext());
                    iv = new ImageView(view.getContext());

                    chooseImage_button.setText("choose image");
                    chooseImage_button.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
                        }
                    });
                    mLayout2.addView(chooseImage_button);
                    break;
                } else {
                    layoutImageflag = false;
                    mLayout2.removeAllViews();
                    break;
                }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    applyToImageView(mLayout2, iv, selectedImage);
                    //commit configuration change
                    mListener.onImgReplaceChosen(selectedImage);
                }
                break;
        }
    }

    private void applyToImageView(LinearLayout layout, ImageView iv, Uri image) {
        if(layout.getChildCount() > 1){
            layout.removeViewAt(0);
        }
        layout.addView(iv, 0);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
        lp.width = 500;
        lp.height = 500;
        lp.gravity = Gravity.CENTER;
        iv.setImageURI(image);
    }

    private void checkSwitches() {
        if (mPresenter == null) return;
            ((Switch) mListener.getView(R.id.switch3)).setChecked(mPresenter.getSharedPrefsBool
                    (ConfigTags.sslStrip.toString()));
            ((Switch) mListener.getView(R.id.switch4)).setChecked(mPresenter.getSharedPrefsBool
                    (ConfigTags.imgReplace.toString()));
            ((Switch) mListener.getView(R.id.switch1)).setChecked(mPresenter.getSharedPrefsBool
                    (ConfigTags.httpRedirect.toString()));
            ((Switch) mListener.getView(R.id.switch2)).setChecked(mPresenter.getSharedPrefsBool
                    (ConfigTags.jsInject.toString()));
            ((Switch) mListener.getView(R.id.switchs)).setChecked(mPresenter.getSharedPrefsBool
                    (ConfigTags.httpsRedirect.toString()));
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {

            case R.id.switch1:
                Log.e("Switch - ", "redirect http " + isChecked);
                mListener.onTrafficRedirect("HTTP", isChecked);
                mListener.onSwitchToggle(isChecked, ConfigTags.httpRedirect.toString());
                changeSwitch((Switch) getActivity().findViewById(R.id.switch2));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch3));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch4));
                break;
            case R.id.switchs:
                Log.e("Switch - ", "redirect https " + isChecked);
                mListener.onTrafficRedirect("HTTPS", isChecked);
                mListener.onSwitchToggle(isChecked, ConfigTags.httpsRedirect.toString());
                break;
            case R.id.switch2:
                Log.e("Switch - ", "inject " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onSwitchToggle(isChecked, ConfigTags.jsInject.toString());
                    //add default payload
                    List<Pair<Integer, String>> payloads = new ArrayList<>();
                    Pair<Integer, String> p1 = new Pair<>(1, "Hello from Evil-AP!");
                    payloads.add(p1);
                    mListener.onJsPayloadApply(payloads);
                } else {
                    ((Switch) mListener.getView(R.id.switch2)).setChecked(false);
                }
                break;
            case R.id.switch3:
                Log.e("Switch - ", "strip " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onSwitchToggle(isChecked, ConfigTags.sslStrip.toString());
                } else {
                    ((Switch) mListener.getView(R.id.switch3)).setChecked(false);
                }
                break;
            case R.id.switch4:
                Log.e("Switch - ", "images " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onSwitchToggle(isChecked, ConfigTags.imgReplace.toString());
                } else {
                    ((Switch) mListener.getView(R.id.switch4)).setChecked(false);
                }
                break;
        }
    }

    private void changeSwitch(Switch s) {
        if (s.isChecked())
            s.setChecked(false);
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
    public interface onAcFragmentInteraction {
        void onTrafficRedirect(String traffic, boolean on);

        View getView(int x);

        void onImgReplaceChosen(Uri uri);

        void onJsPayloadApply(List<Pair<Integer, String>> payloads);

        void onSwitchToggle(boolean on, String tag);

        IMVP.PresenterOps getPresenter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onAcFragmentInteraction) {
            mListener = (onAcFragmentInteraction) context;
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
