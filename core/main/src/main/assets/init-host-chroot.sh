#!/bin/sh
SU="/system/bin/su"
UBUNTU_DIR=$PREFIX/local/ubuntu

mkdir -p "$UBUNTU_DIR" || exit 1

if [ ! -f "$UBUNTU_DIR/.rootfs-ready" ]; then
    tar -xzf "$PREFIX/files/ubuntu.tar.gz" -C "$UBUNTU_DIR" || exit 1
    # Ubuntu Base has no CA certificates; APT still verifies Ubuntu archive signatures.
    sed -i \
        -e 's|http://ports\.ubuntu\.com/ubuntu-ports/|http://mirrors.cloud.tencent.com/ubuntu-ports/|g' \
        -e 's|http://archive\.ubuntu\.com/ubuntu/|http://mirrors.cloud.tencent.com/ubuntu/|g' \
        -e 's|http://security\.ubuntu\.com/ubuntu/|http://mirrors.cloud.tencent.com/ubuntu/|g' \
        "$UBUNTU_DIR/etc/apt/sources.list" || exit 1
    touch "$UBUNTU_DIR/.rootfs-ready" || exit 1
fi

MOUNTS=""

mnt_bind() {
    src="$1"
    dst="$UBUNTU_DIR${2:-$1}"
    if [ -e "$src" ] && [ ! -e "$dst" ]; then
        mkdir -p "$(dirname "$dst")" 2>/dev/null
        if [ -d "$src" ]; then
            $SU -c "mkdir -p '$dst'"
        else
            $SU -c "touch '$dst'"
        fi
    fi
    if [ -e "$src" ]; then
        $SU -c "mount --bind '$src' '$dst'" 2>/dev/null
        MOUNTS="$MOUNTS $dst"
    fi
}

for system_mnt in /apex /odm /product /system /system_ext /vendor \
 /linkerconfig/ld.config.txt \
 /linkerconfig/com.android.art/ld.config.txt \
 /plat_property_contexts /property_contexts; do
    if [ -e "$system_mnt" ]; then
        system_mnt=$(realpath "$system_mnt")
        mnt_bind "$system_mnt"
    fi
done
unset system_mnt

mnt_bind /sdcard
mnt_bind /storage
mnt_bind /dev
mnt_bind /data
mnt_bind /proc
mnt_bind /sys
mnt_bind /dev/urandom /dev/random
mnt_bind $PREFIX

if [ -e "/proc/self/fd" ]; then mnt_bind /proc/self/fd /dev/fd; fi
if [ -e "/proc/self/fd/0" ]; then mnt_bind /proc/self/fd/0 /dev/stdin; fi
if [ -e "/proc/self/fd/1" ]; then mnt_bind /proc/self/fd/1 /dev/stdout; fi
if [ -e "/proc/self/fd/2" ]; then mnt_bind /proc/self/fd/2 /dev/stderr; fi

if [ ! -d "$UBUNTU_DIR/tmp" ]; then
    $SU -c "mkdir -p '$UBUNTU_DIR/tmp' && chmod 1777 '$UBUNTU_DIR/tmp'"
fi
mnt_bind "$UBUNTU_DIR/tmp" /dev/shm

if [ -e "$PREFIX/local/stat" ]; then
    $SU -c "cp '$PREFIX/local/stat' '$UBUNTU_DIR/proc/stat'" 2>/dev/null
fi
if [ -e "$PREFIX/local/vmstat" ]; then
    $SU -c "cp '$PREFIX/local/vmstat' '$UBUNTU_DIR/proc/vmstat'" 2>/dev/null
fi

cleanup() {
    for m in $MOUNTS; do
        $SU -c "umount -l '$m'" 2>/dev/null
    done
}
trap cleanup EXIT INT TERM

$SU -c "'$CHROOT' '$UBUNTU_DIR' /usr/bin/env -i HOME=/root PATH=/bin:/sbin:/usr/bin:/usr/sbin TERM=xterm-256color LANG=C.UTF-8 COLORTERM=truecolor USE_CHROOT=1 sh '$PREFIX/local/bin/init' $*"
cleanup
