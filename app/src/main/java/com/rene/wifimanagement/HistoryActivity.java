package com.rene.wifimanagement;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class HistoryActivity extends AppCompatActivity {

    private List<String> devicesLst;

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.history_layout);

        final RecyclerView devicesRecyclerview = findViewById(R.id.devicesRecyclerview);
        // get the device list of the user from history cache
        devicesLst = new Gson().fromJson(Util.Cache.Instance(this).getString(Util.historyKey, ""),
                new TypeToken<List<String>>(){}.getType());

        if (devicesLst == null) // just return if the devices list is empty.
            return;

        // otherwise, set recyclerview's data
        devicesRecyclerview.setHasFixedSize(true);
        devicesRecyclerview.setAdapter(new DevicesAdapter(devicesLst));
        devicesRecyclerview.setLayoutManager(new LinearLayoutManager(this));
        Objects.requireNonNull(devicesRecyclerview.getAdapter()).notifyDataSetChanged();
    }

    public class DevicesAdapter extends RecyclerView.Adapter<DevicesAdapter.ViewHolder> {

        List<String> data;

        public DevicesAdapter(List<String> _data) {
            data = _data;
        }

        @NonNull
        @Override
        public DevicesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.history_fragment, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DevicesAdapter.ViewHolder holder, int position) {
            View view = holder.itemView;

            final CardView root = view.findViewById(R.id.root);
            final ImageButton deleteBtn = view.findViewById(R.id.delete_btn);
            final TextView text = view.findViewById(R.id.text);

            text.setText(devicesLst.get(position));

            // if the indicator for the deletion is active then just hide it
            if (_isShowDeletionIndicator(root)) {
                _hideDeletionIndicator(root);
                deleteBtn.setVisibility(View.GONE);
            }

            root.setOnClickListener(view1 -> {
                if (_isShowDeletionIndicator(root)) {
                    _hideDeletionIndicator(root);
                    deleteBtn.setVisibility(View.GONE);
                }
            });

            root.setOnLongClickListener(view2 -> {
                _showDeletionIndicator(root);
                deleteBtn.setVisibility(View.VISIBLE);
                return true;
            });

            deleteBtn.setOnClickListener(view3 -> {
                devicesLst.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, getItemCount() - position);

                Util.Cache.Instance(getApplicationContext())
                        .edit().putString(Util.historyKey, new Gson().toJson(devicesLst)).apply();
            });

        }

        private void _showDeletionIndicator(@NonNull CardView view) {
            view.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.red));
        }

        private void _hideDeletionIndicator(@NonNull CardView view) {
            view.setCardBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.primaryColor));
        }

        private boolean _isShowDeletionIndicator(@NonNull CardView root) {
            return root.getCardBackgroundColor() == ColorStateList.valueOf(ContextCompat.getColor(getApplicationContext(), R.color.red));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        public class ViewHolder extends RecyclerView.ViewHolder {
            public ViewHolder(View _itemView) {
                super(_itemView);
            }
        }
    }
}
