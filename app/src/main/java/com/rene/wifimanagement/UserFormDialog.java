package com.rene.wifimanagement;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class UserFormDialog extends Dialog {

    private EditText name;
    private EditText dueDate;
    private EditText connected;
    private EditText toPay;
    private Spinner status;
    private EditText devices;
    private Spinner monthOf;

    private final Context context;

    private final int position;
    private boolean hasError = false;
    private final HashMap<String, Object> informations;

    private final SaveBtnListener listener;
    public interface SaveBtnListener {
        void OnClick(boolean isFromEdit, int _position, String _name, String _dueDate, String _connected, String _toPay, String _status, String _month, String _devices);
    }

    public UserFormDialog(@NonNull Context _context, HashMap<String, Object> infos, int pos, SaveBtnListener _listener) {
        super(_context);
        position = pos;
        context = _context;
        informations = infos;
        listener = _listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_custom_layout);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setLayout((LinearLayout.LayoutParams.MATCH_PARENT), LinearLayout.LayoutParams.WRAP_CONTENT);
        getWindow().setDimAmount(0.3f);

        name = findViewById(R.id.customer_name_edit);
        dueDate = findViewById(R.id.due_date_edit);
        connected = findViewById(R.id.connected_edit);
        toPay = findViewById( R.id.to_pay_edit);
        status = findViewById(R.id.status_edit);
        devices = findViewById(R.id.devices_edit);
        monthOf = findViewById(R.id.month_of_edit);
        Button saveBtn = findViewById(R.id.save_btn);

        List<String> statusLst = new ArrayList<>();
        statusLst.add("PAID");
        statusLst.add("NOT PAID");
        statusLst.add("NOT FULLY PAID");

        String lastStatus = "";
        int regMosInt = -1;
        List<String> monthsLst = _Months();

        final Calendar calendar = Calendar.getInstance();
        final int monthToday = calendar.get(Calendar.MONTH);
        final int dayToday = calendar.get(Calendar.DAY_OF_MONTH);
        final int maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

        status.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_ref, statusLst));
        ((ArrayAdapter)status.getAdapter()).notifyDataSetChanged();

        monthOf.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_ref, monthsLst));
        ((ArrayAdapter)monthOf.getAdapter()).notifyDataSetChanged();

        if (informations != null) {
            lastStatus = Objects.requireNonNull(informations.get(Util.STATUS_KEY)).toString();
            String regMos = Objects.requireNonNull(informations.get(Util.REG_MOS)).toString();
            regMosInt = Util.isInteger(regMos) ?
                    Integer.parseInt(regMos) :
                    (int) Double.parseDouble(regMos);
            _setupInformation();
        }
        final String finalLastStatus = lastStatus;
        final int finalRegMosInt = regMosInt;

        _initTextWatchers();
        saveBtn.setOnClickListener(view -> {
            if (name.getText().toString().isEmpty()) {
                _showError(name);
                hasError = true;
            }
            int inputDueDate = Integer.parseInt(dueDate.getText().toString());
            if (dueDate.getText().toString().isEmpty() || !Util.isNumeric(dueDate.getText().toString()) || inputDueDate > maxDays) {
                _showError(dueDate);
                hasError = true;
            }
            if (connected.getText().toString().isEmpty()) {
                _showError(connected);
                hasError = true;
            }
            if (toPay.getText().toString().isEmpty()) {
                _showError(toPay);
                hasError = true;
            }

            if (hasError)
                return;

            // automatically update the month and balance if the admin forgot to update it. => Edit Mode
            if (informations != null) {
                final int dueDate = Util.isInteger(Objects.requireNonNull(informations.get(Util.DUEDATE_KEY)).toString()) ?
                        Integer.parseInt(Objects.requireNonNull(informations.get(Util.DUEDATE_KEY)).toString()) :
                        (int) Double.parseDouble(Objects.requireNonNull(informations.get(Util.DUEDATE_KEY)).toString());

                if ((finalLastStatus.equals("NOT PAID") && status.getSelectedItem().toString().equals("PAID")) ||
                    (finalLastStatus.equals("NOT FULLY PAID") && status.getSelectedItem().toString().equals("PAID"))) {
                    if (!toPay.getText().toString().equals("0"))
                        toPay.setText("0");
                }

                if (!finalLastStatus.equals(status.getSelectedItem().toString()) &&
                        Util.StrToNumMonth(monthOf.getSelectedItem().toString()) != monthToday)
                    monthOf.setSelection(!(dayToday < dueDate)? monthToday : monthToday-1);

                if (finalLastStatus.equals(status.getSelectedItem().toString()) &&
                        Util.StrToNumMonth(monthOf.getSelectedItem().toString()) != finalRegMosInt)
                    monthOf.setSelection(finalRegMosInt);
            }
            // Add Mode
            boolean isShowingWarning = false;
            if (informations == null) {
                if (Util.StrToNumMonth(monthOf.getSelectedItem().toString()) != monthToday) {
                    isShowingWarning = true;
                    final AlertDialog.Builder alertWarning = new AlertDialog.Builder(context).setTitle("WARNING!");
                            alertWarning.setMessage("Your selected month is not the same as the month today, Are you sure to continue?");
                            alertWarning.setNegativeButton("Cancel", (dialogInterface, i) -> {});
                            alertWarning.setPositiveButton("Continue", (dialogInterface, i) -> {
                                // onClick
                                listener.OnClick(false, position, name.getText().toString(), dueDate.getText().toString(), connected.getText().toString(), toPay.getText().toString(), status.getSelectedItem().toString(), String.valueOf(Util.StrToNumMonth(monthOf.getSelectedItem().toString())), devices.getText().toString());
                                dismiss();
                            });
                    alertWarning.setCancelable(false);
                    alertWarning.show();
                }
            }

            if (isShowingWarning)
                return;

            listener.OnClick(informations != null, position, name.getText().toString(), dueDate.getText().toString(), connected.getText().toString(), toPay.getText().toString(), status.getSelectedItem().toString(), String.valueOf(Util.StrToNumMonth(monthOf.getSelectedItem().toString())), devices.getText().toString());
            dismiss();
        });
    }
    // setups information if it is for editing info
    private void _setupInformation() {
        name.setText(Objects.requireNonNull(informations.get(Util.NAME_KEY)).toString());
        dueDate.setText(Objects.requireNonNull(informations.get(Util.DUEDATE_KEY)).toString());
        connected.setText(Objects.requireNonNull(informations.get(Util.CONNECTED_KEY)).toString());
        toPay.setText(Objects.requireNonNull(informations.get(Util.TOPAY_KEY)).toString());
        if (Objects.requireNonNull(informations.get(Util.STATUS_KEY)).toString().equals("PAID"))
            status.setSelection(0);
        else if (Objects.requireNonNull(informations.get(Util.STATUS_KEY)).toString().equals("NOT PAID"))
            status.setSelection(1);
        else if (Objects.requireNonNull(informations.get(Util.STATUS_KEY)).toString().equals("NOT FULLY PAID"))
            status.setSelection(2);

        final int regMonth = Util.isInteger(Objects.requireNonNull(informations.get(Util.REG_MOS)).toString())?
                                Integer.parseInt(Objects.requireNonNull(informations.get(Util.REG_MOS)).toString()) :
                                (int) Double.parseDouble(Objects.requireNonNull(informations.get(Util.REG_MOS)).toString());

        monthOf.setSelection(regMonth);

        if (Objects.requireNonNull(informations.get(Util.DEVICES_KEY)).toString().isEmpty())
            return;

        final List<String> devicesLst = new Gson().fromJson(Objects.requireNonNull(informations.get(Util.DEVICES_KEY)).toString(), new TypeToken<List<String>>() {}.getType());
        devices.setText(_DevicesListToStringConversion(devicesLst));
    }

    @NonNull
    private List<String> _Months() {
        final List<String> list = new ArrayList<>();
        for (int i=0; i<12; i++)
            list.add(Util.getStrMonth(i));

        return list;
    }

    @NonNull
    private String _DevicesListToStringConversion(@NonNull List<String> listStr) {
        StringBuilder devices = new StringBuilder();
        int i = 0;
        for (String str : listStr) {
            if (i == listStr.size()-1) {
                devices.append(str);
                break;
            }

            devices.append(str).append(", ");
            i++;
        }

        return devices.toString();
    }

    private void _initTextWatchers() {
        name.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                _hideError(name);
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
        dueDate.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                _hideError(dueDate);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        connected.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                _hideError(connected);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        toPay.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                _hideError(toPay);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        devices.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                _hideError(devices);
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
    }

    private void _showError(@NonNull EditText editText) {
        editText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.red)));
        @SuppressLint("Recycle") final ObjectAnimator animateError = ObjectAnimator.ofFloat(editText, "translationX", -2, 2);
        animateError.setDuration(200);
        animateError.setRepeatCount(4);
        animateError.start();

        animateError.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                editText.setTranslationX(0);
            }
        });

    }

    private void _hideError(@NonNull EditText editText) {
        if (editText.getBackgroundTintList().getDefaultColor() == ContextCompat.getColor(context, R.color.red)) {
            editText.setBackgroundTintList(ColorStateList.valueOf(ContextCompat.getColor(context, R.color.primaryColor)));
            hasError = false;
        }
    }
}
