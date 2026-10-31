# KITT para Android — prototipo 0.1

Aplicación nativa Android (Kotlin) con:
- Interfaz oscura inspirada en KITT.
- Reconocimiento de voz en español de Argentina, si Android dispone de un servicio de reconocimiento.
- Respuestas locales básicas sin usar una API de IA.
- Conversación online opcional con OpenAI API, con clave introducida por el usuario.
- Síntesis de voz de Android con velocidad y tono ajustados para una voz más grave.
- El audio sale por la ruta de audio seleccionada por Android; al conectar Bluetooth, el teléfono normalmente enruta la reproducción al equipo compatible.

## Importante
Este es un prototipo inicial de teléfono, no un APK compilado ni una integración certificada de Android Auto.
Android Auto solo admite determinadas categorías de aplicaciones y exige interfaces/servicios específicos. Una app conversacional genérica no puede declararse compatible sin una arquitectura apropiada y validación de Google. La integración para el auto debe ser una segunda fase, probablemente con una experiencia de audio/media aprobada o las nuevas APIs de voz/conversación que estén disponibles para la categoría y el programa de publicación.

## Abrir y compilar
1. Instalá Android Studio en una computadora.
2. Descomprimí el ZIP y abrí la carpeta `KITT_Android_Project` como proyecto.
3. Dejá que Gradle sincronice el proyecto.
4. Conectá el teléfono Android 15 con Depuración USB habilitada, o generá un APK desde Build > Build APK(s).
5. Instalá el APK de prueba y aceptá el permiso de micrófono.

El proyecto requiere Android SDK 35 y JDK 17; Android Studio suele ayudar a instalar los componentes faltantes.

## Activar la IA online
1. Creá una clave API desde la plataforma de OpenAI.
2. Abrí KITT > Configurar IA y pegá la clave.
3. Las llamadas a la API se cobran según la cuenta, modelo y uso. No se incluye una clave en el código.
4. Para distribuir la app a otras personas, no se recomienda guardar claves personales en el cliente; se debe usar un backend propio con límites y autenticación.

## Prueba rápida
- Tocá “Probar voz”.
- Tocá “Hablar con KITT” y decí “¿Quién sos?”, “¿Cómo estás?” o “Ayuda”.
- Para probar conversación avanzada, configurá una clave API y preguntá algo que no sea un comando local.

## Limitaciones conocidas del prototipo
- No hay palabra de activación permanente (“Hola KITT”); se toca el botón para iniciar la escucha.
- El reconocimiento de voz sin conexión depende del paquete de reconocimiento instalado y su idioma offline.
- La voz grave depende de las voces disponibles en el motor TTS del teléfono. El tono se ajusta, pero no clona la voz original de la serie.
- No lee datos del auto ni controla funciones del vehículo.
- Bluetooth funciona mediante el enrutamiento de audio del sistema; el micrófono del estéreo no está garantizado en todos los modelos.
- No es una app aprobada ni publicada para Android Auto.

## Seguridad al conducir
Configurá y probá la aplicación con el vehículo estacionado. No manipules el teléfono mientras conducís. No uses comandos que distraigan ni confíes en el prototipo para decisiones críticas.


## Compilar usando solamente el teléfono (nube)
Este ZIP incluye `.github/workflows/android-apk.yml` para compilar el APK en GitHub Actions sin instalar Android Studio en el teléfono.

1. En el navegador del teléfono, entrá a https://github.com/new y creá un repositorio privado llamado `KITT-Android`.
2. En el repositorio, elegí **Add file > Upload files** y subí el contenido del ZIP descomprimido (incluida la carpeta oculta `.github/workflows`). Si la interfaz móvil no deja subir carpetas, usá la web en modo escritorio o el método de carga de archivos que ofrezca GitHub.
3. Confirmá el commit en la rama `main`.
4. Abrí la pestaña **Actions**, seleccioná **Build KITT APK** y tocá **Run workflow** si no se inició automáticamente.
5. Cuando el trabajo termine correctamente, abrí la ejecución y descargá el artefacto `KITT-debug-apk`.
6. Extraé el ZIP del artefacto, tocá `app-debug.apk` y autorizá la instalación desde esa fuente si Android lo solicita.

GitHub puede cambiar el diseño móvil de la interfaz. No compartas contraseñas ni tokens con nadie. El artefacto de prueba está firmado para depuración; no es una publicación de Play Store.
