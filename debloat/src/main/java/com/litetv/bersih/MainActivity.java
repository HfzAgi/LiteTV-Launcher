package com.litetv.bersih;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.EditText;
import android.text.InputType;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** BersihSTB: nonaktifkan aplikasi bawaan STB lewat root, selalu bisa dipulihkan. */
public class MainActivity extends Activity {

    private static final int F_SUGGESTED = 0;
    private static final int F_ALL = 1;
    private static final int F_DISABLED = 2;
    private static final int F_PROTECTED = 3;
    private static final int F_PROFILE = 4;
    private static final String[] FILTER_NAMES = {"Disarankan", "Semua", "Nonaktif", "Dilindungi", "Profil"};

    private final Handler ui = new Handler(Looper.getMainLooper());
    private final List<AppItem> all = new ArrayList<AppItem>();
    private final List<AppItem> shown = new ArrayList<AppItem>();
    private int filter = F_SUGGESTED;
    private Boolean root = null; // null = sedang dicek
    private boolean busy = false;
    private volatile boolean adbConnected = false;

    private TextView status;
    private TextView runBtn;
    private final TextView[] filterBtns = new TextView[5];
    private final Set<String> profilePkgs = new HashSet<String>();
    private String profileId;
    private String profileName = "";
    private AppAdapter adapter;

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    // ---------- UI ----------

    private TextView makeButton(String text, final Runnable action) {
        TextView b = new TextView(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(18), dp(10), dp(18), dp(10));
        b.setFocusable(true);
        b.setClickable(true);

        GradientDrawable normal = new GradientDrawable();
        normal.setColor(Color.parseColor("#1E293B"));
        normal.setCornerRadius(dp(10));
        GradientDrawable focused = new GradientDrawable();
        focused.setColor(Color.parseColor("#0284C7"));
        focused.setCornerRadius(dp(10));
        focused.setStroke(dp(2), Color.WHITE);
        StateListDrawable bg = new StateListDrawable();
        bg.addState(new int[]{android.R.attr.state_focused}, focused);
        bg.addState(new int[]{}, normal);
        b.setBackground(bg);

        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                action.run();
            }
        });
        return b;
    }

    private LinearLayout.LayoutParams gap() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = dp(10);
        return lp;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout rootView = new LinearLayout(this);
        rootView.setOrientation(LinearLayout.VERTICAL);
        rootView.setBackground(new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#0F172A"), Color.parseColor("#020617")}));
        rootView.setPadding(dp(40), dp(20), dp(40), dp(12));

        TextView title = new TextView(this);
        title.setText("BersihSTB");
        title.setTextColor(Color.WHITE);
        title.setTextSize(26);
        rootView.addView(title);

        status = new TextView(this);
        status.setTextColor(Color.parseColor("#94A3B8"));
        status.setTextSize(14);
        status.setPadding(0, dp(2), 0, dp(10));
        rootView.addView(status);

        LinearLayout filters = new LinearLayout(this);
        filters.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < FILTER_NAMES.length; i++) {
            final int idx = i;
            filterBtns[i] = makeButton(FILTER_NAMES[i], new Runnable() {
                @Override
                public void run() {
                    setFilter(idx);
                }
            });
            filters.addView(filterBtns[i], gap());
        }
        rootView.addView(filters);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setPadding(0, dp(8), 0, 0);
        actions.addView(makeButton("Sambungkan ADB", new Runnable() {
            @Override
            public void run() {
                connectAdb();
            }
        }), gap());
        actions.addView(makeButton("Pilih profil", new Runnable() {
            @Override
            public void run() {
                chooseProfile();
            }
        }), gap());
        actions.addView(makeButton("Pilih semua", new Runnable() {
            @Override
            public void run() {
                toggleSelectAll();
            }
        }), gap());
        runBtn = makeButton("Nonaktifkan", new Runnable() {
            @Override
            public void run() {
                onRun();
            }
        });
        actions.addView(runBtn, gap());
        actions.addView(makeButton("Pulihkan semua", new Runnable() {
            @Override
            public void run() {
                onRestoreAll();
            }
        }), gap());
        actions.addView(makeButton("Muat ulang", new Runnable() {
            @Override
            public void run() {
                scan();
            }
        }), gap());
        rootView.addView(actions);

        GradientDrawable selector = new GradientDrawable();
        selector.setColor(Color.parseColor("#33FFFFFF"));
        selector.setCornerRadius(dp(10));
        selector.setStroke(dp(2), Color.parseColor("#80FFFFFF"));

        ListView list = new ListView(this);
        list.setSelector(selector);
        list.setDivider(null);
        list.setVerticalScrollBarEnabled(false);
        list.setPadding(0, dp(10), 0, 0);
        list.setClipToPadding(false);
        adapter = new AppAdapter();
        list.setAdapter(adapter);
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onRowClick(position);
            }
        });
        rootView.addView(list, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(rootView);
        initProfile();
        refreshChrome();

        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = RootShell.isRootAvailable();
                Runner.root = ok;
                final boolean adbOk = !ok && AdbManager.connectSmart(MainActivity.this) == AdbManager.CONNECTED;
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        root = ok;
                        adbConnected = adbOk;
                        refreshChrome();
                    }
                });
            }
        }, "root-check").start();
        scan();

        if (getIntent() != null && getIntent().hasExtra("selftest")) runSelfTest();
    }

    /** Dipakai CI: sambung ke adbd lokal dan jalankan "id". Hasil ditulis ke logcat (tag BERSIH). */
    private void runSelfTest() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    AdbManager m = AdbManager.get(MainActivity.this);
                    boolean c = m.isConnected() || m.connect("127.0.0.1", 5555);
                    if (!c) {
                        Log.i("BERSIH", "SELFTEST GAGAL: tidak bisa sambung");
                        return;
                    }
                    RootShell.Result r = AdbManager.shell(MainActivity.this, "id", 10000);
                    Log.i("BERSIH", "SELFTEST " + (r.out.contains("uid=") ? "OK " : "GAGAL ") + r.out);
                } catch (Throwable t) {
                    Log.i("BERSIH", "SELFTEST GAGAL: " + t);
                }
            }
        }, "selftest").start();
    }

    // ---------- Sambung ADB ----------

    private void connectAdb() {
        if (busy) return;
        if (root != null && root) {
            toast("Root sudah aktif, tidak perlu ADB.");
            return;
        }
        busy = true;
        status.setText("Menyambung ke ADB STB…");
        new Thread(new Runnable() {
            @Override
            public void run() {
                final int r = AdbManager.connectSmart(MainActivity.this);
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        adbConnected = r == AdbManager.CONNECTED;
                        refreshChrome();
                        if (r == AdbManager.CONNECTED) {
                            toast("ADB terhubung.");
                        } else if (r == AdbManager.PAIRING_REQUIRED) {
                            askPairingCode();
                        } else {
                            showAdbHelp();
                        }
                    }
                });
            }
        }, "adb-connect").start();
    }

    private void showAdbHelp() {
        new AlertDialog.Builder(this).setTitle("ADB belum bisa tersambung")
                .setMessage("Di STB: Pengaturan → Preferensi Perangkat → Tentang → tekan \"Build\" 7 kali.\n"
                        + "Lalu Opsi pengembang → aktifkan \"USB debugging\" dan/atau \"Wireless debugging\".\n"
                        + "Jika muncul \"Allow USB debugging?\", pilih Allow (centang \"Always allow\").\n\n"
                        + "Lalu tekan \"Sambungkan ADB\" lagi. Untuk Wireless debugging, buka \"Pair device with pairing code\" "
                        + "dan masukkan kodenya di sini.")
                .setNegativeButton("Tutup", null)
                .setPositiveButton("Masukkan kode pairing", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        askPairingCode();
                    }
                }).show();
    }

    private void askPairingCode() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("Kode 6 digit");
        new AlertDialog.Builder(this).setTitle("Pairing Wireless debugging")
                .setMessage("Buka Opsi pengembang → Wireless debugging → \"Pair device with pairing code\", "
                        + "lalu ketik kode 6 digit yang tampil. Biarkan layar kode itu terbuka sampai selesai.")
                .setView(input)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Pasangkan", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        final String code = input.getText().toString().trim();
                        if (code.length() < 6) {
                            toast("Kode harus 6 digit.");
                            return;
                        }
                        busy = true;
                        status.setText("Pairing…");
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                final int r = AdbManager.pairAndConnect(MainActivity.this, code);
                                ui.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        busy = false;
                                        adbConnected = r == AdbManager.CONNECTED;
                                        refreshChrome();
                                        toast(adbConnected ? "Pairing dan koneksi berhasil." : "Pairing gagal. Cek kode dan coba lagi.");
                                    }
                                });
                            }
                        }, "adb-pair").start();
                    }
                }).show();
    }

    // ---------- Profil ----------

    private void initProfile() {
        String saved = getSharedPreferences("bersih", MODE_PRIVATE).getString("profile", null);
        List<Profiles.Info> ranked = Profiles.ranked(this);
        Profiles.Info pick = null;
        if (saved != null) {
            for (Profiles.Info i : ranked) if (i.id.equals(saved)) pick = i;
        }
        if (pick == null && !ranked.isEmpty()) pick = ranked.get(0).score > 0 ? ranked.get(0) : null;
        if (pick == null) {
            for (Profiles.Info i : ranked) if (i.id.equals("zte-b866f")) pick = i;
        }
        if (pick != null) applyProfile(pick);
    }

    private void applyProfile(Profiles.Info info) {
        profileId = info.id;
        profileName = info.name;
        profilePkgs.clear();
        profilePkgs.addAll(Profiles.packages(this, info.id));
        getSharedPreferences("bersih", MODE_PRIVATE).edit().putString("profile", info.id).apply();
    }

    private void chooseProfile() {
        if (busy) return;
        final List<Profiles.Info> ranked = Profiles.ranked(this);
        int n = Math.min(ranked.size(), 15);
        String[] names = new String[n];
        for (int i = 0; i < n; i++) {
            Profiles.Info p = ranked.get(i);
            names[i] = (p.score > 0 ? "★ " : "") + p.name;
        }
        new AlertDialog.Builder(this).setTitle("Pilih profil (paling cocok di atas)")
                .setItems(names, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        applyProfile(ranked.get(which));
                        setFilter(F_PROFILE);
                    }
                }).setNegativeButton("Batal", null).show();
    }

    // ---------- Data ----------

    private void scan() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<AppItem> items = queryAll();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        all.clear();
                        all.addAll(items);
                        applyFilter();
                    }
                });
            }
        }, "scan").start();
    }

    private List<AppItem> queryAll() {
        PackageManager pm = getPackageManager();
        Set<String> homes = new HashSet<String>();
        Intent homeIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        for (ResolveInfo ri : pm.queryIntentActivities(homeIntent, 0)) {
            if (ri.activityInfo != null) homes.add(ri.activityInfo.packageName);
        }
        String ime = null;
        try {
            String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
            if (s != null && s.indexOf('/') > 0) ime = s.substring(0, s.indexOf('/'));
        } catch (Throwable ignored) {
        }

        List<AppItem> out = new ArrayList<AppItem>();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.MATCH_DISABLED_COMPONENTS);
        for (ApplicationInfo ai : apps) {
            String pkg = ai.packageName;
            boolean system = (ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0;
            boolean persistent = (ai.flags & ApplicationInfo.FLAG_PERSISTENT) != 0;
            boolean core = pkg.equals(getPackageName()) || homes.contains(pkg) || pkg.equals(ime)
                    || ai.uid == 1000 || persistent || Catalog.isProtectedName(pkg);
            int tier = core ? AppItem.TIER_PROTECTED
                    : (Catalog.isSuggested(pkg) ? AppItem.TIER_SUGGESTED : AppItem.TIER_OTHER);
            String label;
            try {
                CharSequence cs = ai.loadLabel(pm);
                label = cs != null ? cs.toString() : pkg;
            } catch (Throwable t) {
                label = pkg;
            }
            Drawable icon = null;
            try {
                icon = ai.loadIcon(pm);
            } catch (Throwable ignored) {
            }
            out.add(new AppItem(pkg, label, icon, system, ai.enabled, tier));
        }
        Collections.sort(out, new Comparator<AppItem>() {
            @Override
            public int compare(AppItem a, AppItem b) {
                int ra = rank(a.tier);
                int rb = rank(b.tier);
                if (ra != rb) return ra < rb ? -1 : 1;
                if (a.system != b.system) return a.system ? -1 : 1;
                return a.label.compareToIgnoreCase(b.label);
            }
        });
        return out;
    }

    /** Urutan tampil: disarankan, lainnya, dilindungi. */
    private static int rank(int tier) {
        if (tier == AppItem.TIER_SUGGESTED) return 0;
        if (tier == AppItem.TIER_OTHER) return 1;
        return 2;
    }

    private void setFilter(int f) {
        filter = f;
        for (AppItem a : all) a.selected = false;
        applyFilter();
    }

    private void applyFilter() {
        shown.clear();
        for (AppItem a : all) {
            boolean keep;
            switch (filter) {
                case F_SUGGESTED:
                    keep = a.enabled && a.tier == AppItem.TIER_SUGGESTED;
                    break;
                case F_ALL:
                    keep = a.enabled && a.tier != AppItem.TIER_PROTECTED;
                    break;
                case F_DISABLED:
                    keep = !a.enabled;
                    break;
                case F_PROFILE:
                    keep = a.enabled && a.tier != AppItem.TIER_PROTECTED && profilePkgs.contains(a.pkg);
                    break;
                default:
                    keep = a.enabled && a.tier == AppItem.TIER_PROTECTED;
                    break;
            }
            if (keep) shown.add(a);
        }
        adapter.notifyDataSetChanged();
        refreshChrome();
    }

    private List<AppItem> selectedItems() {
        List<AppItem> r = new ArrayList<AppItem>();
        for (AppItem a : all) if (a.selected) r.add(a);
        return r;
    }

    private void refreshChrome() {
        String rootText = root == null ? "memeriksa root…"
                : (root ? "root aktif" : (adbConnected ? "ADB terhubung (tanpa root)" : "belum terhubung (tanpa root)"));
        int selected = selectedItems().size();
        status.setText("Status: " + rootText + "   •   " + shown.size() + " aplikasi   •   " + selected + " dipilih"
                + (filter == F_PROFILE ? "\nProfil: " + profileName : ""));
        status.setTextColor(root != null && !root && !adbConnected ? Color.parseColor("#FBBF24") : Color.parseColor("#94A3B8"));
        for (int i = 0; i < FILTER_NAMES.length; i++) {
            filterBtns[i].setTextColor(i == filter ? Color.parseColor("#38BDF8") : Color.WHITE);
        }
        runBtn.setText((filter == F_DISABLED ? "Aktifkan" : "Nonaktifkan") + " (" + selected + ")");
        runBtn.setAlpha(filter == F_PROTECTED || busy ? 0.4f : 1f);
    }

    // ---------- Actions ----------

    private void onRowClick(int position) {
        if (busy || position < 0 || position >= shown.size()) return;
        if (filter == F_PROTECTED) {
            toast("Aplikasi inti sistem, dilindungi demi keamanan.");
            return;
        }
        AppItem a = shown.get(position);
        a.selected = !a.selected;
        adapter.notifyDataSetChanged();
        refreshChrome();
    }

    private void toggleSelectAll() {
        if (filter == F_PROTECTED || busy) return;
        boolean anyUnselected = false;
        for (AppItem a : shown) if (!a.selected) anyUnselected = true;
        for (AppItem a : shown) a.selected = anyUnselected;
        adapter.notifyDataSetChanged();
        refreshChrome();
    }

    private void onRun() {
        if (busy || filter == F_PROTECTED) return;
        final List<AppItem> items = selectedItems();
        if (items.isEmpty()) {
            toast("Pilih aplikasi dulu (tekan OK pada barisnya).");
            return;
        }
        final boolean enable = filter == F_DISABLED;
        if (root == null) {
            toast("Sedang memeriksa root, coba lagi sebentar.");
            return;
        }
        if (!root && !adbConnected) {
            needConnection(items, enable);
            return;
        }
        confirm((enable ? "Aktifkan " : "Nonaktifkan ") + items.size() + " aplikasi?",
                enable ? "Aplikasi akan diaktifkan kembali."
                        : "Aplikasi hanya dinonaktifkan, tidak dihapus. Bisa dipulihkan kapan saja lewat filter \"Nonaktif\" atau \"Pulihkan semua\".",
                new Runnable() {
                    @Override
                    public void run() {
                        execute(items, enable);
                    }
                });
    }

    private void onRestoreAll() {
        if (busy) return;
        final List<AppItem> disabled = new ArrayList<AppItem>();
        for (AppItem a : all) if (!a.enabled) disabled.add(a);
        if (disabled.isEmpty()) {
            toast("Tidak ada aplikasi yang nonaktif.");
            return;
        }
        if (root == null) {
            toast("Sedang memeriksa root, coba lagi sebentar.");
            return;
        }
        if (!root && !adbConnected) {
            needConnection(disabled, true);
            return;
        }
        confirm("Pulihkan " + disabled.size() + " aplikasi?", "Semua aplikasi yang nonaktif akan diaktifkan lagi.",
                new Runnable() {
                    @Override
                    public void run() {
                        execute(disabled, true);
                    }
                });
    }

    private void needConnection(final List<AppItem> items, final boolean enable) {
        new AlertDialog.Builder(this).setTitle("Belum terhubung")
                .setMessage("Tanpa root, app perlu tersambung ke ADB STB. Sambungkan sekarang, atau ambil perintah adb untuk dijalankan dari PC.")
                .setNegativeButton("Batal", null)
                .setNeutralButton("Perintah adb", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        showAdbCommands(items, enable);
                    }
                })
                .setPositiveButton("Sambungkan ADB", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        connectAdb();
                    }
                }).show();
    }

    private static boolean validPackage(String p) {
        return p.matches("[A-Za-z0-9_.]+");
    }

    private void execute(final List<AppItem> items, final boolean enable) {
        busy = true;
        refreshChrome();
        new Thread(new Runnable() {
            @Override
            public void run() {
                writeBackup();
                final StringBuilder fails = new StringBuilder();
                int okCount = 0;
                int failCount = 0;
                int i = 0;
                for (AppItem a : items) {
                    i++;
                    final int n = i;
                    ui.post(new Runnable() {
                        @Override
                        public void run() {
                            status.setText("Memproses " + n + "/" + items.size() + "…");
                        }
                    });
                    if (!validPackage(a.pkg)) {
                        failCount++;
                        continue;
                    }
                    String cmd = enable ? "pm enable " + a.pkg : "pm disable-user --user 0 " + a.pkg;
                    RootShell.Result r = Runner.run(MainActivity.this, cmd, 20000);
                    boolean success = r.ok() && (r.out.contains("disabled") || r.out.contains("enabled"));
                    if (success) {
                        okCount++;
                    } else {
                        failCount++;
                        if (fails.length() < 600) {
                            fails.append(a.pkg).append(": ").append(r.out.length() > 80 ? r.out.substring(0, 80) : r.out).append('\n');
                        }
                    }
                }
                final int ok = okCount;
                final int bad = failCount;
                final String failText = fails.toString();
                ui.post(new Runnable() {
                    @Override
                    public void run() {
                        busy = false;
                        scan();
                        String msg = ok + " berhasil" + (bad > 0 ? ", " + bad + " gagal" : "") + ".";
                        if (bad > 0) msg += "\n\n" + failText;
                        if (!enable && ok > 0) msg += "\nJika ada yang tidak beres, pakai \"Pulihkan semua\".";
                        info(enable ? "Selesai mengaktifkan" : "Selesai menonaktifkan", msg);
                    }
                });
            }
        }, "execute").start();
    }

    /** Simpan daftar paket yang sedang nonaktif sebelum mengubah apa pun. */
    private void writeBackup() {
        try {
            File f = new File(getFilesDir(), "backup-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".txt");
            FileWriter w = new FileWriter(f);
            for (AppItem a : all) if (!a.enabled) w.write(a.pkg + "\n");
            w.close();
        } catch (IOException ignored) {
        }
    }

    private void showAdbCommands(List<AppItem> items, boolean enable) {
        StringBuilder sb = new StringBuilder();
        for (AppItem a : items) {
            if (!validPackage(a.pkg)) continue;
            sb.append("adb shell ").append(enable ? "pm enable " : "pm disable-user --user 0 ").append(a.pkg).append('\n');
        }
        String path = "";
        try {
            File dir = getExternalFilesDir(null);
            if (dir != null) {
                File f = new File(dir, enable ? "perintah-pulihkan.txt" : "perintah-nonaktifkan.txt");
                FileWriter w = new FileWriter(f);
                w.write(sb.toString());
                w.close();
                path = "\n\nDisimpan di:\n" + f.getAbsolutePath();
            }
        } catch (IOException ignored) {
        }
        String[] lines = sb.toString().split("\n");
        StringBuilder preview = new StringBuilder();
        for (int i = 0; i < lines.length && i < 8; i++) preview.append(lines[i]).append('\n');
        if (lines.length > 8) preview.append("… dan ").append(lines.length - 8).append(" lainnya");
        info("Perintah adb",
                "Jalankan perintah ini dari PC lewat adb:\n\n" + preview + path);
    }

    // ---------- Dialog helpers ----------

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();
    }

    private void info(String title, String msg) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setPositiveButton("OK", null).show();
    }

    private void confirm(String title, String msg, final Runnable yes) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(msg)
                .setNegativeButton("Batal", null)
                .setPositiveButton("Lanjut", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        yes.run();
                    }
                }).show();
    }

    // ---------- List adapter ----------

    private final class AppAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return shown.size();
        }

        @Override
        public Object getItem(int position) {
            return shown.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LinearLayout row;
            View box;
            ImageView icon;
            TextView name;
            TextView pkg;
            TextView badge;
            if (convertView == null) {
                row = new LinearLayout(MainActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(12), dp(6), dp(12), dp(6));
                row.setLayoutParams(new AbsListView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, dp(56)));

                box = new View(MainActivity.this);
                row.addView(box, new LinearLayout.LayoutParams(dp(22), dp(22)));

                icon = new ImageView(MainActivity.this);
                LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(dp(34), dp(34));
                ilp.leftMargin = dp(14);
                row.addView(icon, ilp);

                LinearLayout texts = new LinearLayout(MainActivity.this);
                texts.setOrientation(LinearLayout.VERTICAL);
                name = new TextView(MainActivity.this);
                name.setTextColor(Color.WHITE);
                name.setTextSize(16);
                name.setSingleLine(true);
                pkg = new TextView(MainActivity.this);
                pkg.setTextColor(Color.parseColor("#94A3B8"));
                pkg.setTextSize(12);
                pkg.setSingleLine(true);
                texts.addView(name);
                texts.addView(pkg);
                LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
                tlp.leftMargin = dp(14);
                row.addView(texts, tlp);

                badge = new TextView(MainActivity.this);
                badge.setTextSize(12);
                row.addView(badge, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

                row.setTag(new View[]{box, icon, name, pkg, badge});
            } else {
                row = (LinearLayout) convertView;
                View[] p = (View[]) row.getTag();
                box = p[0];
                icon = (ImageView) p[1];
                name = (TextView) p[2];
                pkg = (TextView) p[3];
                badge = (TextView) p[4];
            }
            AppItem a = shown.get(position);
            GradientDrawable g = new GradientDrawable();
            g.setCornerRadius(dp(5));
            g.setStroke(dp(2), Color.parseColor("#64748B"));
            g.setColor(a.selected ? Color.parseColor("#38BDF8") : Color.TRANSPARENT);
            box.setBackground(g);
            box.setVisibility(filter == F_PROTECTED ? View.INVISIBLE : View.VISIBLE);
            icon.setImageDrawable(a.icon);
            name.setText(a.label);
            pkg.setText(a.pkg);
            if (!a.enabled) {
                badge.setText("Nonaktif");
                badge.setTextColor(Color.parseColor("#F87171"));
            } else if (a.tier == AppItem.TIER_SUGGESTED) {
                badge.setText("Disarankan");
                badge.setTextColor(Color.parseColor("#4ADE80"));
            } else if (a.tier == AppItem.TIER_PROTECTED) {
                badge.setText("Dilindungi");
                badge.setTextColor(Color.parseColor("#FBBF24"));
            } else {
                badge.setText(a.system ? "Sistem" : "Aplikasi");
                badge.setTextColor(Color.parseColor("#94A3B8"));
            }
            return row;
        }
    }
}
