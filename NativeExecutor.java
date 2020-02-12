package com.example;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.lang3.ArrayUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

class NativeExecutor {
    private final String TAG = "NativeExecutor";

    private final File cacheDir;
    private final File filesDir;
    private final String arch;

    private final File prootFile;
    private final File rootfsTarGzFile;
    private final File rootfsDir;
    private final File resolvFile;

    NativeExecutor(Context context) throws Exception {
        filesDir = context.getFilesDir();
        cacheDir = context.getCacheDir();
        arch = getArch();

        prootFile = new File(filesDir, "proot");
        rootfsTarGzFile = new File(cacheDir, "rootfs.tar.gz");
        rootfsDir = new File(filesDir, "rootfs");
        resolvFile = new File(filesDir, "rootfs/etc/resolv.conf");
    }

    private String getArch() throws Exception {
        Map<String, String> abis = new HashMap<String, String>() {
            {
                put("armeabi-v7a", "armv7");
                put("arm64-v8a", "aarch64");
                put("x86", "x86");
                put("x86_64", "x86_64");
            }
        };

        for (String supportedAbi : Build.SUPPORTED_ABIS) {
            if (abis.containsKey(supportedAbi)) {
                return abis.get(supportedAbi);
            }
        }

        throw new Exception("No supported ABI found");
    }

    boolean needLoadProot() {
        return !prootFile.exists();
    }

    void loadProot() throws IOException {
        URL downloadUrl = new URL("https://skirsten.github.io/proot-portable-android-binaries/" + arch + "/proot");

        Log.d(TAG, "Loading proot to " + prootFile.getPath());

        try (InputStream in = downloadUrl.openStream();
                FileOutputStream fileOutputStream = new FileOutputStream(prootFile)) {
            IOUtils.copy(in, fileOutputStream);
        }

        prootFile.setExecutable(true);
    }

    boolean needLoadRootfs() {
        return !rootfsTarGzFile.exists();
    }

    void loadRootfs() throws IOException {
        String baseUrl = "https://alpine.global.ssl.fastly.net/alpine/edge/releases/" + arch + "/";
        URL downloadUrl = null;

        try (InputStream in = new URL(baseUrl + "latest-releases.yaml").openStream();
                InputStreamReader isr = new InputStreamReader(in);
                BufferedReader br = new BufferedReader(isr)) {
            for (String line; (line = br.readLine()) != null;) {
                if (line.startsWith("  file: alpine-minirootfs-")) {
                    downloadUrl = new URL(baseUrl + line.substring(8));
                    break;
                }
            }
        }

        if (downloadUrl == null) {
            throw new IOException("downloadUrl not found in latest-releases.yml");
        }

        Log.d(TAG, "Loading rootfs to " + rootfsTarGzFile.getPath());

        try (InputStream in = downloadUrl.openStream();
                FileOutputStream fileOutputStream = new FileOutputStream(rootfsTarGzFile)) {
            IOUtils.copy(in, fileOutputStream);
        }
    }

    boolean needExtractRootfs() {
        return !rootfsDir.exists();
    }

    void extractRootfs() throws IOException, InterruptedException {
        File tarFile = new File(cacheDir, "rootfs.tar");

        rootfsDir.mkdir();

        Log.d(TAG, "Extracting rootfs to " + rootfsDir.getPath());

        try (FileInputStream fileInputStream = new FileInputStream(rootfsTarGzFile);
                GZIPInputStream gzipInputStream = new GZIPInputStream(fileInputStream);
                FileOutputStream tarOutputStream = new FileOutputStream(tarFile)) {

            IOUtils.copy(gzipInputStream, tarOutputStream);
        }

        // tar should be available (TODO: investigate)
        Process proc = Runtime.getRuntime()
                .exec(new String[] { "tar", "-xf", tarFile.getPath(), "-C", rootfsDir.getPath() });

        proc.waitFor();

        rootfsTarGzFile.delete();
        tarFile.delete();
    }

    boolean needSetNameServers() {
        return !resolvFile.exists();
    }

    void setNameServers() throws IOException {
        Log.d(TAG, "Setting nameservers " + resolvFile.getPath());

        try (PrintWriter out = new PrintWriter(resolvFile, "UTF-8")) {
            out.println("nameserver 1.1.1.1");
            out.println("nameserver 1.0.0.1");
        }
    }

    private String[] execParams(String... additional) {
        return ArrayUtils.addAll(
                ArrayUtils.addAll(new String[] { new File(filesDir, "proot").getPath(), "--link2symlink", "-0", "-r",
                        new File(filesDir, "rootfs/").getPath() }, additional),
                "-b", "/dev/", "-b", "/sys/", "-b", "/proc/", "-w", "/root", "/usr/bin/env", "HOME=/root",
                "PATH=/bin:/usr/bin:/sbin:/usr/sbin");
    }

    private String[] execEnvp() {
        return new String[] { "PROOT_TMP_DIR=" + cacheDir.getPath() };
    }

    int exec(String... cmd) throws IOException, InterruptedException {
        Process proc = Runtime.getRuntime().exec(ArrayUtils.addAll(execParams(), cmd), execEnvp());

        DebugGobbler errorGobbler = new DebugGobbler(TAG, proc.getErrorStream());
        DebugGobbler outputGobbler = new DebugGobbler(TAG, proc.getInputStream());
        errorGobbler.start();
        outputGobbler.start();

        proc.waitFor();

        return proc.exitValue();
    }
}
