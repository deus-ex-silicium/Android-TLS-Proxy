package com.nibiru.evil_ap.log;

/**
 * Created by Nibiru on 2016-12-29.
 */

public class LogEntry {
    /**************************************CLASS FIELDS********************************************/
    private long mId;
    private String timestamp;
    private String host;
    private String details;
    /**************************************CLASS METHODS*******************************************/
    public LogEntry(long id, String timestamp, String host, String details){
        this.mId = id;
        this.timestamp = timestamp;
        this.host = host;
        this.details = details;
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
}
