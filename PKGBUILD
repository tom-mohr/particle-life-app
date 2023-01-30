# Maintainer: Your Name <youremail@domain.com>
pkgname="particle-life"
pkgrel=1
pkgver=1.1.3 # todo: make automatic
pkgdesc="GUI for Particle Life, a particle system showing life-like behaviour"
arch=("x86_64")
url="https://github.com/tom-mohr/particle-life-app"
license=('GPL')
depends=('java-environment')
makedepends=('git' 'java-environment>=11')
source=("$pkgname.tar.gz::$url/archive/refs/tags/$pkgver.tar.gz")
md5sums=('SKIP')
build() {
	cd "$pkgname-app-$pkgver"
    chmod +x ./gradlew
    ./gradlew shadowJar
}
package() {
    cd "$pkgname-app-$pkgver"
    install -Dm755 "$srcdir/$pkgname-app-$pkgver/build/libs/$pkgname-app-$pkgver-1.0.0-all.jar" "$pkgdir/usr/share/java/${pkgname}/${pkgname}.jar"
    install -Dm755 "$srcdir/$pkgname-app-$pkgver/$pkgname.sh" "$pkgdir/usr/bin/${pkgname}"
}