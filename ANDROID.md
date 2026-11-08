# Aplicación Android

La app nativa se construye como APK de depuración mediante GitHub Actions.

## Funciones
- Importa el PDF histórico de Mega Millions desde el selector de archivos Android.
- También admite CSV con columnas `date,white1,white2,white3,white4,white5,mega_ball`.
- Guarda el historial y las combinaciones generadas localmente.
- Genera cinco bolas blancas (1–70) y Mega Ball independiente (1–24) con `SecureRandom`.
- Rechaza filas completas repetidas y cualquier combinación con más de dos valores numéricos distintos en común con una fila histórica.

## Obtener el APK
Al completarse el workflow **Android APK**, abre la ejecución en GitHub Actions, baja el artefacto `mega-millions-debug-apk` y extrae `app-debug.apk`. En Android, permite instalar la aplicación desde esa fuente cuando el sistema lo solicite.

La primera vez, pulsa **Cargar PDF / CSV** y selecciona el archivo histórico en el teléfono. La app conserva los datos localmente para usos posteriores.
