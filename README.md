# Portable PRoot Android Binaries

![CI](https://github.com/skirsten/proot-portable-android-binaries/workflows/CI/badge.svg)

- [`https://skirsten.github.io/proot-portable-android-binaries/x86_64/proot`](https://skirsten.github.io/proot-portable-android-binaries/x86_64/proot)
- [`https://skirsten.github.io/proot-portable-android-binaries/x86/proot`](https://skirsten.github.io/proot-portable-android-binaries/x86/proot)
- [`https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot`](https://skirsten.github.io/proot-portable-android-binaries/aarch64/proot)
- [`https://skirsten.github.io/proot-portable-android-binaries/armv7/proot`](https://skirsten.github.io/proot-portable-android-binaries/armv7/proot)

Based on the [Termux proot package](https://wiki.termux.com/wiki/PRoot).
By default it uses `/tmp` as temp directory (which is not available on Android).
You need to define the `PROOT_TMP_DIR` environment variable.

## Examples

- [NativeExecutor.java](NativeExecutor.java)

  - Downloads proot for the correct arch from the links above.
  - Downloads and extracts the latest Alpine Linux rootfs (could be improved).
  - Sets name servers.
  - After that you can run any normal Alpine commands in the chroot.
  - Use like this:

    ```java
    NativeExecutor executor = new NativeExecutor(context);

    if (executor.needLoadProot()) {
        executor.loadProot();
    }

    if (executor.needExtractRootfs()) {
        if (executor.needLoadRootfs()) {
            executor.loadRootfs();
        }
        executor.extractRootfs();
    }

    if (executor.needSetNameServers()) {
        executor.setNameServers();
    }

    // Install some alpine packages...
    executor.exec("apk", "add", "youtube-dl", "ffmpeg");
    // Do other stuff
    ```
