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

"$PROOT" $ARGS /bin/sh "$PREFIX/local/bin/init" "$@"
