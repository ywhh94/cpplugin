package com.ywh.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("all")
public class Utils {

    private static LogDebug logDebug = new LogDebug("Utils");

    private static List<String> baseActivityList = new ArrayList<>();

    //用户在manifest中配置的所有activity
    private static List<String> manifestActivitys = new ArrayList<>();
    //key  className,  value superClassName
    private static HashMap<String, String> classWithParent = new HashMap<>();
    //result
    private static List<String> trueActivity = new ArrayList<>();
    private static List<String> falseActivity = new ArrayList<>();


    //全部忽略list
    private static List<String> ignoreList = new ArrayList<>();

    //扩展参数
    private static boolean mThrowException = true;
    private static boolean mExceptionAtOnce = true;
    private static boolean mShowUserLog = false;


    //添加baseClass
    public static void addBaseClass(String className, String annotation) {
        if ("Lcom/ywh/cpp/annotation/BaseActivityCheck;".equals(annotation)) {
            baseActivityList.add(className);
            return;
        }

        if ("Lcom/ywh/cpp/annotation/IgnoreCheck;".equals(annotation)) {
            ignoreList.add(className);
            return;
        }

    }

    public static void init(CheckBaseExtension cpExtension) {
        mThrowException = cpExtension.throwException;
        mExceptionAtOnce = cpExtension.exceptionAtOnce;
        mShowUserLog = cpExtension.openLog;
        clearData();
        logDebug("Params", cpExtension.toString());
    }

    /**
     * 只添加manifestActivity中activity
     *
     * @param key
     * @param value
     */
    public static void putActivityWithParent(String key, String value) {
        if (manifestActivitys.contains(key) && !ignoreList.contains(key)) {
            classWithParent.put(key, value);
        }
    }

    public static void putManifestActivity(String activity) {
        //[{http://schemas.android.com/apk/res/android}name:com.ywh.base.TestActivity]
        manifestActivitys.add(handlerUserActivity(activity));
    }

    public static int getManifestAcSize() {
        return manifestActivitys.size();
    }

    private static void clearData() {
        manifestActivitys.clear();
        ignoreList.clear();
        baseActivityList.clear();
        classWithParent.clear();
        trueActivity.clear();
        falseActivity.clear();
    }

    /**
     * 获取 [{http://schemas.android.com/apk/res/android}name:com.ywh.base.TestActivity]中name对应值
     *
     * @param activity
     * @return
     */
    public static String handlerUserActivity(String activity) {
        if (activity.contains("name:")) {
            activity = activity.substring(activity.indexOf("name:") + "name:".length(), activity.length());
            if (activity.endsWith("]")) {
                activity = activity.substring(0, activity.length() - 1);
            }
            activity = activity.replace(".", "/");
        }
        return activity;
    }

    //开始检查
    public static void check() throws Throwable {
        logDebug("ManifestActivity", "checkcheckcheckcheckcheck");
        for (String name : manifestActivitys) {
            logDebug("ManifestActivity", name);
        }

        trueActivity.clear();
        falseActivity.clear();
        for (Map.Entry<String, String> entry : classWithParent.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            checkParentBaseActivity(key, value);
        }
        logForUser("BaseActivity", baseActivityList);
        logForUser("Ignore", ignoreList);
        logForUserResult(true, trueActivity);
        logForUserResult(false, falseActivity);
        throwErrorAll();
    }


    private static void checkParentBaseActivity(String key, String value) {
        logDebug("check", "key:" + key + ",value:" + value);

        if (ignoreList.contains(key)) {
            return;
        }
        if (baseActivityList.contains(value)) {
            trueActivity.add(key);
        } else {
            falseActivity.add(key);
            throwError(key);
        }
    }

    private static void addToIgnore(String originalKey) {
        if (!ignoreList.contains(originalKey)) {
            ignoreList.add(originalKey);
        }
    }


    //打印结果,true/false
    private static void logForUserResult(boolean result, List<String> list) {
        if (result) {
            if (!mShowUserLog) {
                return;
            }
        }
        for (String name : list) {
            System.out.println("CheckBaseActivity:" + "[Result:" + result + "]:" + name);
        }
    }


    //抛异常全部不合规范的class
    private static void throwErrorAll() {
        if (!mThrowException) {
            return;
        }
        if (falseActivity.size() > 0) {
            String clazzStrs = "";
            for (String name : falseActivity) {
                if (clazzStrs.equals("")) {
                    clazzStrs = "[" + name + "]";
                } else {
                    clazzStrs += ",[" + name + "]";
                }
            }
            String log = "class " + clazzStrs + "  : not meet the conditions:extends baseActivity";
            throw new ClassFormatError(log);
        }
    }

    //抛异常
    private static void throwError(String clazz) {
        if (mExceptionAtOnce) {
            logForUserResult(false, falseActivity);
            throw new ClassFormatError("class " + clazz + " : not meet the conditions:extends baseActivity");
        }
    }

    //开发测试打日志
    private static void logForUser(String action, List<String> list) {
        if (!mShowUserLog) {
            return;
        }
        for (String name : list) {
            System.out.println("CheckBaseActivity:[" + action + "]:" + name);
        }
    }

    //开发测试打日志
    private static void logDebug(String action, String str) {
        logDebug.log(action, str);
    }
}
