package com.rene.wifimanagement;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rene.wifimanagement.WorkManaging.BroadcastServiceReceiver;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    private List<HashMap<String, Object>> listInfos = new ArrayList<>();

    private FloatingActionButton fab;
    private SharedPreferences cache;
    private RecyclerView list;
    private Calendar calendar;
    private DrawerLayout drawer;

    private boolean isListEmpty = true;
    private boolean isFromAddingCust = false;
    private int mMosLastChecked = -1;

    @Override
    public void onBackPressed() {
        if (drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            super.onBackPressed();
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cache = Util.Cache.Instance(this);
        //cache.edit().putInt(Util.LAST_CHECK_KEY, 0).apply();

        // declaration
        final TextView date = findViewById(R.id.date);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        final LinearLayout navView = findViewById(R.id.nav_view);
        final LinearLayout drawerHistoryBtn = navView.findViewById(R.id.history_btn);
        drawerHistoryBtn.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                drawerHistoryBtn.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                drawerHistoryBtn.getLayoutParams().width = navView.getMeasuredWidth() - 50;
                drawerHistoryBtn.requestLayout();
            }
        });
        drawerHistoryBtn.setOnClickListener(view -> {
            Intent nextActivityIntent = new Intent(this, HistoryActivity.class);
            startActivity(nextActivityIntent);
        });

        calendar = Calendar.getInstance();
        drawer = findViewById(R.id.drawer);
        fab = findViewById(R.id.fab);
        list = findViewById(R.id.list);

        toolbar.setNavigationOnClickListener(view -> onBackPressed());
        // initialize drawerlayout and toolbar
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        toolbar.setTitleTextColor(ContextCompat.getColor(this, R.color.white));
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.app_name, R.string.app_name);
        toggle.getDrawerArrowDrawable().setColor(ContextCompat.getColor(this, R.color.white));
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        if (cache.getString(Util.isWorkEnabledKey, "").isEmpty())
            cache.edit().putString(Util.isWorkEnabledKey, "").apply();

        _initListener();
        // *** SETS THE DATE *** \\
        final String dateStr = calendar.get(Calendar.DAY_OF_MONTH) + "-" +
                                Util.getStrMonth(calendar.get(Calendar.MONTH)) + "-" +
                                calendar.get(Calendar.YEAR);
        date.setText(dateStr);

        final String cacheStr = cache.getString(Util.cacheKey, "");
        // checks if there is any data from cache
        if (!cacheStr.isEmpty())
            listInfos = new Gson().fromJson(cacheStr, new TypeToken<List<HashMap<String, Object>>>() {}.getType());
        // if there is any data from cache load it.
        if (listInfos.size() > 0) {
            list.setHasFixedSize(true);
            list.setAdapter(new ListAdapter(listInfos, isFromAddingCust));
            list.setLayoutManager(new LinearLayoutManager(this) {
                @Override
                public void onLayoutCompleted(RecyclerView.State state) {
                    super.onLayoutCompleted(state);
                    // always update the cache of listInfos every after layout complete of recyclerview
                    cache.edit().putString(Util.cacheKey, new Gson().toJson(listInfos)).apply();
                    // update the month to when was the app was last checked or opened.
                    if (mMosLastChecked != -1)
                        cache.edit().putInt(Util.LAST_CHECK_KEY, mMosLastChecked).apply();
                }
            });
            Objects.requireNonNull(list.getAdapter()).notifyDataSetChanged();
            isListEmpty = false;
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private  void _initListener() {
        final boolean[] isFabOnResetAlarm = {false};
        fab.setOnClickListener(view -> {
            if (isFabOnResetAlarm[0]) { // if its for resetting the alarm
                isFabOnResetAlarm[0] = false;
                Util.disableWork(this);
                Util.initiateWork(this);

                fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add));
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor)));
                return;
            }
            // otherwise, its for adding new user.
            list.setEnabled(false);
            new UserFormDialog(this, null, 0, (isFromEdit, _position, _name, _dueDate, _connected, _toPay, _status, _monthOf, _devices) -> {
                // on save button clicked!
                if (! isFromEdit)
                    listInfos.add(infoToMap(_monthOf, _name, _dueDate, _connected, _toPay, _status, _devices));

                if (! isListEmpty)
                    Objects.requireNonNull(list.getAdapter()).notifyItemInserted(listInfos.size() - 1);
                else {
                    list.setHasFixedSize(true);
                    list.setAdapter(new ListAdapter(listInfos, isFromAddingCust=true));
                    list.setLayoutManager(new LinearLayoutManager(this) {
                        @Override
                        public void onLayoutCompleted(RecyclerView.State state) {
                            super.onLayoutCompleted(state);
                            isFromAddingCust = false;
                        }
                    });
                    Objects.requireNonNull(list.getAdapter()).notifyDataSetChanged();
                    isListEmpty = false;
                }
                // saves infos to cache
                cache.edit().putString(Util.cacheKey, new Gson().toJson(listInfos)).apply();

                // enables BroadcastServiceReceiver
                ComponentName receiver = new ComponentName(this, BroadcastServiceReceiver.class);
                PackageManager pm = this.getPackageManager();
                pm.setComponentEnabledSetting(receiver,
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && cache.getString(Util.isWorkEnabledKey, "").isEmpty()) {
                    cache.edit().putString(Util.isWorkEnabledKey, String.valueOf(true)).apply();
                    Util.disableWork(this);
                    Util.initiateWork(this);
                }
            }).show();
        });

        fab.setOnLongClickListener(view -> {
            if (!isFabOnResetAlarm[0]) {
                isFabOnResetAlarm[0] = true;
                fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_refresh_alarm_ic));
                fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.purple_200)));
                return true;
            }

            isFabOnResetAlarm[0] = false;
            fab.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add));
            fab.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryColor)));

            return true;
        });
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * *
     * Build the Information in a HashMap Object
     * * * * * * * * * * * * * * * * * * * * * * * *  */
    @NonNull
    private HashMap<String, Object> infoToMap(String regMos, String name, String dueDate, String connected, String toPay, String status, @NonNull String devices) {
        final HashMap<String, Object> map = new HashMap<>();
        map.put(Util.NAME_KEY, name);
        map.put(Util.DUEDATE_KEY, dueDate);
        map.put(Util.CONNECTED_KEY, connected);
        map.put(Util.TOPAY_KEY, toPay);
        map.put(Util.STATUS_KEY, status);
        map.put(Util.DEVICES_KEY, !devices.isEmpty()?new Gson().toJson(_separateDevices(devices)):"");
        map.put(Util.REG_MOS, regMos);

        return map;
    }

    /* * * * * * * * * * * * * * * * * * * * * * * * *
     * Converts the String value of devicesText and separate
     * each devices and store it to List.
     * * * * * * * * * * * * * * * * * * * * * * * *  */
    @NonNull
    private List<String> _separateDevices(@NonNull String devicesText) {
        final List<String> strLst = new ArrayList<>();
        // separate
        String[] devices = devicesText.split(",");
        // clean the text, removed the leading and trailing spaces
        for (String device : devices) {
            strLst.add(device.trim());
        }

        return strLst;
    }

    public class ListAdapter extends RecyclerView.Adapter<ListAdapter.ViewHolder> {
        private final List<HashMap<String, Object>> data;
        private final boolean isFromAddingCustomer;
        private final int PAID_COLOR_INDICATOR = Color.parseColor("#FF4CAF50"); // green
        private final int NOT_PAID_COLOR_INDICATOR = Color.parseColor("#FFFF0000"); // red
        private final int NOT_FULLY_PAID_COLOR_INDICATOR = Color.parseColor("#FFC39200"); // yellow

        public ListAdapter(List<HashMap<String, Object>> _data, boolean _isFromAddingCustomer) {
            data = _data;
            isFromAddingCustomer = _isFromAddingCustomer;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_fragment, parent, false);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            view.setLayoutParams(lp);

            return new ViewHolder(view);
        }

        @SuppressLint("SetTextI18n")
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            View view = holder.itemView;

            final CardView root = view.findViewById(R.id.root_layout);
            final TextView customerName = view.findViewById(R.id.customer_name);
            final TextView dueDate = view.findViewById(R.id.due_date);
            final TextView connected = view.findViewById(R.id.connected);
            final TextView toPay = view.findViewById(R.id.to_pay);
            final TextView status = view.findViewById(R.id.status);
            final TextView dueDateTxt = view.findViewById(R.id.due_dateTxt);
            final TextView connectedTxt = view.findViewById(R.id.connectedTxt);
            final TextView toPayTxt = view.findViewById(R.id.to_payTxt);
            final TextView statusTxt = view.findViewById(R.id.statusTxt);
            final TextView devicesTxt = view.findViewById(R.id.devicesTxt);
            final Spinner devices = view.findViewById(R.id.devices);
            final ImageButton delete = view.findViewById(R.id.delete);
            final ImageButton editBtn = view.findViewById(R.id.edit_btn);

            String statusStr = Objects.requireNonNull(listInfos.get(position).get(Util.STATUS_KEY)).toString();
            String connectedStr = Objects.requireNonNull(listInfos.get(position).get(Util.CONNECTED_KEY)).toString();
            String toPayStr = Objects.requireNonNull(listInfos.get(position).get(Util.TOPAY_KEY)).toString();
            String dueDateStr = Objects.requireNonNull(listInfos.get(position).get(Util.DUEDATE_KEY)).toString();
            String registeredMonthStr = Objects.requireNonNull(listInfos.get(position).get(Util.REG_MOS)).toString();

            final int dueDateInt = Util.isInteger(dueDateStr) ? Integer.parseInt(dueDateStr) : (int) Double.parseDouble(dueDateStr);
            final int regMos = Util.isInteger(registeredMonthStr) ?
                    Integer.parseInt(registeredMonthStr) :
                    (int) Double.parseDouble(registeredMonthStr);

            customerName.setText(Objects.requireNonNull(listInfos.get(position).get(Util.NAME_KEY)).toString());

            //noinspection MagicConstant
            if (regMos == calendar.get(Calendar.MONTH)) {
                dueDate.setText(Objects.requireNonNull(listInfos.get(position).get(Util.DUEDATE_KEY)) + " of " + Util.getStrMonth(Util.StringToInteger(Objects.requireNonNull(listInfos.get(position).get(Util.REG_MOS)).toString()) + 1));
            } else {
                dueDate.setText(Objects.requireNonNull(listInfos.get(position).get(Util.DUEDATE_KEY)) + " of " +
                        Util.getStrMonth(calendar.get(Calendar.DAY_OF_MONTH)>dueDateInt?calendar.get(Calendar.MONTH)+1:calendar.get(Calendar.MONTH)));
            }

            connected.setText(Objects.requireNonNull(listInfos.get(position).get(Util.CONNECTED_KEY)) + "x Person(s)");
            toPay.setText((Util.isInteger(toPayStr) ? Integer.parseInt(toPayStr) : (int) Double.parseDouble(toPayStr)) + " PHP");
            status.setText(Objects.requireNonNull(listInfos.get(position).get(Util.STATUS_KEY)).toString());

            switch (statusStr) {
                case "PAID":
                    status.setTextColor(PAID_COLOR_INDICATOR);
                    // if from adding customer infos, dont need to recheck the status. break it;
                    if (isFromAddingCustomer)
                        break;

                    _recheckUserStatus(status, toPay, dueDate, connectedStr, toPayStr, dueDateStr, registeredMonthStr, position);
                    break;
                case "NOT PAID":
                    status.setTextColor(NOT_PAID_COLOR_INDICATOR);
                    // if from adding customer infos, dont need to recheck the status. break it;
                    if (isFromAddingCustomer)
                        break;

                    // updates the amount balance to pay
                    final int balanceAmnt = Util.isInteger(toPayStr) ? Integer.parseInt(toPayStr) : (int) Double.parseDouble(toPayStr);
                    final int connectedInt = Util.isInteger(connectedStr) ?
                            Integer.parseInt(connectedStr) :
                            (int) Double.parseDouble(connectedStr);
                    final int mosLastChecked = cache.getInt(Util.LAST_CHECK_KEY, -1);
                    final int mosToday = calendar.get(Calendar.MONTH);
                    final int dayToday = calendar.get(Calendar.DAY_OF_MONTH);

                    if (mosToday == regMos)
                        break;

                    if (mosLastChecked == -1)
                        break;

                    int mosDelayed = mosToday < regMos ?
                            Math.abs((mosToday-regMos)+12) :
                            mosToday - regMos;

                    if (mosDelayed != 0) {
                        dueDate.setText(dueDate.getText().toString() +
                                " (" + (calendar.get(Calendar.DAY_OF_MONTH)>=dueDateInt?mosDelayed:mosDelayed-1) + " month(s) not paid)");
                    }

                    if (mosToday > mosLastChecked || mosLastChecked > mosToday) {
                        if (dayToday >= dueDateInt) {
                            mMosLastChecked = mosToday;

                            int diff = mosToday < mosLastChecked ? Math.abs((mosToday-mosLastChecked)+12):mosToday - mosLastChecked;
                            int newBalance = diff * (Util.PRICE * connectedInt) + balanceAmnt;

                            toPay.setText(newBalance + " PHP");
                            listInfos.get(position).put(Util.TOPAY_KEY, newBalance);
                        }
                    }

                    break;
                case "NOT FULLY PAID":
                    status.setTextColor(NOT_FULLY_PAID_COLOR_INDICATOR);
                    // if from adding customer infos, dont need to recheck the status. break it;
                    if (isFromAddingCustomer)
                        break;

                    _recheckUserStatus(status, toPay, dueDate, connectedStr, toPayStr, dueDateStr, registeredMonthStr, position);
                    break;
                default:
                    status.setText("UNKNOWN");
                    break;
            }

            List<String> devicesLst = new Gson().fromJson(Objects.requireNonNull(listInfos.get(position).get(Util.DEVICES_KEY)).toString(), new TypeToken<List<String>>() {}.getType());
            if (!Objects.requireNonNull(listInfos.get(position).get(Util.DEVICES_KEY)).toString().isEmpty()) {
                devices.setAdapter(new ArrayAdapter<>(getApplicationContext(), R.layout.spinner_ref, devicesLst));
                ((ArrayAdapter)devices.getAdapter()).notifyDataSetChanged();
            }

            // sets the background to white if the bg is red
            if (root.getCardBackgroundColor().getDefaultColor() == ContextCompat.getColor(getApplicationContext(), R.color.red)) {
                root.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
            }

            // # region: Listeners
            root.setOnLongClickListener(view1 -> {
                delete.setVisibility(View.VISIBLE);
                editBtn.setVisibility(View.VISIBLE);
                root.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
                return true;
            });

            root.setOnClickListener(view13 -> {
                if (delete.getVisibility() == View.VISIBLE) {
                    delete.setVisibility(View.GONE);
                    editBtn.setVisibility(View.GONE);
                    root.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.white));
                }
            });

            delete.setOnClickListener(view12 -> {
                // *** add to history cache ***
                if (devicesLst != null) {
                    List<String> historyList = new Gson().fromJson(cache.getString(Util.historyKey, ""), new TypeToken<List<String>>() {
                    }.getType());
                    if (historyList == null)
                        historyList = new ArrayList<>();
                    // checks if the device is already exist in the history cache
                    // if the device already existed then, dont add it.

                    for (String device : devicesLst) {
                        String deviceNoSpc = device.replaceAll("\\s", "");
                        if (historyList.contains(deviceNoSpc))
                            continue;

                        historyList.add(deviceNoSpc);
                    }
                    // store the devices to history cache.
                    cache.edit().putString(Util.historyKey, new Gson().toJson(historyList)).apply();
                }
                // *** *** *** *** *** *** *** ***
                listInfos.remove(position);
                if (!(listInfos.size() > 0)) {
                    cache.edit().putString(Util.cacheKey, "").apply();
                    cache.edit().putString(Util.isWorkEnabledKey, "").apply();
                    Util.disableBroadcastReceiver(getApplicationContext());
                    Util.disableWork(getApplicationContext());
                }
                else
                    cache.edit().putString(Util.cacheKey, new Gson().toJson(listInfos)).apply();

                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount() - position);
            });

            editBtn.setOnClickListener(view14 -> new UserFormDialog(MainActivity.this, listInfos.get(position), position, (isFromEdit, _position, _name, _dueDate, _connected, _toPay, _status, _monthOf, _devices) -> {
                // on save btn clicked
                if (isFromEdit) {
                    listInfos.set(_position, infoToMap(_monthOf, _name, _dueDate, _connected, _toPay, _status, _devices));
                    // save to cache
                    cache.edit().putString(Util.cacheKey, new Gson().toJson(listInfos)).apply();

                    notifyItemChanged(_position);
                    notifyItemRangeChanged(_position, getItemCount() - _position);
                }
            }).show());

            connectedTxt.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                // onGlobalLayout
                int connectedTxtWidth = connectedTxt.getMeasuredWidth();
                dueDateTxt.getLayoutParams().width = dueDateTxt.getMeasuredWidth() + (connectedTxtWidth-dueDateTxt.getMeasuredWidth());
                dueDateTxt.requestLayout();

                toPayTxt.getLayoutParams().width = toPayTxt.getMeasuredWidth() + (connectedTxtWidth-toPayTxt.getMeasuredWidth());
                toPayTxt.requestLayout();

                statusTxt.getLayoutParams().width = statusTxt.getMeasuredWidth() + (connectedTxtWidth-statusTxt.getMeasuredWidth());
                statusTxt.requestLayout();

                devicesTxt.getLayoutParams().width = devicesTxt.getMeasuredWidth() + (connectedTxtWidth-devicesTxt.getMeasuredWidth());
                devicesTxt.requestLayout();
            });
            // # end-region: Listeners
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        @SuppressWarnings("MagicConstant")
        @SuppressLint("SetTextI18n")
        private void _recheckUserStatus(TextView view, TextView toPayTextView, TextView dueDateTxt, @NonNull String connectedTxt, @NonNull String toPayTxt, String dueDateStr, String registeredMonthStr, int _position) {
            final int dueDate = Util.isInteger(dueDateStr) ? Integer.parseInt(dueDateStr) : (int) Double.parseDouble(dueDateStr);
            final int connected = Util.isInteger(connectedTxt) ?
                    Integer.parseInt(connectedTxt) :
                    (int) Double.parseDouble(connectedTxt);
            final int balanceAmnt = Util.isInteger(toPayTxt) ?
                    Integer.parseInt(toPayTxt) :
                    (int) Double.parseDouble(toPayTxt);

            int registeredMonth;

            if (Util.isInteger(registeredMonthStr))
                registeredMonth = Integer.parseInt(registeredMonthStr);
            else
                registeredMonth = (int) Double.parseDouble(registeredMonthStr);

            if (calendar.get(Calendar.MONTH) != registeredMonth) {
                mMosLastChecked = calendar.get(Calendar.MONTH);
                /* subt -> used to substract the var. "diff"
                * @Values:
                *   1 ==> if the current day is less than the dueDate(day) of the client,
                *         we need to subtract it with 1 so the program wont add another bill before the next dueDate
                *   0 ==> if the current day is bigger or equal than the dueDate(day) of the client,
                *         we dont need need to subtract the diff so it can add another bill bcuz its his/her due or past his/her due.
                */
                final int subt = calendar.get(Calendar.DAY_OF_MONTH) >= dueDate ? 0 : 1;
                // diff -> the difference between the num of current month to the num of month, of when was the last time the user paid the bill.
                int diff = calendar.get(Calendar.MONTH) < registeredMonth ? Math.abs((calendar.get(Calendar.MONTH)-registeredMonth)+12):calendar.get(Calendar.MONTH) - registeredMonth;
                // calculate the months that was delayed or the months that was not paid.
                int mosDelayed = calendar.get(Calendar.MONTH) < registeredMonth ?
                        Math.abs((calendar.get(Calendar.MONTH)-registeredMonth)+12) :
                        calendar.get(Calendar.MONTH) - registeredMonth;

                // set the new balance and display it on the UI
                final int newBalanceInt = (diff - subt) * (Util.PRICE * connected) + balanceAmnt;
                final String newBalanceStr = String.valueOf(newBalanceInt);
                // but if there is no new balance then just return it.
                if (newBalanceInt == balanceAmnt)
                    return;

                // diplays how many months the client delayed his/her bills
                if (mosDelayed != 0) {
                    dueDateTxt.setText(dueDateTxt.getText().toString() +
                            " (" + (calendar.get(Calendar.DAY_OF_MONTH)>=dueDate?mosDelayed:mosDelayed-1) + " month(s) not paid)");
                }

                toPayTextView.setText(newBalanceStr + " PHP");
                view.setText("NOT PAID");
                view.setTextColor(NOT_PAID_COLOR_INDICATOR);
                listInfos.get(_position).put(Util.STATUS_KEY, "NOT PAID");
                listInfos.get(_position).put(Util.TOPAY_KEY, newBalanceStr);
                cache.edit().putString(Util.cacheKey, new Gson().toJson(listInfos)).apply();
            }
        }
    }
}