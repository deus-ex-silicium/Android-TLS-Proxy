package com.nibiru.evil_ap.fragments;

import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.nibiru.evil_ap.MainActivity;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;

import java.util.ArrayList;
import java.util.List;

public class ACHTTPFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener {


    private OnFragmentInteractionListener mListener;
    private LinearLayout mLayout;
    private LinearLayout mLayout2;
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
        mLayout = (LinearLayout)v.findViewById(R.id.lin2dynamic);
        mLayout2 = (LinearLayout)v.findViewById(R.id.lin4dynamic);
        switchRedirectHTML.setOnCheckedChangeListener(this);
        switchInjectHTML.setOnCheckedChangeListener(this);
        switchSSLStrip.setOnCheckedChangeListener(this);
        switchReplacImages.setOnCheckedChangeListener(this);
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

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button_injectHTML:
                Log.e("E!", "inject");
                TextView textView = new TextView(view.getContext());
                textView.setText("testDynamic textView");
                mLayout.addView(textView);
                break;
            case R.id.button_replaceImages:
                Log.e("E!", "images");
                TextView textVie = new TextView(view.getContext());
                textVie.setText("testDynamic textView2");
                mLayout2.addView(textVie);
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch1:
                Log.e("Switch - ","redirect "+isChecked);
                Routing.redirectHTTP(isChecked);
                Log.e("Switch - ", "redirect " + isChecked);
                changeSwitch((Switch) getActivity().findViewById(R.id.switch2));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch3));
                changeSwitch((Switch) getActivity().findViewById(R.id.switch4));
                break;
            case R.id.switch2:
                Log.e("Switch - ", "inject " + isChecked);
                if (((Switch) getActivity().findViewById(R.id.switch1)).isChecked()) {
                    //do everything normal
                } else {
                    ((Switch) getActivity().findViewById(R.id.switch2)).setChecked(false);
                }
                break;
            case R.id.switch3:
                Log.e("Switch - ", "strip " + isChecked);
                if (((Switch) getActivity().findViewById(R.id.switch1)).isChecked()) {
                    //do everything normal
                } else {
                    ((Switch) getActivity().findViewById(R.id.switch3)).setChecked(false);
                }
                break;
            case R.id.switch4:
                Log.e("Switch - ","images "+isChecked);
                if (isChecked)
                    ((MainActivity)getActivity()).proxyService.swapWithImg(R.raw.pixel_skull);
                else
                    ((MainActivity)getActivity()).proxyService.swapWithImg(-1);
                Log.e("Switch - ", "images " + isChecked);
                if (((Switch) getActivity().findViewById(R.id.switch1)).isChecked()) {
                    //do everything normal
                } else {
                    ((Switch) getActivity().findViewById(R.id.switch4)).setChecked(false);
                }
                break;
        }
    }

    public void changeSwitch(Switch s) {
        if (s.isChecked())
            s.setChecked(false);
    }
    TextView createATextView(String text, int padding) {

        TextView textView_item_name = new TextView(getActivity().getApplicationContext());
        // LayoutParams layoutParams = new LayoutParams(
        // LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        // layoutParams.gravity = Gravity.LEFT;
        textView_item_name.setText(text);
        // textView1.setBackgroundColor(0xff66ff66); // hex color 0xAARRGGBB
        textView_item_name.setPadding(padding, padding, padding, padding);

        return textView_item_name;

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
