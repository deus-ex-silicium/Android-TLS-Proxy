package com.nibiru.evil_ap.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nibiru.evil_ap.fragments.ServerItemFragment;
import com.nibiru.evil_ap.log.Client;
import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.fragments.ClientsFragment;

import java.util.ArrayList;

/**
 * Created by Wobbaf on 04/11/2016.
 */

public class server_adapter extends ArrayAdapter<String> {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    private Activity server_activity;
    private ArrayList<String> serverList;
    private ArrayList<String> cliList = null;
    ServerItemFragment Fragment_Server;

    /**************************************
     * CLASS METHODS
     *******************************************/
    public server_adapter(Context context, int resource, ArrayList<String> items,
                           Activity passed_server_activity) {
        super(context, resource, items);
        server_activity = passed_server_activity;
        serverList = items;
        cliList = new ArrayList<>(serverList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v_server = convertView;
        if (v_server == null) {
            LayoutInflater vid;
            vid = LayoutInflater.from(getContext());
            v_server = vid.inflate(R.layout.fragment_serveritem, null);
        }

        return v_server;
    }

    /** in case we want async client sync, \/ template */
//    public void updateDisciplines(ArrayList<Discipline> temp) {
//        Log.e("updateDisciplines","started update: size = "+disciplinesList.size());
//        disciplinesList.clear();
//        Log.e("updateDisciplines", "list cleared: size = "+disciplinesList.size());
//
//        disciplinesList.addAll(temp);
//        Log.e("updateDisciplines", "list populated: size = "+disciplinesList.size());
//
//        this.notifyDataSetChanged();
//
//    }

}
