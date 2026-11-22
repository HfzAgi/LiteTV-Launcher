package com.litetv.bersih;

import android.app.Application;

import io.github.muntashirakon.adb.PRNGFixes;

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        PRNGFixes.apply();
    }
}
