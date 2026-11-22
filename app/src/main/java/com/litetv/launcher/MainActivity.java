package com.litetv.launcher;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Launcher TV yang ringan: jam, lalu grid aplikasi yang bisa dinavigasi dengan D-pad. */
public class MainActivity extends Activity {

    private static final class Entry {
        final String label;
        final Drawable icon;
        final ComponentName component;
        Entry(String label, Drawable icon, ComponentName component) {
            this.label = label;
            this.icon = icon;
            this.component = component;
        }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private AppAdapter adapter;
    private boolean receiverRegistered = false;
    private volatile int loadToken = 0;

    private final BroadcastReceiver packageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            loadApps();
        }
    };

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#0F172A"), Color.parseColor("#020617")});

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackground(bg);
        root.setPadding(dp(48), dp(28), dp(48), dp(16));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("LiteTV");
        title.setTextColor(Color.parseColor("#94A3B8"));
        title.setTextSize(20);
        header.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        LinearLayout clockBox = new LinearLayout(this);
        clockBox.setOrientation(LinearLayout.VERTICAL);
        clockBox.setGravity(Gravity.END);

        TextClock clock = new TextClock(this);
        clock.setFormat24Hour("HH:mm");
        clock.setFormat12Hour("HH:mm");
        clock.setTextColor(Color.WHITE);
        clock.setTextSize(44);
        clock.setGravity(Gravity.END);
        clockBox.addView(clock);

        TextClock date = new TextClock(this);
        date.setFormat24Hour("EEEE, d MMMM yyyy");
        date.setFormat12Hour("EEEE, d MMMM yyyy");
        date.setTextColor(Color.parseColor("#94A3B8"));
        date.setTextSize(16);
        date.setGravity(Gravity.END);
        clockBox.addView(date);

        header.addView(clockBox, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(header, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        GradientDrawable selector = new GradientDrawable();
        selector.setColor(Color.parseColor("#33FFFFFF"));
        selector.setCornerRadius(dp(14));
        selector.setStroke(dp(2), Color.parseColor("#80FFFFFF"));

        GridView grid = new GridView(this);
        grid.setNumColumns(GridView.AUTO_FIT);
        grid.setColumnWidth(dp(150));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setVerticalSpacing(dp(12));
        grid.setHorizontalSpacing(dp(12));
        grid.setSelector(selector);
        grid.setClipToPadding(false);
        grid.setPadding(0, dp(24), 0, dp(8));
        grid.setVerticalScrollBarEnabled(false);
        grid.setFocusable(true);
        grid.setFocusableInTouchMode(true);

        adapter = new AppAdapter();
        grid.setAdapter(adapter);
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                launch(position);
            }
        });
        grid.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                openInfo(position);
                return true;
            }
        });

        root.addView(grid, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
        grid.requestFocus();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!receiverRegistered) {
            IntentFilter f = new IntentFilter();
            f.addAction(Intent.ACTION_PACKAGE_ADDED);
            f.addAction(Intent.ACTION_PACKAGE_REMOVED);
            f.addAction(Intent.ACTION_PACKAGE_CHANGED);
            f.addDataScheme("package");
            registerReceiver(packageReceiver, f);
            receiverRegistered = true;
        }
        loadApps();
    }

    @Override
    protected void onStop() {
        if (receiverRegistered) {
            try {
                unregisterReceiver(packageReceiver);
            } catch (IllegalArgumentException ignored) {
            }
            receiverRegistered = false;
        }
        super.onStop();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        // Tombol Home ditekan saat launcher sudah terbuka: kembali ke awal daftar.
        View grid = findViewById(android.R.id.content);
        if (grid != null) grid.requestFocus();
    }

    @Override
    public void onBackPressed() {
        // Launcher tidak boleh keluar dengan tombol Back.
    }

    private void loadApps() {
        final int token = ++loadToken;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<Entry> result = queryApps();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (token != loadToken || isFinishing()) return;
                        entries.clear();
                        entries.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                });
            }
        }, "litetv-load").start();
    }

    private List<Entry> queryApps() {
        PackageManager pm = getPackageManager();
        List<Entry> out = new ArrayList<Entry>();
        Set<String> seen = new HashSet<String>();
        String[] categories = {Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER};
        for (String category : categories) {
            Intent q = new Intent(Intent.ACTION_MAIN).addCategory(category);
            List<ResolveInfo> list = pm.queryIntentActivities(q, 0);
            for (ResolveInfo ri : list) {
                if (ri.activityInfo == null) continue;
                String pkg = ri.activityInfo.packageName;
                if (pkg.equals(getPackageName())) continue;
                // Satu entri per aplikasi; versi Leanback diutamakan karena diproses lebih dulu.
                if (!seen.add(pkg)) continue;
                CharSequence label = ri.loadLabel(pm);
                Drawable icon = null;
                try {
                    icon = ri.loadIcon(pm);
                } catch (Throwable ignored) {
                }
                out.add(new Entry(label != null ? label.toString() : pkg, icon,
                        new ComponentName(pkg, ri.activityInfo.name)));
            }
        }
        Collections.sort(out, new Comparator<Entry>() {
            @Override
            public int compare(Entry a, Entry b) {
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    private void launch(int position) {
        if (position < 0 || position >= entries.size()) return;
        Intent i = new Intent(Intent.ACTION_MAIN);
        i.setComponent(entries.get(position).component);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
        try {
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private void openInfo(int position) {
        if (position < 0 || position >= entries.size()) return;
        Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + entries.get(position).component.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(i);
        } catch (Throwable ignored) {
        }
    }

    private final class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return entries.size();
        }

        @Override
        public Object getItem(int position) {
            return entries.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            ImageView icon;
            TextView label;
            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setGravity(Gravity.CENTER_HORIZONTAL);
                row.setPadding(dp(8), dp(14), dp(8), dp(10));

                icon = new ImageView(MainActivity.this);
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);
                row.addView(icon, new LinearLayout.LayoutParams(dp(64), dp(64)));

                label = new TextView(MainActivity.this);
                label.setTextColor(Color.WHITE);
                label.setTextSize(14);
                label.setGravity(Gravity.CENTER);
                label.setSingleLine(true);
                label.setEllipsize(android.text.TextUtils.TruncateAt.END);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                lp.topMargin = dp(8);
                row.addView(label, lp);

                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(120)));
                row.setTag(new View[]{icon, label});
            } else {
                row = (LinearLayout) convertView;
                View[] parts = (View[]) row.getTag();
                icon = (ImageView) parts[0];
                label = (TextView) parts[1];
            }
            Entry e = entries.get(position);
            icon.setImageDrawable(e.icon);
            label.setText(e.label);
            return row;
        }
    }
}
