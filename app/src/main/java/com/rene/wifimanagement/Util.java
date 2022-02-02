package com.rene.wifimanagement;

import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.rene.wifimanagement.WorkManaging.BroadcastServiceReceiver;
import com.rene.wifimanagement.WorkManaging.NotificationWorker;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class Util {

    public static final int PRICE = 200;
    public static final String cacheKey = "customers_list";
    public static final String historyKey = "history";
    public static final String isWorkEnabledKey = "isWorkEnabled";

    public static final String NAME_KEY = "name";
    public static final String DUEDATE_KEY = "due_date";
    public static final String CONNECTED_KEY = "connected";
    public static final String TOPAY_KEY = "to_pay";
    public static final String STATUS_KEY = "status";
    public static final String DEVICES_KEY = "devices";
    public static final String REG_MOS = "registered_month";
    public static final String LAST_CHECK_KEY = "last_checked";

    public static void disableBroadcastReceiver(Context context) {

        ComponentName receiver =  new ComponentName(context, BroadcastServiceReceiver.class);
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(receiver,
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    public static void disableWork(Context context) {
        WorkManager.getInstance(context)
                .cancelAllWork();
    }

    public static void initiateWork(Context context) {
        final int repeatIntervalValue = 12;

        PeriodicWorkRequest notificationWorkRequest =
                new PeriodicWorkRequest.Builder(NotificationWorker.class, repeatIntervalValue, TimeUnit.HOURS)
                        .build();

        WorkManager.getInstance(context)
                .enqueue(notificationWorkRequest);

        Toast.makeText(context, "Automatic Alarm Activated!", Toast.LENGTH_LONG).show();
    }

    public static String getStrMonth(final int numMonth) {
        String strMonth = "";
        switch (numMonth) {
            case 0: case 12:
                strMonth = "JANUARY";
                break;
            case 1:
                strMonth = "FEBRUARY";
                break;
            case 2:
                strMonth = "MARCH";
                break;
            case 3:
                strMonth = "APRIL";
                break;
            case 4:
                strMonth = "MAY";
                break;
            case 5:
                strMonth = "JUNE";
                break;
            case 6:
                strMonth = "JULY";
                break;
            case 7:
                strMonth = "AUGUST";
                break;
            case 8:
                strMonth = "SEPTEMBER";
                break;
            case 9:
                strMonth = "OCTOBER";
                break;
            case 10:
                strMonth = "NOVEMBER";
                break;
            case 11:
                strMonth = "DECEMBER";
                break;
        }

        return strMonth;
    }

    public static int StrToNumMonth(@NonNull final String strMonth) {
        int num = -1;
        switch (strMonth) {
            case "JANUARY":
                num = 0;
                break;
            case "FEBRUARY":
                num =  1;
                break;
            case "MARCH":
                num =  2;
                break;
            case "APRIL":
                num =  3;
                break;
            case "MAY":
                num =  4;
                break;
            case "JUNE":
                num =  5;
                break;
            case "JULY":
                num =  6;
                break;
            case "AUGUST":
                num =  7;
                break;
            case "SEPTEMBER":
                num =  8;
                break;
            case "OCTOBER":
                num =  9;
                break;
            case "NOVEMBER":
                num =  10;
                break;
            case "DECEMBER":
                num =  11;
                break;
        }

        return num;
    }

    public static int StringToInteger(final String str) {
        if (!isNumeric(str))
            return -1;

        return isInteger(str)?Integer.parseInt(str):(int)Double.parseDouble(str);
    }

    public static boolean isInteger(String num) {
        try {
            Integer.parseInt(num);
            return true;
        } catch(Exception e){
            return false;
        }
    }

    public static boolean isNumeric(@NonNull String str) {
        boolean isNumeric = true;
        HashMap<Character, String> nums = new HashMap<>();
        nums.put('1', ""); nums.put('2', ""); nums.put('3', "");
        nums.put('4', ""); nums.put('5', ""); nums.put('6', "");
        nums.put('7', ""); nums.put('8', ""); nums.put('9', ""); nums.put('0', ""); nums.put('.', "");

        final char[] characters = str.toCharArray();
        for (char _char : characters) {
            if (!nums.containsKey(_char)) {
                isNumeric = false;
                break;
            }
        }

        return isNumeric;
    }

    public static class Cache {
        private static SharedPreferences cache;

        public static SharedPreferences Instance(Context context) {
            if (cache == null)
                cache = context.getSharedPreferences("infos", AppCompatActivity.MODE_PRIVATE);

            return cache;
        }
    }
}
