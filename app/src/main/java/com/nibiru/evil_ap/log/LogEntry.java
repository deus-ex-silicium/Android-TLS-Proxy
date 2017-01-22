package com.nibiru.evil_ap.log;

/**
 * Created by Nibiru on 2016-12-29.
 */

public class LogEntry {
    /**************************************CLASS FIELDS********************************************/
    private long mId;
    private String timestamp;
    private String host;
    private String reqLine;
    private String details;
    private String method;
    /**************************************CLASS METHODS*******************************************/
    public LogEntry(long id, String timestamp, String host, String reqLine, String details){
        this.mId = id;
        this.timestamp = timestamp;
        this.host = host;
        this.details = details;
        this.reqLine = reqLine;
        if (reqLine.startsWith("GET"))
            method = "GET";
        else
            method = "POST";
    }

    public long getmId() {
        return mId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public String getHost() {
        return host;
    }

    public String getDetails() {
        return details;
    }

    public String getMethod() {
        return method;
    }

    public String getReqLine() {
        return reqLine;
    }
}
