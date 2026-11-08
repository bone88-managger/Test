# Simulador Mega Millions

Aplicación educativa para generar combinaciones legales de Mega Millions y filtrarlas contra un historial de sorteos.

## Reglas actuales implementadas

- Cinco bolas blancas distintas del 1 al 70, mostradas en orden ascendente.
- Una Mega Ball del 1 al 24, elegida independientemente. Puede coincidir numéricamente con una bola blanca.
- Generación con `secrets.SystemRandom`, usando la fuente criptográfica del sistema operativo.
- Se rechaza el candidato si comparte más de N valores numéricos distintos con **cualquier** fila histórica. Por defecto N=2.
- Se comparan los seis valores como conjunto numérico, sin distinguir si el número histórico estaba en una posición blanca o en Mega Ball. Un valor repetido en una misma fila cuenta una sola vez.
- El historial admite valores antiguos hasta 99, sin aplicarles los rangos actuales. Así se pueden conservar sorteos de eras anteriores.

## Ejecutar localmente

Requiere Python 3.10 o posterior.

```bash
python -m venv .venv
source .venv/bin/activate  # Windows: .venv\\Scripts\\activate
pip install -r requirements.txt
streamlit run app.py
```

## Formato CSV

El CSV debe incluir estas columnas (puede incluir otras):

```csv
date,white1,white2,white3,white4,white5,mega_ball
2025-04-08,10,20,30,40,50,7
```

La aplicación acepta CSV y PDF. La importación PDF es heurística: intenta leer cada línea como fecha seguida de cinco bolas blancas y la Mega Ball. **Revisa el CSV normalizado que descarga la aplicación** antes de confiar en los resultados. Si el PDF tiene columnas o saltos de línea complejos, convierte primero los datos a CSV.

## Pruebas

```bash
pytest -q
```

## Límites importantes

Este programa no predice el futuro, no garantiza que nadie gane ni vuelve menos probable que salga una combinación. Los sorteos oficiales son independientes. Los filtros históricos solo restringen las combinaciones que este programa devuelve; no alteran el mecanismo oficial ni crean una ventaja matemática. Si el filtro es demasiado estricto, el generador se detiene tras un límite de intentos y muestra un error.

Reglas oficiales: [Mega Millions – How to Play](https://www.megamillions.com/how-to-play).
