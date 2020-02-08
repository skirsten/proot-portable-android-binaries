TERMUX_PKG_HOMEPAGE=https://proot-me.github.io/
TERMUX_PKG_DESCRIPTION="Emulate chroot, bind mount and binfmt_misc for non-root users"
TERMUX_PKG_LICENSE="GPL-2.0"
# Just bump commit and version when needed:
_COMMIT=1f4ec1c9d3fcc5d44c2a252eda6d09b0c24928cd
TERMUX_PKG_VERSION=5.1.107
TERMUX_PKG_REVISION=25
TERMUX_PKG_SRCURL=https://github.com/termux/proot/archive/${_COMMIT}.zip
TERMUX_PKG_SHA256=1119f1d27ca7a655eb627ad227fbd9c7a0343ea988dad3ed620fd6cd98723c20
TERMUX_PKG_DEPENDS="libtalloc-static"

# Install loader in libexec instead of extracting it every time
# export PROOT_UNBUNDLE_LOADER=$TERMUX_PREFIX/libexec/proot

termux_step_pre_configure() {
	CPPFLAGS+=" -DARG_MAX=131072"
  LDFLAGS+=" -static"
}

termux_step_make_install() {
	cd $TERMUX_PKG_SRCDIR/src

  sed -i 's/P_tmpdir/"\/tmp"/g' path/temp.c

	make V=1
	make install

  $STRIP proot
	cp proot /home/builder/termux-packages
}