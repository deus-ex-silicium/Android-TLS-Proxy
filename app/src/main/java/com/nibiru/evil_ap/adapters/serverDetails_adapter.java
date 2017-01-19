package com.nibiru.evil_ap.adapters;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.nibiru.evil_ap.R;
import com.nibiru.evil_ap.log.LogEntry;

import java.util.ArrayList;

/**
 * Created by Wobbaf on 04/11/2016.
 */

public class serverDetails_adapter extends ArrayAdapter<LogEntry> {
    /**************************************
     * CLASS FIELDS
     ********************************************/
    private Activity log_activity;
    private ArrayList<LogEntry> logList;
    private ArrayList<LogEntry> lList = null;

    /**************************************
     * CLASS METHODS
     *******************************************/
    public serverDetails_adapter(Context context, int resource, ArrayList<LogEntry> items,
                                 Activity passed_clients_activity) {
        super(context, resource, items);
        log_activity = passed_clients_activity;
        logList = items;
        lList = new ArrayList<>(logList);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, @NonNull ViewGroup parent) {
        View v_clients = convertView;
        if (v_clients == null) {
            LayoutInflater vid;
            vid = LayoutInflater.from(getContext());
            v_clients = vid.inflate(R.layout.fragment_server_details_item, null);
        }

        final LogEntry d = logList.size() > 0 ? logList.get(position) : null;

        if (d != null) {
            final TextView time = (TextView) v_clients.findViewById(R.id.details_time);
            final TextView det = (TextView) v_clients.findViewById(R.id.details_det);
            if (time != null) {
                time.setText(logList.get(position).getTimestamp());
            }
            if (det != null) {
                det.setText(logList.get(position).getDetails());
            }
        }
        return v_clients;
    }
}
