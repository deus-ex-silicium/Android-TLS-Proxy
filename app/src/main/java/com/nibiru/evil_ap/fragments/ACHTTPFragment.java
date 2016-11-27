package com.nibiru.evil_ap.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
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
import android.widget.TextView;
import android.widget.Toast;

import com.nibiru.evil_ap.MainActivity;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.manager.Root;
import com.nibiru.evil_ap.manager.Routing;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;

public class ACHTTPFragment extends Fragment implements View.OnClickListener, CompoundButton
        .OnCheckedChangeListener {


    private OnFragmentInteractionListener mListener;
    private LinearLayout mLayout;
    private LinearLayout mLayout2;
    private boolean l1flag = false;
    private boolean l2flag = false;
    private ImageView iv;

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
                if (!l1flag) {
                    l1flag = !l1flag;
                    Log.e("E!", "inject");
                    CheckBox cb = new CheckBox(view.getContext());
                    cb.setText("Payload1");
                    EditText et = new EditText(view.getContext());
                    et.setText
                            ("PADSFPSFSDPFSPDFPSDPFDSPFPSDFPSDPFPSDFPDSPFPSDPFPSDFPSDPFPSDFPSAPDFPASPDFPASDPFAPSDFPASPDFPASDPFPSDAFPSDFP");
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
                    l1flag = !l1flag;
                    mLayout.removeAllViews();
                }
            case R.id.button_replaceImages:
                if (!l2flag) {
                    l2flag = !l2flag;
                    Log.e("E!", "images");
                    Button b = new Button(view.getContext());
                    Button b2 = new Button(view.getContext());
                    iv = new ImageView(view.getContext());

                    b.setText("choose image");
                    b2.setText("load form url");
                    b.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            Intent pickPhoto = new Intent(Intent.ACTION_PICK,
                                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                            startActivityForResult(pickPhoto, 1);//one can be replaced with any action code
                        }
                    });
                    b2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
//                            AlertDialog.Builder builder = new AlertDialog.Builder(view.getContext());
//                            builder.setTitle("Input url");
//
//                            // Set up the input
//                            final EditText input = new EditText(view.getContext());
//                            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//                            input.setInputType(InputType.TYPE_CLASS_TEXT);
//                            builder.setView(input);
//
//                            // Set up the buttons
//                            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    String url = input.getText().toString();
//                                    url = "https://pbs.twimg" +
//                                            ".com/profile_images/601755021403762688/7_MKT2B_.jpg";
//                                    Log.e("e",url);
//                                    Drawable d = LoadImageFromWebOperations(url);
//                                    if(iv !=null){
//                                        mLayout2.removeView(iv);
//                                    }
//                                    iv.setImageDrawable(d);
//                                    mLayout2.addView(iv);
//                                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
//                                    lp.width = 500;
//                                    lp.height = 500;
//                                    lp.gravity = Gravity.CENTER;
//                                }
//                            });
//                            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.cancel();
//                                }
//                            });
//                            builder.show();
//
                            URL url = null;
                            try {
                                url = new URL("http://image10.bizrate-images.com/resize?sq=60&uid=2216744464");
                            } catch (MalformedURLException e) {
                                e.printStackTrace();
                            }
                            Bitmap bmp = null;
                            try {
                                bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            iv.setImageBitmap(bmp);
                            if(iv != null){
                                mLayout2.removeView(iv);
                            }
                            mLayout2.addView(iv);
                        }

                    });

                    mLayout2.addView(b);
                    mLayout2.addView(b2);
                    break;
                } else {
                    l2flag = !l2flag;
                    mLayout2.removeAllViews();
                }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        switch (requestCode) {
            case 0:
                if (resultCode == RESULT_OK) {

                }

                break;
            case 1:
                if (resultCode == RESULT_OK) {
                    Uri selectedImage = imageReturnedIntent.getData();
                    if(iv != null){
                        mLayout2.removeView(iv);
                    }
                    mLayout2.addView(iv);
                    LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) iv.getLayoutParams();
                    lp.width = 500;
                    lp.height = 500;
                    lp.gravity = Gravity.CENTER;
                    iv.setImageURI(selectedImage);
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.switch1:
                Log.e("Switch - ", "redirect " + isChecked);
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
                Log.e("Switch - ", "images " + isChecked);
                if (isChecked)
                    ((MainActivity) getActivity()).proxyService.swapWithImg(R.raw.pixel_skull);
                else
                    ((MainActivity) getActivity()).proxyService.swapWithImg(-1);
                Log.e("Switch - ", "images " + isChecked);
                if (((Switch) getActivity().findViewById(R.id.switch1)).isChecked()) {
                    //do everything normal
                } else {
                    ((Switch) getActivity().findViewById(R.id.switch4)).setChecked(false);
                }
                break;
        }
    }

    public static Drawable LoadImageFromWebOperations(String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            Drawable d = Drawable.createFromStream(is, "src name");
            return d;
        } catch (Exception e) {
            return null;
        }
    }

    public void changeSwitch(Switch s) {
        if (s.isChecked())
            s.setChecked(false);
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
