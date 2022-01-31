package com.rene.wifimanagement;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
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

        List<String> monthsLst = _Months();

        status.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_ref, statusLst));
        ((ArrayAdapter)status.getAdapter()).notifyDataSetChanged();

        monthOf.setAdapter(new ArrayAdapter<>(context, R.layout.spinner_ref, monthsLst));
        ((ArrayAdapter)monthOf.getAdapter()).notifyDataSetChanged();

        if (informations != null)
            _setupInformation();

        _initTextWatchers();
        saveBtn.setOnClickListener(view -> {
            if (name.getText().toString().isEmpty()) {
                _showError(name);
                hasError = true;
            }
            if (dueDate.getText().toString().isEmpty() || !Util.isNumeric(dueDate.getText().toString())) {
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
