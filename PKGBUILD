# Maintainer: Your Name <youremail@domain.com>
pkgname="particle-life"
pkgver=1.1.2
pkgrel=1
pkgdesc="GUI for Particle Life, a particle system showing life-like behaviour"
arch=("x86_64")
url="https://github.com/tom-mohr/particle-life-app"
license=('GPL')
depends=('java-environment')
makedepends=('git')
source=("$pkgname.tar.gz::https://github.com/altruios/particle-life-app/archive/refs/tags/v1.1.2.tar.gz")
md5sums=('SKIP')


build() {
	echo "$pkgname-app-$pkgver"
	cd "$pkgname-app-$pkgver"
    chmod +x ./gradlew
    ./gradlew shadowJar
    java -jar build/libs/particle-life-app-1.0.0-all.jar
}

#move stuff to places it should be in linux
package() {
	cd "$pkgname-app"
    install -Dm755 "./$pkgname" "$pkgdir/usr/bin/$pkgname"
    install -Dm644 ./README.md "$pkgdir/usr/share/doc/$pkgname"
}
