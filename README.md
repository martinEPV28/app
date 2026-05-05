# CampoLibreFutbol

Aplicacion Android para gestion de usuarios y reservas de canchas de futbol.
El proyecto prioriza el funcionamiento local con SQLite, y permite sincronizacion opcional con Firebase.

## Caracteristicas principales

- Autenticacion local de usuarios (registro, login y cambio de clave).
- Politica de contrasena y control de intentos fallidos.
- Reserva de canchas por fecha y franja horaria.
- Prevencion de reservas duplicadas para el mismo slot (cancha + fecha + hora).
- Seccion Mis reservas por usuario autenticado, con cancelacion.
- Panel de administracion (usuarios y reservas) para perfiles admin.
- Exportacion de usuarios y reservas en formato CSV.
- Interfaz principal construida con Jetpack Compose + Material 3.

## Stack tecnologico

- Kotlin
- Android SDK (minSdk 24, targetSdk 36)
- Jetpack Compose + Material 3
- SQLite (SQLiteOpenHelper)
- Firebase Auth y Firestore (uso opcional)
- Gradle Kotlin DSL

## Requisitos

- Android Studio (version reciente con soporte para AGP 8.13+)
- JDK 11
- SDK Android API 36
- Emulador o dispositivo Android (API 24 o superior)

## Configuracion y ejecucion

1. Clonar el repositorio.
2. Abrir la carpeta del proyecto en Android Studio.
3. Sincronizar Gradle.
4. Verificar que exista el archivo local.properties con la ruta del SDK Android.
5. Ejecutar la app en un emulador o dispositivo.

Tambien puedes compilar desde terminal:

### Windows (PowerShell)

./gradlew.bat assembleDebug

### Linux/macOS

./gradlew assembleDebug

## Firebase (opcional)

El proyecto puede funcionar sin Firebase en modo local.
Si deseas activar autenticacion/sincronizacion remota:

- Configura un proyecto en Firebase.
- Agrega el archivo google-services.json al modulo app.
- Habilita Authentication y Firestore en Firebase Console.
- Ajusta reglas y configuracion segun el entorno.

Si Firebase no esta disponible, las reservas se mantienen en SQLite.

## Estructura del proyecto (resumen)

- app/src/main/java/com/example/campolibrefutbol/
  - LoginActivity.kt
  - MainActivity.kt
  - SQLiteUserHelper.kt
  - ui/main/home/
  - ui/main/reservations/
  - ui/main/admin/
  - ui/main/navigation/
  - ui/main/common/
  - ui/theme/

## Validacion funcional rapida

1. Crear usuario o iniciar sesion.
2. Reservar una cancha en fecha y hora.
3. Verificar que la misma cancha no se ofrezca nuevamente en ese slot.
4. Ir a Mis reservas y confirmar que aparece la reserva.
5. Cancelar la reserva y validar que desaparece.
6. Como admin, revisar gestion de usuarios/reservas y exportacion CSV.

## Documentacion tecnica

Para mayor detalle de arquitectura, modulos y flujo funcional:

- MANUAL_TECNICO.md

## Estado actual

- Version de app: 1.0
- applicationId: com.example.campolibrefutbol

## Proximos pasos sugeridos

- Agregar tests unitarios para la capa SQLite.
- Evaluar migracion a Room para persistencia tipada y migraciones.
- Incorporar pruebas UI Compose para los flujos criticos.
