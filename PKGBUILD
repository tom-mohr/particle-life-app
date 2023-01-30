# Maintainer: Your Name <youremail@domain.com>
pkgname="particle-life"
pkgver=0.0.3
pkgrel=1
pkgdesc="GUI for Particle Life, a particle system showing life-like behaviour"
arch=("x86_64")
url="https://github.com/altruios/particle-life-app"
license=('GPL')
depends=('java-environment')
source=("$pkgname.tar.gz::https://github.com/altruios/$pkgname-app/archive/refs/tags/$pkgver.tar.gz")
md5sums=('SKIP')


build() {
	echo "$pkgname-app-$pkgver"
	cd "$pkgname-app-$pkgver"
    chmod +x ./gradlew
    ./gradlew shadowJar
}

#move stuff to places it should be in linux
package() {
	cd "$pkgname-app-$pkgver"
    install -Dm755 "/build/libs$pkgname-app-$pkgver-1.0.0-all.jar" "$pkgdir/usr/bin/$pkgname"
    install -Dm644 ./README.md "$pkgdir/usr/share/doc/$pkgname"
}
