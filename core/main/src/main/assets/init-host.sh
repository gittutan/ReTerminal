UBUNTU_DIR=$PREFIX/local/ubuntu

# All sessions share this lock; closing the descriptors releases it even after a crash.
# Keep the lock file so every session locks the same inode.
(
    /system/bin/flock -x 9 || exit 1
    mkdir -p "$UBUNTU_DIR" || exit 1

    if [ ! -f "$UBUNTU_DIR/.rootfs-ready" ]; then
        # Preserve rootfs absolute symlinks and emulate hard links blocked by Android.
        "$PROOT" --link2symlink /system/bin/tar -xPzf "$PREFIX/files/ubuntu.tar.gz" -C "$UBUNTU_DIR" || exit 1
        touch "$UBUNTU_DIR/.rootfs-ready" || exit 1
    fi
    mkdir -p "$UBUNTU_DIR/root" || exit 1

    case "$APT_MIRROR" in
        tuna) MIRROR_URL=http://mirrors.tuna.tsinghua.edu.cn/ubuntu-ports/ ;;
        aliyun) MIRROR_URL=https://mirrors.aliyun.com/ubuntu-ports/ ;;
        tencent) MIRROR_URL=https://mirrors.cloud.tencent.com/ubuntu-ports/ ;;
        ubuntu) MIRROR_URL=http://ports.ubuntu.com/ubuntu-ports/ ;;
        *) MIRROR_URL=http://mirrors.ustc.edu.cn/ubuntu-ports/ ;;
    esac
    if ! sed -i \
        -e "s#https*://mirrors\.cloud\.tencent\.com/ubuntu-ports/#$MIRROR_URL#g" \
        -e "s#https*://mirrors\.tuna\.tsinghua\.edu\.cn/ubuntu-ports/#$MIRROR_URL#g" \
        -e "s#https*://mirrors\.ustc\.edu\.cn/ubuntu-ports/#$MIRROR_URL#g" \
        -e "s#https*://mirrors\.aliyun\.com/ubuntu-ports/#$MIRROR_URL#g" \
        -e "s#https*://ports\.ubuntu\.com/ubuntu-ports/#$MIRROR_URL#g" \
        -e "s#https*://mirrors\.cloud\.tencent\.com/ubuntu/#$MIRROR_URL#g" \
        -e "s#https*://archive\.ubuntu\.com/ubuntu/#$MIRROR_URL#g" \
        -e "s#https*://security\.ubuntu\.com/ubuntu/#$MIRROR_URL#g" \
        "$UBUNTU_DIR/etc/apt/sources.list"; then
        echo 'Unable to update Ubuntu sources; continuing with existing sources.' >&2
    fi
) 9>"$PREFIX/local/.ubuntu-rootfs.lock" || exit 1

ARGS="--kill-on-exit"
ARGS="$ARGS -w /"

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do

 if [ -e "$system_mnt" ]; then
  system_mnt=$(realpath "$system_mnt")
  ARGS="$ARGS -b ${system_mnt}"
 fi
done
unset system_mnt

ARGS="$ARGS -b /sdcard"
ARGS="$ARGS -b /storage"
ARGS="$ARGS -b /dev"
ARGS="$ARGS -b /data"
ARGS="$ARGS -b /dev/urandom:/dev/random"
ARGS="$ARGS -b /proc"
ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b $PREFIX/local/stat:/proc/stat"
ARGS="$ARGS -b $PREFIX/local/vmstat:/proc/vmstat"

if [ -e "/proc/self/fd" ]; then
  ARGS="$ARGS -b /proc/self/fd:/dev/fd"
fi

if [ -e "/proc/self/fd/0" ]; then
  ARGS="$ARGS -b /proc/self/fd/0:/dev/stdin"
fi

if [ -e "/proc/self/fd/1" ]; then
  ARGS="$ARGS -b /proc/self/fd/1:/dev/stdout"
fi

if [ -e "/proc/self/fd/2" ]; then
  ARGS="$ARGS -b /proc/self/fd/2:/dev/stderr"
fi


ARGS="$ARGS -b $PREFIX"
ARGS="$ARGS -b /sys"

if [ ! -d "$UBUNTU_DIR/tmp" ]; then
 mkdir -p "$UBUNTU_DIR/tmp"
 chmod 1777 "$UBUNTU_DIR/tmp"
fi
ARGS="$ARGS -b $UBUNTU_DIR/tmp:/dev/shm"

ARGS="$ARGS -r $UBUNTU_DIR"
ARGS="$ARGS -0"
ARGS="$ARGS --link2symlink"
ARGS="$ARGS --sysvipc"
ARGS="$ARGS -L"

(
    /system/bin/flock -x 9 || exit 1
    UBUNTU_PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:$PATH"
    CA_READY="$UBUNTU_DIR/.reterminal-ca-20260601-ready"
    CERT_BUNDLE="$UBUNTU_DIR/etc/ssl/certs/ca-certificates.crt"
    if [ -f "$CA_READY" ] && [ -s "$CERT_BUNDLE" ]; then
        exit 0
    fi
    if [ -s "$CERT_BUNDLE" ] &&
       [ "$(PATH="$UBUNTU_PATH" "$PROOT" $ARGS /usr/bin/dpkg-query -W -f='${Status}' ca-certificates 2>/dev/null)" = 'install ok installed' ]; then
        touch "$CA_READY"
    else
        OFFLINE_PACKAGES="$PREFIX/files/ca-certificates_20260601~22.04.1_all.deb"
        if [ "$(PATH="$UBUNTU_PATH" "$PROOT" $ARGS /usr/bin/dpkg-query -W -f='${Status}' openssl 2>/dev/null)" != 'install ok installed' ]; then
            OFFLINE_PACKAGES="$PREFIX/files/openssl_3.0.2-0ubuntu1.29_arm64.deb $OFFLINE_PACKAGES"
        fi
        if DEBIAN_FRONTEND=noninteractive PATH="$UBUNTU_PATH" "$PROOT" $ARGS /usr/bin/dpkg -i $OFFLINE_PACKAGES; then
            if [ -s "$CERT_BUNDLE" ]; then
                touch "$CA_READY"
            else
                echo "Certificate bundle missing or empty after dpkg -i: $CERT_BUNDLE" >&2
            fi
        fi
    fi
) 9>"$PREFIX/local/.ubuntu-rootfs.lock"

"$PROOT" $ARGS /bin/sh "$PREFIX/local/bin/init" "$@"
