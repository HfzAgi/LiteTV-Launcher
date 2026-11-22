package com.litetv.bersih;

// Disesuaikan dari aplikasi contoh libadb-android (Muntashir Al-Islam), dipakai di bawah lisensi Apache-2.0.
// Lihat NOTICE.md dan licenses/libadb-android-Apache-2.0.txt.

import android.content.Context;
import android.os.Build;
import android.sun.misc.BASE64Encoder;
import android.sun.security.provider.X509Factory;
import android.sun.security.x509.AlgorithmId;
import android.sun.security.x509.CertificateAlgorithmId;
import android.sun.security.x509.CertificateExtensions;
import android.sun.security.x509.CertificateIssuerName;
import android.sun.security.x509.CertificateSerialNumber;
import android.sun.security.x509.CertificateSubjectName;
import android.sun.security.x509.CertificateValidity;
import android.sun.security.x509.CertificateVersion;
import android.sun.security.x509.CertificateX509Key;
import android.sun.security.x509.KeyIdentifier;
import android.sun.security.x509.PrivateKeyUsageExtension;
import android.sun.security.x509.SubjectKeyIdentifierExtension;
import android.sun.security.x509.X500Name;
import android.sun.security.x509.X509CertImpl;
import android.sun.security.x509.X509CertInfo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.muntashirakon.adb.AbsAdbConnectionManager;
import io.github.muntashirakon.adb.AdbPairingRequiredException;
import io.github.muntashirakon.adb.AdbStream;
import io.github.muntashirakon.adb.android.AdbMdns;
import io.github.muntashirakon.adb.android.AndroidUtils;

/** Koneksi ADB ke daemon adbd milik STB sendiri (tanpa root). */
public class AdbManager extends AbsAdbConnectionManager {
    static final int CONNECTED = 0;
    static final int PAIRING_REQUIRED = 1;
    static final int FAILED = 2;

    private static AdbManager INSTANCE;

    static synchronized AdbManager get(@NonNull Context context) throws Exception {
        if (INSTANCE == null) INSTANCE = new AdbManager(context.getApplicationContext());
        return INSTANCE;
    }

    private PrivateKey mPrivateKey;
    private Certificate mCertificate;

    private AdbManager(@NonNull Context context) throws Exception {
        setApi(Build.VERSION.SDK_INT);
        mPrivateKey = readPrivateKeyFromFile(context);
        mCertificate = readCertificateFromFile(context);
        if (mPrivateKey == null || mCertificate == null) {
            int keySize = 2048;
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(keySize, SecureRandom.getInstance("SHA1PRNG"));
            KeyPair generateKeyPair = keyPairGenerator.generateKeyPair();
            PublicKey publicKey = generateKeyPair.getPublic();
            mPrivateKey = generateKeyPair.getPrivate();
            String subject = "CN=BersihSTB";
            String algorithmName = "SHA512withRSA";
            long expiryDate = System.currentTimeMillis() + 365L * 86400000L;
            CertificateExtensions certificateExtensions = new CertificateExtensions();
            certificateExtensions.set("SubjectKeyIdentifier", new SubjectKeyIdentifierExtension(
                    new KeyIdentifier(publicKey).getIdentifier()));
            X500Name x500Name = new X500Name(subject);
            Date notBefore = new Date();
            Date notAfter = new Date(expiryDate);
            certificateExtensions.set("PrivateKeyUsage", new PrivateKeyUsageExtension(notBefore, notAfter));
            CertificateValidity certificateValidity = new CertificateValidity(notBefore, notAfter);
            X509CertInfo x509CertInfo = new X509CertInfo();
            x509CertInfo.set("version", new CertificateVersion(2));
            x509CertInfo.set("serialNumber", new CertificateSerialNumber(new Random().nextInt() & Integer.MAX_VALUE));
            x509CertInfo.set("algorithmID", new CertificateAlgorithmId(AlgorithmId.get(algorithmName)));
            x509CertInfo.set("subject", new CertificateSubjectName(x500Name));
            x509CertInfo.set("key", new CertificateX509Key(publicKey));
            x509CertInfo.set("validity", certificateValidity);
            x509CertInfo.set("issuer", new CertificateIssuerName(x500Name));
            x509CertInfo.set("extensions", certificateExtensions);
            X509CertImpl x509CertImpl = new X509CertImpl(x509CertInfo);
            x509CertImpl.sign(mPrivateKey, algorithmName);
            mCertificate = x509CertImpl;
            writePrivateKeyToFile(context, mPrivateKey);
            writeCertificateToFile(context, mCertificate);
        }
    }

    @NonNull
    @Override
    protected PrivateKey getPrivateKey() {
        return mPrivateKey;
    }

    @NonNull
    @Override
    protected Certificate getCertificate() {
        return mCertificate;
    }

    @NonNull
    @Override
    protected String getDeviceName() {
        return "BersihSTB";
    }

    // ---------- Alur sambung / pairing ----------

    /** Coba sambung otomatis: Wireless debugging (mDNS) dulu, lalu USB debugging lewat jaringan (port 5555). */
    static int connectSmart(Context context) {
        boolean pairing = false;
        try {
            AdbManager m = get(context);
            if (m.isConnected()) return CONNECTED;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                try {
                    if (m.autoConnect(context, 5000)) return CONNECTED;
                } catch (AdbPairingRequiredException e) {
                    pairing = true;
                } catch (Throwable ignored) {
                }
            }
            try {
                if (m.connect("127.0.0.1", 5555)) return CONNECTED;
            } catch (AdbPairingRequiredException e) {
                pairing = true;
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
        return pairing ? PAIRING_REQUIRED : FAILED;
    }

    /** Pairing dengan kode 6 digit dari layar "Pair device with pairing code", lalu sambung. */
    static int pairAndConnect(Context context, String code) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return FAILED;
        try {
            AdbManager m = get(context);
            final AtomicInteger port = new AtomicInteger(-1);
            final CountDownLatch latch = new CountDownLatch(1);
            AdbMdns mdns = new AdbMdns(context, AdbMdns.SERVICE_TYPE_TLS_PAIRING, (hostAddress, p) -> {
                port.set(p);
                latch.countDown();
            });
            mdns.start();
            try {
                if (!latch.await(60, TimeUnit.SECONDS)) return FAILED;
            } finally {
                mdns.stop();
            }
            if (!m.pair(AndroidUtils.getHostIpAddress(context), port.get(), code)) return FAILED;
            return connectSmart(context);
        } catch (Throwable t) {
            return FAILED;
        }
    }

    /** Jalankan satu perintah shell lewat ADB, mengembalikan keluarannya. */
    static RootShell.Result shell(Context context, String command, long timeoutMs) {
        final StringBuilder sb = new StringBuilder();
        final AdbStream[] holder = new AdbStream[1];
        try {
            final AdbManager m = get(context);
            if (!m.isConnected()) return new RootShell.Result(-1, "ADB belum terhubung");
            holder[0] = m.openStream("shell:" + command);
            Thread reader = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        java.io.BufferedReader r = new java.io.BufferedReader(
                                new java.io.InputStreamReader(holder[0].openInputStream(), StandardCharsets.UTF_8));
                        String line;
                        while ((line = r.readLine()) != null) {
                            synchronized (sb) {
                                sb.append(line).append('\n');
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }, "adb-reader");
            reader.start();
            reader.join(timeoutMs);
            boolean done = !reader.isAlive();
            try {
                holder[0].close();
            } catch (IOException ignored) {
            }
            synchronized (sb) {
                return new RootShell.Result(done ? 0 : -2, sb.toString().trim());
            }
        } catch (Throwable t) {
            return new RootShell.Result(-1, String.valueOf(t.getMessage()));
        }
    }

    // ---------- Simpan kunci ----------

    @Nullable
    private static Certificate readCertificateFromFile(@NonNull Context context)
            throws IOException, CertificateException {
        File certFile = new File(context.getFilesDir(), "cert.pem");
        if (!certFile.exists()) return null;
        try (InputStream cert = new FileInputStream(certFile)) {
            return CertificateFactory.getInstance("X.509").generateCertificate(cert);
        }
    }

    private static void writeCertificateToFile(@NonNull Context context, @NonNull Certificate certificate)
            throws CertificateEncodingException, IOException {
        File certFile = new File(context.getFilesDir(), "cert.pem");
        BASE64Encoder encoder = new BASE64Encoder();
        try (OutputStream os = new FileOutputStream(certFile)) {
            os.write(X509Factory.BEGIN_CERT.getBytes(StandardCharsets.UTF_8));
            os.write('\n');
            encoder.encode(certificate.getEncoded(), os);
            os.write('\n');
            os.write(X509Factory.END_CERT.getBytes(StandardCharsets.UTF_8));
        }
    }

    @Nullable
    private static PrivateKey readPrivateKeyFromFile(@NonNull Context context)
            throws IOException, NoSuchAlgorithmException, InvalidKeySpecException {
        File privateKeyFile = new File(context.getFilesDir(), "private.key");
        if (!privateKeyFile.exists()) return null;
        byte[] privKeyBytes = new byte[(int) privateKeyFile.length()];
        try (InputStream is = new FileInputStream(privateKeyFile)) {
            int off = 0;
            while (off < privKeyBytes.length) {
                int n = is.read(privKeyBytes, off, privKeyBytes.length - off);
                if (n < 0) break;
                off += n;
            }
        }
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privKeyBytes);
        return keyFactory.generatePrivate(privateKeySpec);
    }

    private static void writePrivateKeyToFile(@NonNull Context context, @NonNull PrivateKey privateKey)
            throws IOException {
        File privateKeyFile = new File(context.getFilesDir(), "private.key");
        try (OutputStream os = new FileOutputStream(privateKeyFile)) {
            os.write(privateKey.getEncoded());
        }
    }
}
