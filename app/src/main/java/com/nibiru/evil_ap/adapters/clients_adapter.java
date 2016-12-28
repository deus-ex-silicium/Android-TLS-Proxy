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
import android.widget.LinearLayout;
import android.widget.TextView;

import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.fragments.ClientsFragment;
import com.nibiru.evil_ap.fragments.ServerItemFragment;
import com.nibiru.evil_ap.log.Client;

import java.util.ArrayList;

/**
 * Created by Wobbaf on 04/11/2016.
 */

public class clients_adapter extends ArrayAdapter<Client> {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    private Activity clients_activity;
    private ArrayList<Client> clientsList;
    private ArrayList<Client> cliList = null;
    ClientsFragment Fragment_Clients;

    /**************************************
     * CLASS METHODS
     *******************************************/
    public clients_adapter(Context context, int resource, ArrayList<Client> items,
                           Activity passed_clients_activity) {
        super(context, resource, items);
        clients_activity = passed_clients_activity;
        clientsList = items;
        cliList = new ArrayList<>(clientsList);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v_clients = convertView;
        if (v_clients == null) {
            LayoutInflater vid;
            vid = LayoutInflater.from(getContext());
            v_clients = vid.inflate(R.layout.list_item_clients, null);
        }

        final Client d = clientsList.size() > 0 ? clientsList.get(position) : null;

        if (d != null) {
            final TextView ti = (TextView) v_clients.findViewById(R.id.text_content_clientip);
            if (ti != null) {
                ti.setText(clientsList.get(position).getIp());
                final View finalV_clients = v_clients;
                ti.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        LinearLayout ll = (LinearLayout) ti.getParent();
                        if (ll.getBackground().equals(finalV_clients.getResources().getColor(R
                                .color.oplblue))) {
                            ll.setBackgroundColor(finalV_clients.getResources().getColor(R.color
                                    .opblue));
                        } else {
                            ll.setBackgroundColor(finalV_clients.getResources().getColor(R.color
                                    .oplblue));
                        }
                    }
                });
            }
            final Button log = (Button) v_clients.findViewById(R.id.button1);
            if (log != null) {
                final View finalV_clients = v_clients;
                log.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Activity a = clients_activity;
                        FragmentManager fm = a.getFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ft.replace(R.id.activity_main, new ServerItemFragment(),
                                null);
                        ft.addToBackStack(null).commit();
                    }
                });
            }
        }
        return v_clients;
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
