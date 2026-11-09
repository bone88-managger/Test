# Mega Millions Simulator — memoria del proyecto y registro de conversación

> Documento de continuidad del proyecto. Leer antes de continuar el trabajo en otro chat.
>
> **Regla de mantenimiento:** no borrar tareas ni decisiones anteriores. Cuando una tarea quede terminada y verificada, tachar su texto con Markdown (\`~~tarea~~\`), conservarla en su sección y añadir inmediatamente la fecha y hora local de finalización, incluyendo la zona horaria (formato recomendado: \`— Completada: AAAA-MM-DD HH:MM UTC±HH:MM\`). Usar la hora real de verificación, no una hora estimada. Añadir nuevas tareas sin reemplazar el historial. No marcar como terminada una compilación o prueba hasta comprobar su resultado.

## 1. Identidad y objetivo del proyecto

- Repositorio: https://github.com/bone88-managger/Test
- Rama de trabajo utilizada: \`feature/mega-millions-simulator\`
- Producto deseado: aplicación **APK nativa para Android**. El usuario no quiere que el producto final sea una aplicación web.
- Nombre de trabajo: Mega Millions Simulator.
- Objetivo principal expresado por el usuario: que no se pueda predecir el funcionamiento del generador ni la forma en que se generan sus números. La prioridad es la imprevisibilidad criptográfica y la corrección del proceso, no prometer premios ni predecir sorteos.
- La aplicación usa el formato actual: cinco bolas blancas de 1 a 70 y una Mega Ball de 1 a 24.
- El usuario quiere revisar las decisiones punto por punto. No añadir funciones que no haya aprobado.

## 2. Diez requisitos aprobados para implementar

Estos son los diez requisitos que el usuario aprobó. La numeración corresponde a los puntos originales de la conversación; por eso no es consecutiva.

1. **CSPRNG criptográfico:** utilizar el generador criptográfico seguro del sistema Android, sin semillas manuales, basadas en la hora ni en resultados anteriores.
2. **Selección sin reemplazo:** elegir cinco bolas blancas distintas en cada combinación.
3. **Rejection Sampling:** evitar sesgo de módulo al convertir valores aleatorios en rangos acotados.
4. **Protección contra secuencias repetitivas/predecibles:** evitar semillas predecibles y mecanismos que reproduzcan secuencias. No sustituir aleatoriedad por reglas que fuercen patrones visuales.
5. **Pruebas estadísticas de aleatoriedad:** realizar pruebas de calidad para detectar sesgos de implementación. Las pruebas estadísticas no demuestran por sí solas seguridad criptográfica.
6. **Generación independiente del historial + filtro ALL (opción B):** el generador produce candidatos sin consultar sorteos previos; después el filtro compara cada candidato con todos los sorteos del historial.
7. **Control de distribución uniforme:** comprobar que el muestreador bruto no favorezca artificialmente rangos bajos, medios o altos. No imponer que cada combinación contenga números de todos los rangos; eso introduciría un sesgo.
8. **Bloqueo permanente de combinaciones duplicadas (opción B):** conservar las combinaciones aceptadas y evitar volver a aceptarlas incluso después de cerrar y abrir la app.
9. **Límite configurable de intentos:** detenerse al alcanzar el límite; informar si no se encuentra candidato y nunca saltarse los filtros para forzar un resultado.
10. **Registro local de generaciones:** conservar intentos aceptados y rechazados, combinación, fecha/hora, número de intento y motivo del rechazo/aceptación.

### Regla histórica aprobada

- Se eligió el historial completo (ALL).
- Se rechaza una combinación si tiene más de dos coincidencias entre sus cinco bolas blancas y las cinco bolas blancas de cualquier sorteo del historial.
- La Mega Ball no cuenta como coincidencia, aunque su valor numérico coincida con una bola blanca. Tampoco se compara la Mega Ball del candidato para este filtro.
- La generación debe ser independiente del historial; el historial solo se usa después para filtrar.

## 3. Decisiones rechazadas o aplazadas — conservar como historial

No implementar estas funciones salvo que el usuario cambie expresamente de opinión:

- ~~Punto 5 original: ofuscación del código contra ingeniería inversa — RECHAZADO.~~
- ~~Punto 6 original: protección adicional de datos — APLAZADO; el usuario dijo «por ahora no».~~
- ~~Punto 9 original: módulo independiente de entropía adicional — RECHAZADO. Se conserva el CSPRNG del sistema Android.~~
- ~~Punto 10 original: protección contra manipulación/integridad del generador — RECHAZADO.~~
- ~~Punto 15: panel de estadísticas internas del generador — NO aprobado; el usuario dijo «No, hasta ahí».~~

No confundir las numeraciones originales con la lista de diez requisitos aprobados de la sección 2.

## 4. Registro cronológico de decisiones de la conversación

Este registro resume las decisiones visibles y los antecedentes conservados en el contexto del chat. No se debe presentar como una transcripción literal palabra por palabra de mensajes que no estén disponibles.

1. El usuario compartió un CSV de ejemplo del historial de Mega Millions; aclaró que no era el archivo original, pero servía como ejemplo.
2. El usuario eligió el formato actual, coincidencias ALL e historial ALL.
3. El usuario pidió implementar el proyecto en el repositorio \`bone88-managger/Test\`.
4. El usuario aclaró que quería una APK, no una aplicación web, y confirmó que el enlace/proyecto abría correctamente.
5. El usuario explicó que su objetivo no era ganar, sino que nadie pudiera predecir la aplicación ni la forma de generación de números.
6. El usuario pidió que se explicara cada punto de seguridad por separado y que solo se agregaran los puntos aprobados.
7. Aprobó CSPRNG, selección sin reemplazo, Rejection Sampling y protección contra secuencias repetitivas.
8. Rechazó la ofuscación.
9. Para la separación entre generador e historial, eligió la opción B: generación independiente del historial y filtros históricos posteriores.
10. Aprobó pruebas estadísticas de aleatoriedad.
11. Rechazó el módulo independiente de entropía adicional; se mantiene la fuente criptográfica segura de Android.
12. Rechazó protección contra manipulación del generador.
13. Aprobó control de distribución uniforme, entendida como comprobación estadística del muestreador y no como obligación de equilibrar cada combinación.
14. Aprobó impedir duplicados permanentemente.
15. Aprobó un límite configurable de intentos.
16. Aprobó un registro de generaciones aceptadas y rechazadas.
17. Rechazó, por ahora, el panel separado de estadísticas internas (punto 15).
18. Solicitó implementar los diez requisitos aprobados y dejar fuera los demás.
19. Solicitó crear este documento de continuidad dentro del proyecto para retomar el trabajo desde otro chat y mantener las tareas completadas tachadas, no borradas.
20. Solicitó que cada tarea tachada incluya también la fecha y hora en que fue completada; se estableció registrar la hora local con su zona horaria.

## 5. Cambios de implementación reportados en GitHub

En la rama \`feature/mega-millions-simulator\` se reportó haber actualizado:

- \`app/src/main/java/com/bone88/megamillions/MainActivity.kt\`
- \`app/build.gradle.kts\`
- \`app/src/test/java/com/bone88/megamillions/SecureLotteryRandomTest.kt\` (nuevo)
- \`.github/workflows/android-apk.yml\` (nuevo workflow Android APK)
- \`README.md\`
- \`ANDROID.md\`

Cambios descritos:
- \`SecureRandom\`, Fisher–Yates y muestreo acotado con rechazo.
- Cinco bolas blancas distintas de 1–70 y Mega Ball independiente de 1–24.
- Filtro histórico ALL posterior a la generación.
- Registro local de combinaciones aceptadas y rechazadas.
- Límite de intentos configurable entre 1 y 250.000, con valor inicial 100.000.
- Pruebas unitarias de rangos y frecuencias/distribución.

**Importante:** estos cambios fueron reportados como escritos en la rama, pero no deben considerarse una entrega final verificada hasta revisar el código, ejecutar las pruebas y confirmar la compilación APK. Tampoco se debe asumir que el criterio de coincidencias o la persistencia son correctos sin revisar el comportamiento real.

## 6. Lista de tareas de trabajo

### En curso / pendiente de verificación

- [ ] Revisar el código Android actualizado y comprobar que compila.
- [ ] Confirmar que las pruebas unitarias del generador terminan correctamente.
- [ ] Revisar que la protección contra secuencias predecibles corresponde al requisito aprobado y no añade reglas sesgadas.
- [ ] Confirmar el comportamiento exacto del filtro ALL y el límite de dos coincidencias frente a cada sorteo histórico.
- [ ] Confirmar que la comparación de duplicados considera los cinco números blancos sin importar el orden y la Mega Ball como campo separado.
- [ ] Confirmar que el registro persiste tras cerrar/reabrir la app y que reimportar el historial no borra los registros ni las combinaciones guardadas.
- [ ] Confirmar que el límite de intentos nunca permite aceptar una combinación que incumpla un filtro.
- [ ] Verificar que el workflow Android APK termina con éxito y publica el artefacto APK.
- [ ] Instalar y probar el APK en el teléfono Android del usuario.
- [ ] Al completar cada tarea y verificarla, tachar su texto y añadir en la misma línea la fecha y hora local exactas de finalización con zona horaria; conservar la tarea en esta lista.

### Fuera de alcance por decisión del usuario

- [x] No añadir ofuscación del código.
- [x] No añadir módulo independiente de entropía.
- [x] No añadir protección contra manipulación del generador.
- [x] No añadir panel de estadísticas internas por ahora.
- [x] Mantener el producto final como APK Android, no web.

## 7. Estado de compilación conocido al crear este documento

En la última consulta disponible:
- El workflow Python \`pytest\` figuraba como completado con éxito.
- El workflow Android \`Android APK\` seguía en ejecución: la etapa \`Run Android unit tests\` figuraba como \`in_progress\`; compilación y subida del APK estaban pendientes.
- Por tanto, **no estaba confirmado que existiera un APK nuevo descargable**.

Comprobar de nuevo el estado de GitHub Actions antes de afirmar que la compilación terminó.

## 8. Cómo continuar en un chat nuevo

1. Abrir este documento: \`docs/SEGUIMIENTO_Y_CONVERSACION.md\`.
2. Revisar también \`README.md\`, \`ANDROID.md\`, el código Kotlin, las pruebas y el workflow.
3. Consultar el estado real de la rama y GitHub Actions; no confiar ciegamente en estados antiguos de este documento.
4. Continuar desde la primera tarea pendiente.
5. Al completar y verificar una tarea, tacharla con Markdown y añadir en esa misma línea la fecha y hora local exactas de finalización, incluyendo la zona horaria; no eliminarla ni borrar decisiones anteriores.
6. No agregar funciones nuevas sin explicarlas al usuario y recibir su aprobación.


## 9. Prueba real del usuario y correcciones de interfaz (2026-11-08)

El usuario instaló el APK en su teléfono y reportó:
- La instalación se completa correctamente.
- La selección/importación del PDF está disponible.
- Durante la lectura del PDF aparece una pantalla negra; después la aplicación vuelve a funcionar.
- La generación de combinaciones funciona.
- No puede desplazarse hacia abajo para consultar los entradas del registro.

Correcciones aplicadas en la rama:
- Se movió la lectura y el análisis del PDF a un hilo de trabajo (`Dispatchers.IO`) para evitar bloquear la interfaz.
- Se añadió un indicador visible de carga y se deshabilitó temporalmente el botón de importación mientras se procesa el archivo.
- Se reemplazó el diseño con una lista desplazable única para toda la pantalla; el registro se muestra dentro de esa lista, sin una lista desplazable anidada dentro de una columna.

Commit de corrección: `339c1819188123993dc648fe75d16d2a673321c1`.

Pendiente:
- [ ] Confirmar que GitHub Actions compila correctamente esta corrección y publica un APK nuevo.
- [x] ~~Instalar el APK corregido en el teléfono y verificar que el PDF muestra el indicador de carga sin pantalla negra.~~ — Completada: 2026-11-08 18:06:26 UTC-04:00
- [x] ~~Verificar que se puede desplazar desde la parte superior hasta todos los registros.~~ — Completada: 2026-11-08 18:06:26 UTC-04:00
- [ ] Confirmar que la importación conserva las generaciones anteriores y que la generación sigue funcionando después de la importación.

No marcar estas pruebas como completadas hasta que el usuario pruebe el nuevo APK.

---
Última actualización: 2026-11-08.

## 10. Separación del historial de rechazos

El usuario confirmó que las dos correcciones anteriores (lectura del PDF y desplazamiento) funcionan en su teléfono. Después indicó que el historial de rechazos hace demasiado larga la pantalla principal y precisó que se debe mover únicamente ese historial, dejando todo lo demás exactamente como está.

Cambio realizado en `app/src/main/java/com/bone88/megamillions/MainActivity.kt`: 
- Se añadió una pantalla independiente para consultar exclusivamente las generaciones rechazadas, con su combinación, número de intento, motivo y fecha/hora.
- Se añadió en la pantalla principal el botón «Ver rechazos (N)» y un botón «VOLVER» en la pantalla de rechazos.
- La pantalla principal conserva el resto de sus controles, avisos, importación, generación, resultado, combinaciones aceptadas y registros aceptados.
- No se modificaron el generador, el filtro histórico, la persistencia ni los datos guardados. Los rechazos siguen almacenándose localmente; solo cambia dónde se muestran.

Commit: `4d17ee3689a1bb7710f8d3de09e76db9f5bd365b`.

Pendiente de verificación:
- [x] ~~Confirmar que GitHub Actions compila este cambio y publica un APK nuevo.~~ — Completada: 2026-11-08 18:24:21 UTC-04:00
- [x] ~~Instalar y probar el APK: confirmar que la pantalla principal permanece igual salvo por el botón de rechazos.~~ — Completada: 2026-11-08 18:24:21 UTC-04:00
- [x] ~~Abrir «Ver rechazos», revisar la lista y volver a la pantalla principal.~~ — Completada: 2026-11-08 18:24:21 UTC-04:00
- [x] ~~Confirmar que todos los rechazos se conservan y que el historial aceptado sigue visible en la pantalla principal.~~ — Completada: 2026-11-08 18:24:21 UTC-04:00

El usuario instaló el APK nuevo y confirmó que todo funciona correctamente. Las cuatro verificaciones anteriores quedan completadas según esa confirmación.

Actualización de verificación del usuario: 2026-11-08 18:24:21 UTC-04:00. El usuario confirmó: «Ya la instale y todo funciona bien, ya podemos continuar».


## 11. Validación de rangos al importar el historial

Durante la revisión del código se detectó que los lectores PDF y CSV aceptaban valores de hasta 99, en lugar de aplicar los rangos oficiales de Mega Millions. Eso podía introducir sorteos inválidos en el historial y afectar el filtro.

Corrección realizada:
- Se añadió una validación común: exactamente cinco bolas blancas distintas entre 1 y 70, y Mega Ball entre 1 y 24.
- Se aplicó la validación tanto a la lectura PDF como CSV.
- Se añadieron pruebas unitarias para límites válidos, números fuera de rango, bolas blancas duplicadas y cantidad incorrecta de bolas.
- No se cambiaron el generador aleatorio, el criterio de coincidencias históricas ni la lógica de aceptación/rechazo.

Commits:
- Código: `e4d342da40e49af07d3d050c00bd3753d05e7cca`
- Pruebas: `372c25b76c87abf05c3a5377aa941c065327cd5e`

Pendiente:
- [x] ~~Confirmar que las pruebas Android pasan con la nueva validación.~~ — Completada: 2026-11-09 12:22:46 UTC-04:00
- [x] ~~Confirmar que el workflow Android APK compila y publica el APK.~~ — Completada: 2026-11-09 12:22:46 UTC-04:00
- [x] ~~Instalar esta versión en el teléfono y probar la importación de PDF/CSV.~~ — Completada: 2026-11-09 12:22:46 UTC-04:00
- [ ] Continuar la auditoría de filtro histórico, duplicados permanentes, límite de intentos y persistencia del registro.


## 12. Auditoría de lógica interna — revisión inicial (2026-11-09)

El usuario confirmó que instaló el APK y que la importación de PDF y CSV funciona correctamente. En GitHub Actions, para el commit `ac8ffa560110dd3d532a051516119267846dd7e7`:
- Workflow `Tests`, ejecución 57: `pytest` completado con éxito.
- Workflow `Android APK`, ejecución 44: tarea `build` completada con éxito.
- El artefacto `mega-millions-debug-apk` está publicado y no ha caducado.

Revisión estática de `MainActivity.kt`:

**Filtro histórico ALL**
- Cada candidato se genera primero sin consultar el historial.
- Después se compara con todas las filas de `history`.
- En la versión auditada, el código reunía los seis valores numéricos de cada sorteo en conjuntos y rechazaba cuando la intersección superaba dos valores.
- El usuario aclaró el criterio correcto: **solo se comparan las cinco bolas blancas contra las cinco bolas blancas**; la Mega Ball queda completamente excluida del conteo de coincidencias. La implementación actual debe revisarse y, si corresponde, corregirse antes de considerar este filtro conforme al requisito.
- La búsqueda se detiene al encontrar el primer sorteo conflictivo, lo cual es suficiente para rechazar el candidato.

**Duplicados**
- La clave ordena las cinco bolas blancas y mantiene la Mega Ball como campo separado.
- Por tanto, el orden de las blancas no crea una combinación distinta, y cambiar la Mega Ball sí crea otra combinación.
- Se consulta contra el historial importado y contra las combinaciones aceptadas guardadas en `generated`.
- Tras aceptar una combinación, la interfaz la añade a `generated` y la guarda.

**Límite de intentos**
- El valor se valida entre 1 y 250.000.
- El bucle realiza como máximo ese número de intentos y registra cada candidato.
- Solo devuelve una combinación si supera los filtros; al agotarse el límite devuelve resultado vacío y no omite los filtros.

**Persistencia — todavía requiere pruebas**
- Las combinaciones aceptadas se serializan en SharedPreferences.
- El registro completo se serializa en `generation_log.json` dentro del almacenamiento privado de la app.
- Al volver a importar, el código conserva en memoria `generated` y `generationLog` y vuelve a guardarlos.
- No hay todavía pruebas automatizadas de reinicio real, reimportación o recuperación tras datos corruptos. Además, los lectores de persistencia capturan cualquier excepción y devuelven listas vacías; si un archivo JSON se daña, la protección contra duplicados o el registro podría desaparecer silenciosamente. No se debe cambiar ese comportamiento sin diseñar y aprobar una recuperación segura.

**Cobertura de pruebas observada**
- Las pruebas actuales cubren rangos, muestreo acotado, bolas blancas distintas y frecuencias estadísticas.
- No se encontraron pruebas unitarias específicas para filtro histórico, duplicados, límite de intentos ni persistencia.

Pendiente de la auditoría:
- [x] ~~Corregir/verificar el filtro ALL para comparar exclusivamente las cinco bolas blancas de cada combinación; excluir siempre la Mega Ball del conteo.~~ — Completada: 2026-11-09 12:35:04 UTC-04:00
- [x] ~~Añadir pruebas deterministas para filtro ALL: exactamente 2 coincidencias blancas debe permitir el candidato; 3 debe rechazarlo; coincidencias de valor entre bola blanca y Mega Ball no deben contar.~~ — Completada: 2026-11-09 12:35:04 UTC-04:00
- [ ] Añadir pruebas para duplicados con blancas en distinto orden y Mega Ball distinta/igual.
- [ ] Añadir pruebas para el límite de intentos, verificando que no se aceptan candidatos inválidos al agotarlo.
- [ ] Probar en el teléfono que combinaciones aceptadas y registro sobreviven al cierre forzado/reapertura y a reimportar el historial.
- [ ] Definir cómo detectar y manejar almacenamiento local dañado sin borrar silenciosamente el registro ni la protección contra duplicados.

Corrección implementada después de la auditoría inicial (2026-11-09):
- El filtro ahora calcula la intersección exclusivamente entre `candidate.white` y `old.white`; rechaza si hay más de dos coincidencias blancas.
- Se eliminó la propiedad auxiliar que combinaba blancas y Mega Ball para el filtro.
- Se actualizó el texto de reglas y el motivo del rechazo para indicar coincidencias de bolas blancas.
- Se añadieron tres pruebas deterministas: dos coincidencias permiten, tres rechazan y coincidencias cruzadas con Mega Ball no cuentan.
- Commits: código `48091c561ddab61a26c2fb52b510c6e40f7daa79`; pruebas `d208ccd174d41c13c2acb892ae1471459caad583`.
- GitHub Actions para el commit `d208ccd174d41c13c2acb892ae1471459caad583`: workflow `Tests` (run 42655188077) completado con éxito; workflow `Android APK` (run 42655187940) completado con éxito. Artefacto `mega-millions-debug-apk`, ID `13692095723`, disponible y no caducado.
- [x] ~~Confirmar que las pruebas deterministas del filtro histórico pasan.~~ — Completada: 2026-11-09 12:35:04 UTC-04:00
- [x] ~~Confirmar que el workflow Android APK compila y publica un artefacto.~~ — Completada: 2026-11-09 12:35:04 UTC-04:00
- [x] ~~Instalar este APK y verificar en el teléfono que la Mega Ball no suma coincidencias en la generación real.~~ — Completada: 2026-11-09 12:45:46 UTC-04:00

La revisión y corrección del filtro histórico quedó implementada; las pruebas automatizadas, la compilación Android y la prueba real en el teléfono del usuario fueron completadas correctamente. El usuario confirmó que instaló el APK y que funciona correctamente. Verificación en dispositivo: 2026-11-09 12:45:46 UTC-04:00.


## 13. Corrección del criterio de coincidencias por columna (2026-11-09)

Nueva decisión confirmada por el usuario:
- Las cinco bolas blancas se comparan por posición/columna, manteniendo el orden ascendente.
- Solo cuenta una coincidencia cuando el mismo número ocupa la misma columna en ambas combinaciones.
- Se rechaza una candidata cuando tiene 3 o más coincidencias en las mismas columnas.
- Compartir tres números en columnas diferentes no es motivo de rechazo.
- Esta regla se aplica contra el historial oficial y contra las combinaciones generadas anteriormente.
- La Mega Ball queda excluida de esta comparación.
- El bloqueo de duplicados y las demás funciones no se modifican en esta corrección.

Ejemplo aprobado:
- A: 10 26 32 45 50
- B: 10 20 26 35 45
- Solo coincide la primera columna; B no se rechaza por coincidencias históricas/posicionales.

Implementación realizada:
- Se añadió una comparación posicional de las bolas blancas.
- El filtro ahora revisa tanto el historial oficial como las generaciones anteriores.
- Se actualizó el texto de reglas y el motivo de rechazo.
- Se añadieron pruebas para 3 números compartidos en columnas diferentes, 3 coincidencias en las mismas columnas y exactamente 2 coincidencias posicionales.

Commits:
- Código: 1b7246ec3b34be8ae80fe56d6686edc9f9b6b419
- Pruebas: 10356518f16f9f655f8d064cabd4c4592ae0b375

Pendiente de verificación:
- [ ] Confirmar que las pruebas Android pasan.
- [ ] Confirmar que el workflow Android APK compila y publica el APK.
- [ ] Instalar el APK y comprobar el ejemplo aprobado en el teléfono.


## 14. Ajuste del límite de coincidencias por columna (2026-11-09)

Nueva decisión del usuario:
- Se permite como máximo **1 coincidencia** en la misma columna entre dos combinaciones.
- Se rechaza una candidata cuando coincide en **2 o más columnas** con cualquier sorteo del historial o combinación generada anteriormente.
- Las coincidencias se cuentan solo cuando el mismo número está en la misma posición/columna; las bolas blancas se mantienen ordenadas ascendentemente.
- La Mega Ball continúa excluida.
- No se modifica el bloqueo de duplicados ni ninguna otra función.

Implementación:
- Se cambió el umbral del filtro posicional de más de 2 a más de 1 coincidencia.
- Se actualizaron el texto visible de reglas y el motivo registrado para los rechazos.
- Se actualizaron las pruebas para verificar que 1 coincidencia permite, 2 rechazan y 3 rechazan; también se conserva la prueba de números compartidos en columnas diferentes.
- Se mantiene el filtro contra el historial oficial y las combinaciones generadas anteriormente.

Pendiente de verificación:
- [ ] Confirmar que las pruebas unitarias Android pasan.
- [ ] Confirmar que el workflow Android APK compila y publica el APK.
- [ ] Instalar el APK y verificar en el teléfono que una coincidencia posicional permite y dos coincidencias posicionales rechazan.


## 15. Acciones separadas para borrar los historiales (2026-11-09)

El usuario aprobó añadir un botón independiente para borrar los registros aceptados y otro para borrar los rechazados. También confirmó que entiende y acepta el efecto sobre la memoria de la aplicación:
- Al borrar los aceptados, esas combinaciones dejan de estar guardadas y podrán volver a aceptarse si se generan otra vez.
- Al borrar los rechazados, esas combinaciones dejan de estar marcadas como rechazadas y se evaluarán nuevamente desde cero.
- El borrado de una categoría no debe borrar la otra.
- La acción debe pedir confirmación antes de eliminar.

Cambio registrado en GitHub:
- Commit: `5760a7b597b95de0197f1da6b3a6245ed056f2bf`
- Mensaje del commit: `Add separate confirmed clear-history actions`
- Rama: `feature/mega-millions-simulator`
- La ejecución `Tests` #85 (run `42685493023`) terminó con `success`.
- La ejecución `Android APK` #72 (run `42685493243`) terminó con `success`.
- La compilación APK de ese commit quedó verificada como exitosa en GitHub Actions. No consta todavía una prueba del usuario en su teléfono de los botones de borrado.

Verificación pendiente:
- [x] ~~Añadir acciones independientes y confirmadas para borrar aceptados y rechazados.~~ — Completada: 2026-11-09 15:46:16 UTC-04:00
- [x] ~~Confirmar que las pruebas automatizadas pasan para el commit de los botones de borrado.~~ — Completada: 2026-11-09 15:46:16 UTC-04:00
- [x] ~~Confirmar que GitHub Actions compila el APK del commit de los botones de borrado.~~ — Completada: 2026-11-09 15:46:16 UTC-04:00
- [ ] Instalar el APK y probar ambos botones en el teléfono; confirmar que borrar una categoría no afecta a la otra.

## 16. Optimización del almacenamiento de aceptados y rechazados — pendiente

El usuario aprobó una mejora para evitar mantener decenas de miles de registros en memoria:
- Migrar los historiales de aceptados y rechazados a una base de datos local SQLite.
- Migrar los datos existentes de forma segura, sin perder los registros anteriores.
- Consultar en la base de datos si una combinación blanca ya está guardada/rechazada; comparar y guardar únicamente registros nuevos.
- No cargar todo el historial de rechazos en RAM durante la generación.
- Mostrar el historial por páginas (por ejemplo, 50 registros por carga), en lugar de cargar los aproximadamente 90.000 rechazos de una sola vez.
- Conservar los dos botones independientes para borrar aceptados y rechazados, ejecutando el borrado directamente en el almacenamiento persistente.
- No cambiar las reglas de generación, comparación por columna, ni la importación PDF/CSV como parte de esta optimización.

Estado real al actualizar estas notas:
- La implementación SQLite y la migración todavía NO están hechas.
- La paginación del historial todavía NO está implementada.
- La compilación exitosa del commit anterior solo verifica los botones de borrado; no significa que la optimización SQLite esté lista.
- No afirmar que la optimización está terminada hasta revisar el código, migrar de forma segura, probar consultas y borrados, y compilar el APK.

Tareas:
- [ ] Diseñar e implementar almacenamiento SQLite para aceptados y rechazados.
- [ ] Migrar de forma transaccional el archivo `generation_log.json` y las combinaciones aceptadas existentes; no eliminar el origen hasta verificar la migración completa.
- [ ] Cambiar la generación para consultar duplicados/rechazos en SQLite sin cargar todos los registros en memoria.
- [ ] Guardar únicamente los nuevos registros, sin reescribir el historial completo.
- [ ] Implementar paginación del historial (50 registros por página o carga incremental equivalente).
- [ ] Mantener los botones de borrado separados y confirmar que cada uno borra exclusivamente su categoría y actualiza su protección correspondiente.
- [ ] Añadir pruebas de migración, consulta, paginación y borrado selectivo.
- [ ] Ejecutar las pruebas y confirmar una nueva compilación APK exitosa.
- [ ] Instalar y probar la nueva versión en el teléfono antes de marcar la tarea como terminada.

Última actualización de este documento: 2026-11-09 15:46:16 UTC-04:00.


## 17. Inicio de implementación SQLite (2026-11-09)

Se inició el trabajo aprobado en la sección 16. Cambios escritos en GitHub:
- Se añadió `GenerationDatabase.kt` con SQLiteOpenHelper, tablas para registro de generaciones y combinaciones generadas, índices, consultas de conteo, consulta de rechazos por clave blanca, consultas de conflictos posicionales, inserción transaccional, paginación de 50 por defecto y borrado selectivo.
- Se hizo visible internamente `GenerationRecord` para que la capa SQLite pueda almacenar y devolver registros.

Commits:
- Capa SQLite inicial: `ff1200b2ea4e9216dd8588cd49c9c8f2fc47c895`
- Visibilidad del modelo de registro: `10228eed7f614a7dd6532c8802b57c7e4e050173`

**Estado y limitaciones de este avance:**
- La clase SQLite ya está añadida, pero todavía no está conectada al flujo de inicio, generación ni pantallas de la aplicación.
- La migración desde JSON/SharedPreferences todavía no está integrada ni verificada.
- La interfaz aún mantiene las listas completas en memoria; la paginación todavía no está conectada.
- No se ha confirmado compilación ni pruebas de esta implementación. No considerar completada la optimización ni descargar/instalar este estado como versión final.

Tareas inmediatas:
- [ ] Integrar migración transaccional de los datos heredados y conservar los archivos originales hasta verificarla.
- [ ] Cambiar la generación para consultar SQLite sin cargar listas de aceptados/rechazados.
- [ ] Conectar pantallas a conteos y páginas de 50 registros.
- [ ] Conectar borrado selectivo y escrituras incrementales a SQLite.
- [ ] Añadir pruebas y confirmar compilación Android.

Actualización: 2026-11-09 15:59:17 UTC-04:00.


## 18. Revisión de estado y continuación de la integración SQLite (2026-11-09)

El usuario pidió actualizar este documento y continuar inmediatamente con el siguiente paso del trabajo.

Revisión del código realizada:
- GenerationDatabase.kt existe y contiene el esquema SQLite, consultas básicas, inserciones, conteos, paginación y borrado selectivo.
- MainActivity.kt sigue leyendo generated desde SharedPreferences y generation_log.json completo a memoria.
- SimulatorScreen mantiene generated y generationLog como listas completas en estado Compose.
- La generación sigue recibiendo las listas completas; todavía no consulta SQLite.
- Los botones de borrado siguen modificando las listas en memoria y guardando el JSON heredado.
- Por tanto, el almacenamiento SQLite todavía no está integrado funcionalmente en la aplicación.

**Siguiente paso en curso: migración segura de los datos heredados a SQLite.**
- Identificar y migrar los registros de generation_log.json y las combinaciones de generated en SharedPreferences.
- Ejecutar la migración dentro de una transacción y verificar que los datos estén persistidos antes de marcarla como completada.
- Conservar los datos heredados hasta verificar la migración; no borrar ni sobrescribir los archivos originales durante este paso.
- Evitar introducir cambios en las reglas de generación, comparación posicional o importación PDF/CSV.

Estado:
- [x] ~~Revisar el estado real del código y documentar qué partes siguen desconectadas.~~ — Completada: 2026-11-09 16:34:26 UTC-04:00
- [ ] Integrar y verificar la migración segura a SQLite.
- [ ] Conectar generación, historial paginado y borrado selectivo a SQLite.
- [ ] Añadir/verificar pruebas y compilar una nueva APK.

Actualización: 2026-11-09 16:34:26 UTC-04:00.


## 19. Primera fase de migración SQLite implementada (2026-11-09)

Cambios realizados en esta fase:
- La aplicación ahora inicia la migración antes de abrir el simulador.
- Los registros de generation_log.json se leen con JsonReader, uno por uno, sin construir primero una lista de todos los rechazos.
- Las combinaciones generadas de SharedPreferences se recorren desde el JSON y se insertan en SQLite una por una.
- La importación se ejecuta dentro de una transacción. Si el JSON está dañado, contiene registros inválidos o falla una inserción/verificación, la transacción se revierte y los datos heredados permanecen intactos.
- Se comprueban los totales de aceptados, rechazados y combinaciones insertadas antes de marcar la migración como completada.
- La aplicación muestra un error de migración en vez de continuar silenciosamente con datos incompletos.

Commits de implementación:
- Inicio de migración al arrancar: 0f54941805c931d4f3c773d70305513dfc9b0a8d
- Verificación transaccional y lectura progresiva: 2bae35fb8cec115068acf3df62f261313afa07b6
- Validación de fin de archivo JSON: 43721a9cc5baaf1900ad3b0a4245b247150966ed

**Limitación importante:** la pantalla todavía carga los historiales completos desde los archivos heredados después de la migración. La generación y los historiales aún no consultan SQLite directamente. Esta APK intermedia no debe considerarse la versión final ni instalarse como solución terminada.

Verificación:
- [x] ~~Implementar lectura progresiva de los datos heredados y migración transaccional con verificación.~~ — Código escrito en GitHub: 2026-11-09 16:36:04 UTC-04:00
- [x] ~~Pruebas automatizadas para el commit 2bae35fb8cec115068acf3df62f261313afa07b6.~~ — GitHub Actions Tests #104 y #105: success.
- [x] ~~Confirmar pruebas automatizadas para el commit 43721a9cc5baaf1900ad3b0a4245b247150966ed.~~ — GitHub Actions Tests #106 y #107: success, 2026-11-09 16:36:23 UTC-04:00\n- [ ] Confirmar compilación APK para el commit 43721a9cc5baaf1900ad3b0a4245b247150966ed.
- [ ] Conectar el generador a consultas SQLite y escrituras incrementales.\n- [ ] Conectar pantallas a páginas de 50 registros, conteos y borrado selectivo.

Actualización: 2026-11-09 16:36:04 UTC-04:00.


## 20. Siguiente fase: conectar el generador a SQLite

Una vez implementada la migración segura, el siguiente trabajo aprobado es eliminar del generador la dependencia de las listas completas de combinaciones generadas y rechazos.

Criterios obligatorios:
- Consultar en SQLite los duplicados exactos de las cinco bolas blancas; Mega Ball no participa en la comparación.
- Consultar en SQLite los rechazos previos por clave blanca y saltarlos sin volver a crear un registro de rechazo.
- Consultar conflictos posicionales en combinaciones generadas almacenadas en SQLite y conservar la comparación con el historial oficial importado.
- Insertar los nuevos registros por lotes pequeños y guardar la combinación aceptada junto con su registro de forma transaccional.
- No modificar la regla de coincidencia posicional ni la importación PDF/CSV.
- No mantener listas completas de aceptados/rechazados en memoria.

Estado:
- [ ] Conectar generación a consultas SQLite.
- [ ] Guardar registros de generación por lotes y la combinación aceptada en una transacción.
- [ ] Conectar pantallas a paginación de 50 y conteos SQLite.
- [ ] Conectar borrado independiente a SQLite.
- [ ] Confirmar pruebas y APK final.

Actualización: 2026-11-09 16:36:23 UTC-04:00.


## 21. Generador y pantallas conectados a SQLite (2026-11-09)

Avance de la siguiente fase:
- El generador ahora consulta SQLite para evitar combinaciones blancas ya aceptadas y para saltar combinaciones previamente rechazadas sin volver a registrarlas.
- La comparación de duplicados usa exclusivamente las cinco bolas blancas. La Mega Ball no participa.
- Los conflictos posicionales se consultan tanto en el historial oficial importado como en las combinaciones aceptadas guardadas en SQLite.
- Los rechazos se insertan en lotes de 250 registros para limitar el uso de memoria.
- La combinación aceptada y su registro se guardan en la misma transacción.
- La pantalla principal y la pantalla de rechazos cargan 50 registros por página; el usuario puede cargar más cuando lo necesite.
- Los conteos de aceptados, rechazados y combinaciones protegidas se consultan directamente en SQLite.
- Los botones de borrado independiente ahora borran directamente en SQLite y actualizan los conteos/páginas correspondientes.
- La generación y las operaciones de base de datos se ejecutan fuera del hilo de interfaz.

Cambios relevantes:
- Conexión de generación y pantallas a SQLite: commits 337a0b3a2c168bc114bfcccfd92666e3026ddf09 y 53479fbb04231033f58d5092ff3fed2085fc6283.
- Consultas de aceptados y guardado transaccional por lotes: commit 6c362e4e8f9c71532ac9087ae5abbbdf8c3fec7f.
- Comparación explícita de duplicados solo por bolas blancas: commit 80ce5892391c9ccbf600c55813064617a8e27dac.
- Pruebas añadidas para confirmar que Mega Ball no cambia la detección de duplicados y que las cinco blancas deben coincidir en sus posiciones.

Verificación:
- [x] ~~Pruebas automatizadas del commit 4014104cba816ed3569b89fcf3302a73fd7a66f0.~~ — GitHub Actions Tests #116 y #117: success.
- [ ] Confirmar pruebas automatizadas del commit más reciente 80ce5892391c9ccbf600c55813064617a8e27dac.
- [ ] Confirmar compilación APK del commit más reciente.
- [ ] Revisar resultados de uso y posibles errores en el teléfono antes de considerar final la integración.

Limitación pendiente:
- El historial oficial importado continúa en memoria, como antes. Los historiales generados/aceptados/rechazados ya usan páginas SQLite y no se cargan completos al iniciar.

Actualización: 2026-11-09 16:38:00 UTC-04:00.


## 22. Verificación de la integración SQLite en curso (2026-11-09)

- Se añadió una pantalla de espera mientras se verifica y migra el historial al iniciar, para evitar mostrar una pantalla vacía durante la operación.
- Se deshabilitan la importación y el borrado de aceptados/rechazados mientras el generador está trabajando, evitando operaciones simultáneas conflictivas.
- Se añadieron pruebas unitarias que confirman que dos combinaciones con las mismas cinco bolas blancas son duplicadas aunque tengan Mega Ball diferente, y que cambiar una bola blanca deja de ser duplicado.
- GitHub Actions Tests #124 para el commit d440ddb40a6f085ef8a92a47c234f0b4fa423d93 terminó con success.
- La compilación Android APK del mismo commit continúa en ejecución; no marcar como terminada ni entregar la APK hasta confirmar el resultado.

Estado:
- [x] ~~Conectar el generador a consultas SQLite y escrituras transaccionales por lotes.~~ — Código integrado.
- [x] ~~Conectar los historiales aceptados/rechazados a páginas de 50 registros y conteos SQLite.~~ — Código integrado.
- [x] ~~Conectar los botones de borrado independiente a SQLite.~~ — Código integrado.
- [x] ~~Añadir pruebas para la regla de duplicado sin Mega Ball.~~ — GitHub Actions Tests #124: success, 2026-11-09 16:39:07 UTC-04:00.
- [ ] Confirmar la compilación APK del commit d440ddb40a6f085ef8a92a47c234f0b4fa423d93.
- [ ] Verificar en el teléfono la migración de historiales grandes, la generación y los dos botones de borrado.

Actualización: 2026-11-09 16:39:07 UTC-04:00.
