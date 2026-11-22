package com.litetv.bersih;

import android.content.Context;

/** Menjalankan perintah shell lewat root jika ada, kalau tidak lewat ADB yang sudah tersambung. */
final class Runner {
    private Runner() {}

    static volatile boolean root = false;

    static RootShell.Result run(Context c, String cmd, long timeoutMs) {
        if (root) return RootShell.run(cmd, timeoutMs);
        return AdbManager.shell(c, cmd, timeoutMs);
    }
}
