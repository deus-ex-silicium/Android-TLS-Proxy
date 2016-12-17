package com.nibiru.evil_ap;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Created by Nibiru on 2016-12-17.
 */

public class SharedClass {
    /**************************************CLASS FIELDS********************************************/
    private volatile byte[] imgData;
    private volatile List<String> payloads;
    /**************************************CLASS METHODS*******************************************/
    public SharedClass(InputStream is){
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            imgData = buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    synchronized void setPayloads(List<String> p){
        payloads = p;
    }

    public List<String> getPayloads(){
        return payloads;
    }

    synchronized void loadImage(String path){
        File file = new File(path);
        try {
            InputStream imgStream = new FileInputStream(file);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = imgStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            imgData = buffer.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public byte[] getImgData(){
        return imgData;
    }

    public int getImgDataLength(){
        return imgData.length;
    }
}
