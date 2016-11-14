package com.nibiru.evil_ap.log;


import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Nibiru on 2016-11-04.
 */

public class LogRequest {
    private final static String TAG = "LogRequest";
    private String timestamp;

    /*********************************************************************************************/
    public LogRequest(){
        this.timestamp = DateFormat.getDateTimeInstance().format(new Date());

    }
}
