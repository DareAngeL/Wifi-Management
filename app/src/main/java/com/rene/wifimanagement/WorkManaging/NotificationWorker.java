package com.rene.wifimanagement.WorkManaging;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rene.wifimanagement.NotificationService;
import com.rene.wifimanagement.Util;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class NotificationWorker extends Worker {

    public NotificationWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        final Context context = this.getApplicationContext();

        final String cacheKey = "customers_list";
        final String NAME_KEY = "name";
        final String DUEDATE_KEY = "due_date";

        Calendar calendar = Calendar.getInstance();
        SharedPreferences cache = context.getSharedPreferences("infos", AppCompatActivity.MODE_PRIVATE);

        List<HashMap<String, Object>> listInfo = new Gson().fromJson(cache.getString(cacheKey, ""), new TypeToken<List<HashMap<String, Object>>>() {}.getType());

        // checks if its the dueDate or not of one of our wifi users
        final int dayToday = calendar.get(Calendar.DAY_OF_MONTH);
        final int monthToday = calendar.get(Calendar.MONTH);

        HashMap<String, Object> userWithDueDateInfo = null;
        boolean isPastDueDate = false;

        for (HashMap<String, Object> info : listInfo) {
            final String dueDateStr = Objects.requireNonNull(info.get(DUEDATE_KEY)).toString();
            final String userRegMonthStr = Objects.requireNonNull(info.get(Util.REG_MOS)).toString();

            int userRegMonth = Util.isInteger(userRegMonthStr)?Integer.parseInt(userRegMonthStr):(int)Double.parseDouble(userRegMonthStr);
            int userDueDate = Util.isInteger(dueDateStr)?Integer.parseInt(dueDateStr):(int)Double.parseDouble(dueDateStr);

            if (dayToday >= userDueDate && monthToday != userRegMonth) {
                if (dayToday > userDueDate)
                    isPastDueDate = true;

                userWithDueDateInfo = info;
                break;
            }
        }
        // if there is a dueDate for this day, notify me
        if (userWithDueDateInfo != null) {
            new NotificationService(context)
                    .BuildNotification(Objects.requireNonNull(userWithDueDateInfo.get(NAME_KEY)).toString(),"Charge them now.", isPastDueDate)
                    .Notify();
        }

        return Result.success();
    }
}