# Neon Rush 🚀

**Hyper-casual addictive endless runner** listo para publicar en Google Play Store.

Juego móvil Android (Kotlin) con monetización completa:
- **Google AdMob** (banners, interstitials y rewarded videos)
- **Google Play Billing** (paquetes de recursos + suscripciones)

---

## 🎮 Concepto del juego

**Neon Rush** es un endless runner de un solo toque (hyper-casual):

- El personaje corre automáticamente hacia la derecha
- **Toca la pantalla** para saltar
- Evita obstáculos neón
- Recoge monedas y power-ups (imán, escudo, x2 score)
- La velocidad aumenta progresivamente → adictivo
- Sistema de puntuación + monedas (soft currency)
- Tienda con skins, boosts y compras reales
- Revive con video recompensado
- Anuncios intersticiales después de cada partida (configurable)

Diseño visual: estilo **cyberpunk / neon** (fondos oscuros + colores cyan/magenta/rosa).

---

## 📁 Estructura del proyecto

```
neon-rush-android/
├── app/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/maycode/neonrush/
│       │   ├── MainActivity.kt          # Menú principal + tienda
│       │   ├── GameActivity.kt          # Pantalla de juego
│       │   ├── game/
│       │   │   ├── GameView.kt          # Motor del juego (Canvas + loop)
│       │   │   ├── Player.kt
│       │   │   ├── Obstacle.kt
│       │   │   └── Coin.kt
│       │   ├── ads/AdManager.kt         # AdMob (banner, interstitial, rewarded)
│       │   └── billing/BillingManager.kt # Play Billing (IAP + suscripciones)
│       └── res/
│           ├── layout/
│           ├── values/
│           └── drawable/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

---

## 🛠️ Cómo abrir y compilar (Android Studio)

1. Clona el repositorio:
   ```bash
   git clone https://github.com/May-code5/neon-rush-android.git
   ```
2. Abre Android Studio → **Open** → selecciona la carpeta del proyecto
3. Espera a que Gradle sincronice (puede pedir aceptar licencias de SDK)
4. Crea un emulador o conecta un dispositivo físico
5. Pulsa **Run** ▶️

> **Requisito mínimo**: Android Studio Hedgehog o superior + JDK 17+

---

## 💰 Configuración de monetización (obligatorio antes de publicar)

### 1. Google AdMob

1. Ve a [https://admob.google.com](https://admob.google.com) y crea una cuenta
2. Crea una **App** → Android → nombre "Neon Rush"
3. Crea 3 unidades de anuncio:
   - Banner
   - Interstitial
   - Rewarded
4. Copia los **Ad Unit IDs**
5. En el código reemplaza los IDs de prueba en `AdManager.kt` y el **App ID** en `AndroidManifest.xml`

```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-XXXXXXXXXXXXXXXX~YYYYYYYYYY"/>
```

### 2. Google Play Billing (In-App Purchases)

1. En Google Play Console crea la app
2. Ve a **Monetización → Productos** y crea:
   - Productos consumibles: `coins_small`, `coins_medium`, `coins_large`, `gems_pack`
   - Suscripción: `remove_ads_monthly` y/o `premium_pass`
3. En `BillingManager.kt` usa exactamente los mismos product IDs
4. Prueba con cuentas de licencia de prueba

---

## 📦 Productos recomendados (IAP)

| Product ID              | Tipo          | Descripción                     |
|-------------------------|---------------|---------------------------------|
| `coins_1000`            | Consumible    | 1.000 monedas                   |
| `coins_5000`            | Consumible    | 5.000 monedas + bonus           |
| `gems_100`              | Consumible    | 100 gemas                       |
| `remove_ads`            | No consumible | Eliminar anuncios para siempre  |
| `premium_monthly`       | Suscripción   | Premium (sin ads + monedas diarias) |

---

## 🚀 Pasos para publicar en Play Store

1. **Firma el APK/AAB**
   - Android Studio → Build → Generate Signed Bundle / APK
   - Crea un keystore (guárdalo bien)

2. **Crea la app en Play Console**
   - https://play.google.com/console
   - Completa ficha de la tienda (título, descripción corta/larga, capturas, icono 512x512)

3. **Política de privacidad** (obligatorio)
   - Necesitas una URL pública (puedes usar GitHub Pages o un sitio gratis)
   - Debe mencionar AdMob + Play Billing + datos recopilados

4. **Formulario de seguridad de datos** (Data safety)

5. **Clasificación de contenido** (cuestionario)

6. **Sube el AAB** en producción o prueba interna primero

7. Espera la revisión (normalmente 1-7 días)

---

## 🎨 Personalización rápida

- Colores neón → `res/values/colors.xml`
- Velocidad inicial y dificultad → constantes en `GameView.kt`
- Frecuencia de anuncios → `AdManager.kt`
- Textos → `res/values/strings.xml`

---

## ⚠️ Importante antes de publicar

- [ ] Reemplazar todos los IDs de prueba de AdMob por los reales
- [ ] Configurar productos de Billing en Play Console
- [ ] Añadir icono de la app (ic_launcher)
- [ ] Crear capturas de pantalla (mínimo 2)
- [ ] Política de privacidad
- [ ] Probar en dispositivo real (no solo emulador)
- [ ] Cumplir políticas de Google Play (especialmente de anuncios a niños si aplica)

---

## 📞 Soporte

Este proyecto fue generado listo para que lo completes y publiques.
Si necesitas más features (leaderboards con Play Games, más power-ups, niveles, etc.) dime y lo ampliamos.

¡Éxito con el lanzamiento! 🚀

---

Hecho con ❤️ para May-code5
