# Maintainer: Tom Mohr <particle-life-app@gmail.com> 
# Maintainer: Altruios <altruios.phasma@gmail.com>
# Contributor: SuperRyn <*@*.*> 
pkgname="particle-life-app"
pkgrel=1
pkgver=1
pkgdesc="GUI for Particle Life, a particle system showing life-like behaviour"
arch=("x86_64")
url="https://github.com/tom-mohr/$pkgname"
license=('GPL')
depends=('java-environment')
makedepends=('git' 'java-runtime>=16')
source=("git+$url.git")
md5sums=('SKIP')
pkgver() {
  cd "$srcdir/$pkgname"
  git describe --tags | sed 's/\([^-]*-g\)/r\1/;s/-/./g'
}
build() {
  cd "$srcdir/$pkgname"
    chmod +x ./gradlew
    ./gradlew shadowJar
}
package() {
  cd "$srcdir/$pkgname"
    install -Dm755 "$srcdir/$pkgname/build/libs/$pkgname-1.0.0-all.jar" "$pkgdir/usr/share/java/${pkgname}/${pkgname}.jar"
    install -Dm755 "$srcdir/$pkgname/_patch.sh" "$pkgdir/usr/bin/${pkgname}"
    install -Dm755 "$srcdir/$pkgname/README.md" "$pkgdir/usr/share/doc/${pkgname}"
    install -Dm755 "$srcdir/$pkgname/LICENSE.md" "$pkgdir/usr/share/licenses/${pkgname}"
}