package com.ywh.util;

public class LogDebug {
    private String fromClass;


    public LogDebug(String fromClass) {
        this.fromClass = fromClass;
    }

    public void log(Object str) {
        log("Default", str);
    }

    public void log(String action, Object str) {
        if (Cons.DEBUG) {
            System.out.println(fromClass + ":[" + action + "]:" + str.toString());
        }
    }
}
