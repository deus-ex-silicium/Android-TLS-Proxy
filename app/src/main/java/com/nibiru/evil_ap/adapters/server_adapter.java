package com.nibiru.evil_ap.adapters;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.fragments.ServerDetailsFragment;
import com.nibiru.evil_ap.fragments.ServerItemFragment;
import com.nibiru.evil_ap.log.Client;

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
    private ArrayList<String> serverListCount;
    private Client client;
    ServerItemFragment Fragment_Server;

    /**************************************
     * CLASS METHODS
     *******************************************/
    public server_adapter(Context context, int resource, ArrayList<String> items,
                          ArrayList<String> count,
                          Activity passed_server_activity, Client c) {
        super(context, resource, items);
        server_activity = passed_server_activity;
        serverList = items;
        serverListCount = count;
        client = c;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v_server = convertView;
        if (v_server == null) {
            LayoutInflater vid;
            vid = LayoutInflater.from(getContext());
            v_server = vid.inflate(R.layout.fragment_serveritem, null);
        }

        final String d = serverList.size() > 0 ? serverList.get(position) : null;

        if (d != null) {
            final TextView ti = (TextView) v_server.findViewById(R.id.text_content_clientsserver);
            final TextView ticount = (TextView) v_server.findViewById(R.id
                    .text_content_clientsserverCount);
            final Button b = (Button) v_server.findViewById(R.id.button_details);
            if (ti != null) {
                ti.setText(serverList.get(position));
                ticount.setText(serverListCount.get(position));
                final View finalV_clients = v_server;
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Activity a = server_activity;
                        FragmentManager fm = a.getFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ServerDetailsFragment sdf = new ServerDetailsFragment();
                        sdf.initialize(ti.getText().toString(), client);
                        ft.replace(R.id.fragment_clients, sdf, "ServerDetails");
                        ft.addToBackStack(null).commit();
                    }
                });
            }
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
