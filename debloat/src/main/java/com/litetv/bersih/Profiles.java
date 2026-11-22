package com.litetv.bersih;

import android.content.Context;
import android.os.Build;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Profil debloat per model, dibaca dari assets/profiles (file teks, satu paket per baris). */
final class Profiles {
    private Profiles() {}

    static final class Info {
        final String id;
        final String name;
        final String os;
        int score;
        Info(String id, String name, String os) {
            this.id = id;
            this.name = name;
            this.os = os;
        }
    }

    /** Semua profil, diurutkan dari yang paling cocok dengan perangkat ini. */
    static List<Info> ranked(Context c) {
        List<Info> list = new ArrayList<Info>();
        try {
            InputStream in = c.getAssets().open("profiles/index.tsv");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                String[] p = line.split("\t");
                if (p.length >= 2) list.add(new Info(p[0], p[1], p.length > 2 ? p[2] : ""));
            }
            r.close();
        } catch (IOException ignored) {
        }
        final List<String> strong = tokens(Build.MODEL, Build.DEVICE, Build.PRODUCT);
        final List<String> weak = tokens(Build.MANUFACTURER, Build.BRAND);
        for (Info i : list) {
            String hay = (i.id + " " + i.name).toLowerCase(Locale.US);
            for (String t : strong) if (hay.contains(t)) i.score += 3;
            for (String t : weak) if (hay.contains(t)) i.score += 1;
        }
        Collections.sort(list, new Comparator<Info>() {
            @Override
            public int compare(Info a, Info b) {
                if (a.score != b.score) return a.score > b.score ? -1 : 1;
                return a.name.compareToIgnoreCase(b.name);
            }
        });
        return list;
    }

    private static List<String> tokens(String... values) {
        Set<String> out = new HashSet<String>();
        for (String v : values) {
            if (v == null) continue;
            for (String t : v.toLowerCase(Locale.US).split("[^a-z0-9]+")) {
                if (t.length() >= 3 && !t.equals("android") && !t.equals("tv")) out.add(t);
            }
        }
        return new ArrayList<String>(out);
    }

    /** Daftar nama paket dari satu profil; baris kosong dan komentar (#) dilewati. */
    static Set<String> packages(Context c, String id) {
        Set<String> out = new HashSet<String>();
        try {
            InputStream in = c.getAssets().open("profiles/" + id + ".txt");
            BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String line;
            while ((line = r.readLine()) != null) {
                int hash = line.indexOf('#');
                if (hash >= 0) line = line.substring(0, hash);
                line = line.trim();
                if (line.matches("[A-Za-z0-9_.]+")) out.add(line);
            }
            r.close();
        } catch (IOException ignored) {
        }
        return out;
    }
}
