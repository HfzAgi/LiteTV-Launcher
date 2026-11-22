package com.litetv.bersih;

import android.graphics.drawable.Drawable;

final class AppItem {
    static final int TIER_PROTECTED = 0;
    static final int TIER_SUGGESTED = 1;
    static final int TIER_OTHER = 2;

    final String pkg;
    final String label;
    final Drawable icon;
    final boolean system;
    final boolean enabled;
    final int tier;
    boolean selected;

    AppItem(String pkg, String label, Drawable icon, boolean system, boolean enabled, int tier) {
        this.pkg = pkg;
        this.label = label;
        this.icon = icon;
        this.system = system;
        this.enabled = enabled;
        this.tier = tier;
    }
}
