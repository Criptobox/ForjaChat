# ForjaChat 1.6

Tu espacio para todas las IA: ChatGPT, Claude, Gemini, DeepSeek, Qwen, GLM, Arena y los modelos que
agregues, en una sola app Android. Cada asistente se abre en su propia WebView y conserva su sesión.

## Pantallas
- **Inicio:** tarjetas de asistentes con su logo, buscador, favoritos, prompts rápidos y preguntas recientes.
- **Chat:** el asistente a pantalla completa con barra de progreso, pantalla de error con "Reintentar",
  franja de logos para cambiar de asistente y vista dividida (dos a la vez).
- **Prompts:** biblioteca de plantillas (8 de ejemplo); crear, editar, eliminar y usar.
- **Historial:** las preguntas que enviaste; reutilizar, copiar o eliminar.
- **Ajustes:** ruta predeterminada, rellenado automático, abrir el último asistente al iniciar,
  exportar/importar copia (modelos, prompts y favoritos), borrar historial y cerrar sesiones web.
- En tablet o plegable (ancho >= 700 dp) añade una barra lateral con los asistentes.

## Rutas de envío
- **Auto:** un coordinador por reglas elige el asistente (código, razonamiento, datos actuales,
  imágenes, traducción, escritura).
- **Web:** rellena la caja del chat; tú pulsas enviar. Si no encuentra la caja, copia tu pregunta.
- **Asistencia:** copia tu pregunta para pegarla en el chat.
- **Local:** próximamente.

## Extras
- Dictado por voz en la barra de entrada.
- Compartir texto desde cualquier app hacia ForjaChat (menú Compartir y menú de selección de texto).
- Sin permiso de ubicación; solo tráfico HTTPS. Tus prompts e historial se quedan en el dispositivo.

## Compilar en GitHub
1. Sube este proyecto a un repositorio (rama `main`, incluida la carpeta `.github`).
2. **Actions → Build APK** corre en cada push o con **Run workflow**.
3. Descarga el APK en **Artifacts → ForjaChat-debug-apk**.

## Compilar en local
Con Android SDK y Gradle 8.7+: `gradle assembleDebug`
(APK en `app/build/outputs/apk/debug/`).

## Notas
- APK de depuración (firma debug). Si tienes otra versión instalada con otra firma, desinstálala antes.
- Los logos de las marcas pertenecen a sus propietarios y se usan solo para identificar cada servicio.
