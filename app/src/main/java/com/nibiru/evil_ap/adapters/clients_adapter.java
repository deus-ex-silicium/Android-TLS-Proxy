package com.nibiru.evil_ap.adapters;

import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
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
    /************************************ CLASS FIELDS ********************************************/
    private Activity clients_activity;
    private ArrayList<Client> clientsList;
    private ArrayList<Client> cliList = null;
    ClientsFragment Fragment_Clients;
    private ClientsFragment.onClientsFragmentInteraction mListener;

    /************************************ CLASS METHODS *******************************************/
    /**
     *
     * @param context Context passed from Fragment
     * @param resource View in which this adapter operates
     * @param items List of Client objects
     * @param passed_clients_activity Activity passed from fragment
     */
    public clients_adapter(Context context, int resource, ArrayList<Client> items,
                           Activity passed_clients_activity,
                           ClientsFragment.onClientsFragmentInteraction lis) {
        super(context, resource, items);
        clients_activity = passed_clients_activity;
        clientsList = items;
        cliList = new ArrayList<>(clientsList);
        mListener = lis;
    }

    /**
     *
     * @param position Position of an element of which adapter currently operates
     * @param convertView View on which adapter currently operates
     * @param parent Parent view
     * @return Updated view
     */
    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
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
            }
            final Button log = (Button) v_clients.findViewById(R.id.button1);
            if (log != null) {
                log.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Activity a = clients_activity;
                        FragmentManager fm = a.getFragmentManager();
                        FragmentTransaction ft = fm.beginTransaction();
                        ServerItemFragment sif = new ServerItemFragment();
                        sif.initialize(clientsList.get(position));
                        ft.replace(R.id.fragment_clients, sif,"ServerItem");
                        ft.addToBackStack(null).commit();
                    }
                });
            }
            final Button ban = (Button) v_clients.findViewById(R.id.button2);
            if (ban != null) {
                ban.setText(clientsList.get(position).getBanned()?R.string.unban:R.string.ban);
                ban.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Log.e("Onclick",ban.getText().toString());
                        if(ban.getText().toString().equals("BAN")) {
                            ban.setText(R.string.unban);
                            mListener.setBan(clientsList.get(position), true);
                        }
                        else{
                            ban.setText(R.string.ban);
                            mListener.setBan(clientsList.get(position), false);
                        }
                    }
                });
            }
        }
        return v_clients;
    }
}
