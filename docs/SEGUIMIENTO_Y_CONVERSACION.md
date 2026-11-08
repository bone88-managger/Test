# Mega Millions Simulator — memoria del proyecto y registro de conversación

> Documento de continuidad del proyecto. Leer antes de continuar el trabajo en otro chat.
>
> **Regla de mantenimiento:** no borrar tareas ni decisiones anteriores. Cuando una tarea quede terminada y verificada, tachar su texto con Markdown (\`~~tarea~~\`) y conservarla en su sección. Añadir nuevas tareas sin reemplazar el historial. No marcar como terminada una compilación o prueba hasta comprobar su resultado.

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
- El criterio conversado fue no aceptar una combinación que tenga más de dos coincidencias numéricas con cualquier sorteo del historial.
- La generación debe ser independiente del historial; el historial solo se usa después para filtrar.
- Antes de cambiar cómo se cuentan coincidencias (por ejemplo, si se distingue el tipo de bola o se comparan los seis valores como conjunto), confirmar el criterio con el usuario o respetar exactamente el comportamiento previo del proyecto.

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
- [ ] Solo después de todas las verificaciones, tachar las tareas terminadas con \`~~texto~~\`, conservándolas en esta lista.

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
5. Al completar y verificar una tarea, tacharla con Markdown; no eliminarla ni borrar decisiones anteriores.
6. No agregar funciones nuevas sin explicarlas al usuario y recibir su aprobación.

---
Última actualización: 2026-11-08.
