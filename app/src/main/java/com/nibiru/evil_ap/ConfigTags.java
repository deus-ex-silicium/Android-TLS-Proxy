package com.nibiru.evil_ap;

/**
 * Created by Nibiru on 2016-12-16.
 */

public enum ConfigTags {
    imgReplace("imgReplace"),
    sslStrip("sslStrip"),
    jsInject("jsInject"),
    httpRedirect("httpRedirect"),
    httpsRedirect("httpsRedirect"),
    imgPath("imgPath")
    ;

    private final String text;

    ConfigTags(final String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }
}