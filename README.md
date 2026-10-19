# ForjaChat 1.5

Tu espacio para todas las IA: ChatGPT, Claude, Gemini, DeepSeek, Qwen, GLM, Arena y los modelos que
agregues, en una sola app Android. Cada asistente se abre en su propia WebView y conserva su sesión.

## Qué hace
- **Diseño adaptable:** en tablet/plegable muestra barra lateral (asistentes, recientes, agregar modelo)
  y navegación inferior; en teléfono, lista de asistentes y chat a pantalla completa.
- Logo y color de marca por asistente, favoritos, búsqueda, modelos propios
  (editar con pulsación larga, exportar/importar JSON) y vista dividida.
- Barra de entrada con rutas:
  - **Auto:** un coordinador por reglas elige asistente (código, búsqueda actual…).
  - **Web:** rellena la caja del chat; tú pulsas enviar.
  - **Local:** próximamente.
  - **Asistencia:** copia tu pregunta para pegarla en el chat.
- Sin permiso de ubicación; solo tráfico HTTPS.

## Compilar en GitHub
1. Sube este proyecto a un repositorio (rama `main`, incluida la carpeta `.github`).
2. **Actions → Build APK** corre en cada push o con **Run workflow**.
3. Descarga el APK en **Artifacts → ForjaChat-debug-apk**.

## Compilar en local
Con Android SDK y Gradle 8.7+: `gradle assembleDebug`
(APK en `app/build/outputs/apk/debug/`).

## Notas
- APK de depuración (firma debug). Si tienes instalada otra versión firmada con otra clave, desinstálala antes.
- Los logos de las marcas pertenecen a sus propietarios y se usan solo para identificar cada servicio.
