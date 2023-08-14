package Strix_decopile.logging;

/**
 * Created by a.kiperku
 * Date: 14.08.2023
 */

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class StrixLog {
    private static FileLog MAINLOG = null;
    private static FileLog DEBUGLOG = null;
    private static FileLog AUDITLOG = null;
    private static FileLog ERRORLOG = null;
    private static FileLog AUTHLOG = null;

    public StrixLog() {
    }

    public static void error(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.err.println(sdf.format(cal.getTime()) + " [strixplatform] ERROR - " + msg);
        ERRORLOG.log("ERROR - " + msg);
    }

    public static void info(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + " [strixplatform] INFO - " + msg);
        MAINLOG.log("INFO - " + msg);
    }

    public static void warn(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.err.println(sdf.format(cal.getTime()) + " [strixplatform] WARN - " + msg);
        ERRORLOG.log("WARN - " + msg);
    }

    public static void log(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + " [strixplatform] LOG - " + msg);
        MAINLOG.log("LOG - " + msg);
    }

    public static void audit(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + " [strixplatform] AUDIT - " + msg);
        AUDITLOG.log("AUDIT - " + msg);
    }

    public static void debug(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + " [strixplatform] DEBUG - " + msg);
        DEBUGLOG.log("DEBUG - " + msg);
    }

    public static void auth(String msg) {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        System.out.println(sdf.format(cal.getTime()) + " [strixplatform] AUTH - " + msg);
        AUTHLOG.log("AUTH - " + msg);
    }

    static {
        try {
            MAINLOG = new FileLog("strix-platform/log/general.log");
            DEBUGLOG = new FileLog("strix-platform/log/debug.log");
            AUDITLOG = new FileLog("strix-platform/log/audit.log");
            ERRORLOG = new FileLog("strix-platform/log/error.log");
            AUTHLOG = new FileLog("strix-platform/log/auth.log");
        } catch (IOException var1) {
            var1.printStackTrace();
        }

    }
}