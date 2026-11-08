# Mega Millions Simulator — Android APK

Aplicación **nativa para Android**. No necesita navegador ni servidor web.

## Requisitos implementados

- Generación con SecureRandom de Android, sin semillas manuales ni basadas en la hora.
- Selección de cinco bolas blancas distintas entre 1 y 70 mediante Fisher–Yates.
- Muestreo acotado con rechazo explícito para evitar sesgo de módulo.
- Mega Ball independiente entre 1 y 24.
- El generador produce candidatos sin consultar el historial; después se aplican los filtros históricos.
- Filtro ALL: cada candidato se compara con todas las filas históricas y se rechaza si comparte más de dos valores numéricos distintos con alguna fila. Se conservan los criterios de comparación del motor existente: se comparan los seis valores como conjunto numérico, sin distinguir bola blanca o Mega Ball.
- Bloqueo permanente de combinaciones completas ya presentes en el historial o aceptadas anteriormente por la aplicación.
- Límite configurable de intentos (1–250.000; valor inicial 100.000). Si no se encuentra una combinación válida, la aplicación informa del fallo y no omite los filtros.
- Registro local de todos los intentos, aceptados o rechazados, con combinación, fecha/hora, número de intento y motivo.
- Historial oficial, combinaciones aceptadas y registro conservados en el dispositivo. Volver a importar el PDF no borra las generaciones anteriores.
- Pruebas estadísticas de frecuencia y distribución por rangos sobre el muestreador sin filtrar.

## Importar el historial

Pulsa **Cargar PDF / CSV** y selecciona el archivo histórico guardado en el teléfono. El lector extrae las filas con fecha, cinco bolas y Mega Ball. También acepta CSV con columnas:

date,white1,white2,white3,white4,white5,mega_ball

El historial puede contener números de eras anteriores (1–99); los candidatos nuevos siempre usan el formato actual.

## Instalar el APK

El workflow **Android APK** ejecuta primero las pruebas unitarias y luego construye app-debug.apk, que publica como artefacto mega-millions-debug-apk.

1. Abre la pestaña Actions del repositorio.
2. Entra en la ejecución más reciente de **Android APK** que haya terminado correctamente.
3. Descarga el artefacto mega-millions-debug-apk y extrae app-debug.apk.
4. Abre el APK en Android y autoriza la instalación desde esa fuente si el sistema lo solicita.

## Pruebas del generador

Las pruebas unitarias verifican el rechazo del tramo incompleto en el muestreo acotado, la selección sin reemplazo, los rangos y pruebas chi-cuadrado amplias para frecuencias de bolas blancas, Mega Ball y rangos. Son pruebas de detección de errores de implementación, no una demostración matemática de seguridad ni una certificación criptográfica.

## Nota sobre los filtros

El muestreador bruto es uniforme. Los filtros históricos y la prohibición de duplicados condicionan las combinaciones que finalmente se aceptan; por tanto, las combinaciones aceptadas no tienen una distribución uniforme sobre todo el espacio original. Las frecuencias de los tests se miden antes de aplicar esos filtros.

La aplicación no predice sorteos ni cambia las probabilidades de Mega Millions.
