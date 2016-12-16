package com.nibiru.evil_ap.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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

import com.nibiru.evil_ap.IMVP;
import com.nibiru.evil_ap.R;

import static android.app.Activity.RESULT_OK;

public class ACHTTPFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener, IMVP.RequiredViewOps {
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
    public ACHTTPFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        Switch switchRedirectHTML = (Switch) v.findViewById(R.id.switch1);
        Switch switchInjectHTML = (Switch) v.findViewById(R.id.switch2);
        Switch switchSSLStrip = (Switch) v.findViewById(R.id.switch3);
        Switch switchReplacImages = (Switch) v.findViewById(R.id.switch4);
        mLayout = (LinearLayout) v.findViewById(R.id.lin2dynamic);
        mLayout2 = (LinearLayout) v.findViewById(R.id.lin4dynamic);
        iv = new ImageView(v.getContext());
        switchRedirectHTML.setOnCheckedChangeListener(this);
        switchInjectHTML.setOnCheckedChangeListener(this);
        switchSSLStrip.setOnCheckedChangeListener(this);
        switchReplacImages.setOnCheckedChangeListener(this);
        return v;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_injectHTML:
                if (!layotPayloadflag) {
                    layotPayloadflag = !layotPayloadflag;
                    Log.e("E!", "inject");
                    CheckBox cb = new CheckBox(view.getContext());
                    cb.setText("Payload1");
                    EditText et = new EditText(view.getContext());
                    et.setText("Hello from Evil-AP!");
                    mLayout.addView(cb);
                    mLayout.addView(et);
                    CheckBox cb2 = new CheckBox(view.getContext());
                    cb2.setText("Payload2");
                    EditText et2 = new EditText(view.getContext());
                    EditText et3 = new EditText(view.getContext());
                    et2.setHint("IP");
                    et3.setHint("Port");
                    mLayout.addView(cb2);
                    mLayout.addView(et2);
                    mLayout.addView(et3);
                    break;
                } else {
                    layotPayloadflag = !layotPayloadflag;
                    mLayout.removeAllViews();
                }
            case R.id.button_replaceImages:
                if (!layoutImageflag) {
                    layoutImageflag = !layoutImageflag;
                    String imgpath = " ";

                    if (!mPresenter.checkIfSharedPrefsNull() && mPresenter
                            .checkIfSharedPrefsContain(
                                    "imgPath")) {
                        try {
                            imgpath = mPresenter.getSharedPrefsString("imgPath");
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (imgpath == null || imgpath.equals(" ")) {
                            Log.e("SP is null - ", imgpath);
                        } else {
                            applyToImageView(mLayout2, iv, Uri.parse(imgpath));
                        }
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
                    layoutImageflag = !layoutImageflag;
                    mLayout2.removeAllViews();
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

    public void applyToImageView(LinearLayout layout, ImageView iv, Uri image) {
        if(iv != null){
            layout.removeView(iv);
        }
        layout.addView(iv, 0);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
        lp.width = 500;
        lp.height = 500;
        lp.gravity = Gravity.CENTER;
        iv.setImageURI(image);

    }

    public void checkSwitches() {
        if (!mPresenter.checkIfSharedPrefsNull()) {
            if (mPresenter.checkIfSharedPrefsContain("sslStrip") && mListener.getView(R.id.switch3) !=
                    null)
                ((Switch) mListener.getView(R.id.switch3)).setChecked(mPresenter.getSharedPrefsBool("sslStrip"));
            if (mPresenter.checkIfSharedPrefsContain("imgReplace") && mListener.getView(R.id.switch4) != null)
                ((Switch) mListener.getView(R.id.switch4)).setChecked(mPresenter.getSharedPrefsBool("imgReplace"
                ));
            if (mPresenter.checkIfSharedPrefsContain("httpRedirect") && mListener.getView(R.id.switch1) != null)
                ((Switch) mListener.getView(R.id.switch1)).setChecked(mPresenter.getSharedPrefsBool("httpRedirect"
                ));
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch1:
                Log.e("Switch - ", "redirect " + isChecked);
                mListener.onTrafficRedirect("HTTP", isChecked);
                changeSwitch((Switch) getActivity().findViewById(R.id.switch2));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch3));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch4));
                mListener.onHTTPRedirectToggle(isChecked);
                break;
            case R.id.switch2:
                Log.e("Switch - ", "inject " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onJsInject(isChecked);
                } else {
                    ((Switch) mListener.getView(R.id.switch2)).setChecked(false);
                }
                break;
            case R.id.switch3:
                Log.e("Switch - ", "strip " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onSslStripToggle(isChecked);
                } else {
                    ((Switch) mListener.getView(R.id.switch3)).setChecked(false);
                }
                break;
            case R.id.switch4:
                Log.e("Switch - ", "images " + isChecked);
                if (((Switch) mListener.getView(R.id.switch1)).isChecked()) {
                    mListener.onImgReplaceToggle(isChecked);
                } else {
                    ((Switch) mListener.getView(R.id.switch4)).setChecked(false);
                }
                break;
        }
    }

    public void changeSwitch(Switch s) {
        if (s.isChecked())
            s.setChecked(false);
    }

    @Override
    public void showToast(String msg) {

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

        void onSslStripToggle(boolean on);

        void onJsInject(boolean on);

        View getView(int x);

        void onImgReplaceChosen(Uri uri);

        void onImgReplaceToggle(boolean on);


        void onHTTPRedirectToggle(boolean isChecked);

        IMVP.PresenterOps getPresenter();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof onAcFragmentInteraction) {
            mListener = (onAcFragmentInteraction) context;
            mPresenter = mListener.getPresenter();
            checkSwitches();
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
