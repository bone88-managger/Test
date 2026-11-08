# Aplicación Android Mega Millions

La aplicación es nativa para Android y se construye como APK de depuración mediante GitHub Actions.

## Generación

- Usa una instancia de SecureRandom del sistema Android, sin semillas manuales ni basadas en la hora.
- Selecciona cinco bolas blancas distintas del 1 al 70 mediante Fisher–Yates.
- Usa muestreo acotado con rechazo para evitar sesgo de módulo.
- Selecciona la Mega Ball de forma independiente entre 1 y 24.
- El generador crea el candidato sin consultar el historial. Después, el filtro ALL lo compara con todos los sorteos importados.
- Se rechaza un candidato si comparte más de dos valores numéricos distintos con cualquier sorteo histórico, conservando la regla de comparación existente entre los seis valores.

## Duplicados, intentos y registro

- No se acepta una combinación completa que ya esté en el historial o en las combinaciones aceptadas anteriormente por la aplicación.
- Las combinaciones aceptadas se conservan localmente entre reinicios y al volver a importar el historial.
- El límite de intentos se puede configurar desde la pantalla (1 a 250.000; valor inicial 100.000).
- Cada intento queda registrado localmente, tanto si se acepta como si se rechaza. El registro guarda los números, la fecha/hora, el número de intento y el motivo.

## Historial

- Importa PDF desde el selector de archivos Android.
- También admite CSV con columnas date, white1, white2, white3, white4, white5, mega_ball.
- Conserva el historial oficial, las combinaciones aceptadas y el registro en el dispositivo.
- Los sorteos antiguos pueden contener valores de eras anteriores (1–99); los candidatos nuevos usan el formato actual.

## Pruebas

El workflow Android APK ejecuta las pruebas unitarias antes de compilar el APK. Las pruebas cubren el rechazo del tramo incompleto del muestreo, la selección sin reemplazo, los rangos, las frecuencias de bolas blancas y Mega Ball, y la distribución por rangos.

Las pruebas estadísticas son controles de calidad para detectar sesgos o errores de implementación; no constituyen una prueba matemática de seguridad criptográfica. Los filtros históricos y la prohibición de duplicados condicionan la distribución de los resultados aceptados.

## Obtener el APK

Al completarse correctamente el workflow Android APK, abre la ejecución en GitHub Actions, descarga el artefacto mega-millions-debug-apk y extrae app-debug.apk. En Android, permite instalar la aplicación desde esa fuente cuando el sistema lo solicite.
