# Mega Millions Simulator — Android APK

Aplicación **nativa para Android**. No necesita navegador ni servidor web.

## Qué hace
- Importa el historial desde un PDF o CSV usando el selector de archivos del teléfono.
- Conserva el historial y las combinaciones generadas en el almacenamiento local de la app.
- Genera cinco bolas blancas distintas de 1 a 70 y una Mega Ball independiente de 1 a 24.
- Usa `SecureRandom` para generar números aleatorios.
- Rechaza una fila completa ya existente y cualquier candidato que comparta más de dos valores numéricos distintos con una fila histórica.
- Compara los seis valores como conjunto numérico, sin distinguir bola blanca o Mega Ball; un valor duplicado dentro de una fila cuenta una sola vez.
- Admite valores históricos de eras anteriores (1–99) sin imponerles los rangos actuales.

## Instalar
El workflow **Android APK** construye `app-debug.apk` en GitHub Actions y lo publica como artefacto `mega-millions-debug-apk`.
1. Abre la pestaña Actions del repositorio.
2. Entra en la ejecución más reciente de **Android APK** que haya terminado correctamente.
3. Descarga el artefacto `mega-millions-debug-apk` y extrae `app-debug.apk`.
4. Abre el APK en Android y autoriza la instalación desde esa fuente si el sistema lo solicita.

## Primera ejecución
Pulsa **Cargar PDF / CSV** y selecciona el PDF histórico guardado en el teléfono. El lector extrae las filas con fecha, cinco bolas y Mega Ball; después la app conserva el historial localmente. También acepta CSV con columnas:
`date,white1,white2,white3,white4,white5,mega_ball`.

## Pruebas del lector PDF
El PDF histórico aportado en esta conversación tiene 45 páginas. Su extracción de texto produjo 1.406 sorteos únicos (11/03/2026 a 05/17/2013 en el orden del documento), incluyendo números antiguos como 75. La app permite revisar el total importado en pantalla.

## Desarrollo del motor Python
El módulo `lottery.py` conserva el motor de referencia y sus pruebas:
```bash
pip install -r requirements.txt
pytest -q
```

## Límite matemático
Este simulador no predice sorteos ni puede garantizar que nadie gane. Las restricciones solo determinan qué combinaciones acepta la aplicación; no cambian las probabilidades del sorteo oficial.
