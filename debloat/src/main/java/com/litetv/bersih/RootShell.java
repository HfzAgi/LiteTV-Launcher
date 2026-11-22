package com.litetv.bersih;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/** Menjalankan perintah lewat su (root) dengan batas waktu. */
final class RootShell {
    private RootShell() {}

    static final class Result {
        final int code;
        final String out;
        Result(int code, String out) {
            this.code = code;
            this.out = out;
        }
        boolean ok() {
            return code == 0;
        }
    }

    static Result run(String command, long timeoutMs) {
        final Process p;
        try {
            p = new ProcessBuilder("su", "-c", command).redirectErrorStream(true).start();
        } catch (IOException e) {
            return new Result(-1, "su tidak ditemukan");
        }
        final StringBuilder sb = new StringBuilder();
        Thread reader = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
                    String line;
                    while ((line = r.readLine()) != null) {
                        synchronized (sb) {
                            sb.append(line).append('\n');
                        }
                    }
                } catch (IOException ignored) {
                }
            }
        }, "su-reader");
        reader.start();

        long end = System.currentTimeMillis() + timeoutMs;
        Integer code = null;
        while (System.currentTimeMillis() < end) {
            try {
                code = p.exitValue();
                break;
            } catch (IllegalThreadStateException stillRunning) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ie) {
                    break;
                }
            }
        }
        if (code == null) {
            p.destroy();
            return new Result(-2, "waktu habis");
        }
        try {
            reader.join(500);
        } catch (InterruptedException ignored) {
        }
        synchronized (sb) {
            return new Result(code, sb.toString().trim());
        }
    }

    static boolean isRootAvailable() {
        Result r = run("id", 8000);
        return r.ok() && r.out.contains("uid=0");
    }
}
