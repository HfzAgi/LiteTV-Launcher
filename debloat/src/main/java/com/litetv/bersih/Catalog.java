package com.litetv.bersih;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Daftar paket: yang dilindungi (tidak boleh disentuh) dan yang aman untuk dinonaktifkan. */
final class Catalog {
    private Catalog() {}

    /** Paket inti. Nama persis atau diawali "nama." dianggap dilindungi. */
    private static final String[] PROTECTED = {
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.tv.settings",
            "com.android.providers",
            "com.android.inputmethod",
            "com.android.bluetooth",
            "com.android.wifi",
            "com.android.networkstack",
            "com.android.permissioncontroller",
            "com.android.packageinstaller",
            "com.android.shell",
            "com.android.phone",
            "com.android.server",
            "com.android.vending",
            "com.android.webview",
            "com.android.certinstaller",
            "com.android.keychain",
            "com.android.location",
            "com.android.nfc",
            "com.android.captiveportallogin",
            "com.android.cellbroadcast",
            "com.android.externalstorage",
            "com.android.documentsui",
            "com.android.media",
            "com.android.mtp",
            "com.android.se",
            "com.android.ext",
            "com.android.managedprovisioning",
            "com.android.vpndialogs",
            "com.android.carrierconfig",
            "com.android.emergency",
            "com.google.android.gms",
            "com.google.android.gsf",
            "com.google.android.webview",
            "com.google.android.packageinstaller",
            "com.google.android.permissioncontroller",
            "com.google.android.networkstack",
            "com.google.android.ext",
            "com.google.android.modulemetadata",
            "com.google.android.setupwizard",
            "com.google.android.tungsten.setupwraith",
            "com.google.android.tvlauncher",
            "com.google.android.leanbacklauncher",
    };

    /** Aman dinonaktifkan di hampir semua STB/TV; semuanya bisa dipulihkan. */
    private static final Set<String> SUGGESTED = new HashSet<String>(Arrays.asList(
            "com.android.bips",
            "com.android.printspooler",
            "com.android.egg",
            "com.android.bluetoothmidiservice",
            "com.android.dreams.basic",
            "com.android.dreams.phototable",
            "com.android.wallpaperbackup",
            "com.android.traceur",
            "com.android.dynsystem",
            "com.google.android.feedback",
            "com.google.android.videos",
            "com.google.android.music",
            "com.google.android.play.games"
    ));

    static boolean isProtectedName(String pkg) {
        for (String p : PROTECTED) {
            if (pkg.equals(p) || pkg.startsWith(p + ".")) return true;
        }
        return false;
    }

    static boolean isSuggested(String pkg) {
        return SUGGESTED.contains(pkg);
    }
}
