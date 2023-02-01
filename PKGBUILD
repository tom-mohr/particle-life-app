# Maintainer: Your Name <youremail@domain.com>
pkgname="particle-life-app"
pkgrel=1
pkgver=1
pkgdesc="GUI for Particle Life, a particle system showing life-like behaviour"
arch=("x86_64")
url="https://github.com/tom-mohr/$pkgname"
license=('GPL')
depends=('java-environment')
makedepends=('git' 'java-environment>=11')
source=("git+$url.git")
md5sums=('SKIP')
pkgver() {
	cd "$srcdir/$pkgname"
    git describe --long | sed 's/\([^-]*-g\)/r\1/;s/-/./g'

}
build() {
	cd "$srcdir/$pkgname"
    chmod +x ./gradlew #make gradlew an executable
    ./gradlew shadowJar #shadowJar wraps jar with libraries
}
package() {
    cd "$pkgname-$pkgver"
    #copy jar executable to main java location
    #possible error in file name
    install -Dm755 "$srcdir/$pkgname/build/libs/$pkgname-1.0.0-all.jar" "$pkgdir/usr/share/java/${pkgname}/${pkgname}.jar"
    #copy and rename _patch to the package name
    install -Dm755 "$srcdir/$pkgname/_patch.sh" "$pkgdir/usr/bin/${pkgname}"
}