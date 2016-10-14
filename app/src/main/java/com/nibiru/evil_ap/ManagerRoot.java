package com.nibiru.evil_ap;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Nibiru on 2016-10-14.
 * ManagerRoot class takes care of running root commands and checking exit status codes
 * also proves a static method for checking if device is rooted
 */

public class ManagerRoot {
    final static String TAG = "ManagerRoot";
    /*********************************************************************************************/
    public ManagerRoot(){}

    public boolean RunAsRoot(String command){
        Process p;
        try {
            p = Runtime.getRuntime().exec("su");

            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(command + '\n');
            os.writeBytes("exit\n");
            p.waitFor();
            os.flush();
        } catch (IOException e){
            e.printStackTrace();
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
            return false;
        }
        if (p.exitValue() == 0)
            return true;
        else
            return false;
    }

    public boolean isHttpRedirected(){
        return RunAsRoot("iptables -t nat -L | grep \"www redir ports 1337\"");
    }

    public static boolean isDeviceRooted() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su", "/su/bin/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }

    private static boolean checkRootMethod3() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "/system/xbin/which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            if (in.readLine() != null) return true;
            return false;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) process.destroy();
        }
    }
}
