import streamlit as st
from lottery import Draw, generate, load_csv, load_pdf, validate_candidate, export_csv

st.set_page_config(page_title="Mega Millions · Simulador", page_icon="🎱", layout="centered")
st.title("🎱 Simulador Mega Millions")
st.caption("Generador experimental con aleatoriedad criptográfica y filtros históricos.")

st.info("Esto no predice los sorteos ni puede garantizar que nadie gane. Cada combinación legal tiene la misma probabilidad en un sorteo oficial; filtrar por historial cambia el conjunto de combinaciones que este simulador acepta.")

st.subheader("1. Carga el historial")
st.write("Sube un CSV con columnas `date, white1, white2, white3, white4, white5, mega_ball`, o prueba la importación heurística de un PDF.")
upload = st.file_uploader("Historial CSV o PDF", type=["csv", "pdf"])
history = []
if upload:
    try:
        if upload.name.lower().endswith(".csv"):
            history = load_csv(upload.getvalue().decode("utf-8-sig"))
        else:
            history = load_pdf(upload.getvalue())
        st.success(f"{len(history)} sorteos importados.")
        st.download_button("Descargar historial normalizado (CSV)", export_csv(history), "historial_normalizado.csv", "text/csv")
        with st.expander("Ver primeras filas"):
            st.dataframe([{"Fecha":d.date,"Blancas":" · ".join(map(str,d.white)),"Mega Ball":d.mega} for d in history[:20]], use_container_width=True)
    except Exception as exc:
        st.error(str(exc))

st.subheader("2. Restricciones")
max_overlap = st.slider("Máximo de valores numéricos compartidos con cada fila histórica", 0, 6, 2)
st.caption("Se comparan los seis valores como conjunto, sin distinguir si el número histórico era blanco o Mega Ball. Un valor repetido dentro de una misma fila cuenta una sola vez.")
st.write("Reglas actuales: 5 blancas distintas de 1–70, ordenadas; Mega Ball independiente de 1–24. Puede coincidir numéricamente con una blanca.")

st.subheader("3. Genera")
if st.button("Generar combinación", type="primary", use_container_width=True, disabled=not bool(history)):
    try:
        result = generate(history, max_overlap)
        st.success("Combinación aceptada por los filtros del simulador.")
        cols = st.columns(6)
        for i, number in enumerate(result.white):
            cols[i].metric(f"Blanca {i+1}", number)
        cols[5].metric("Mega Ball", result.mega)
        st.caption("La salida es aleatoria; no tiene ventaja predictiva sobre Quick Pick ni sobre otra combinación legal.")
    except Exception as exc:
        st.error(str(exc))

st.subheader("4. Comprueba una combinación")
a, b, c, d, e, m = st.columns(6)
white = [a.number_input("B1", 1, 70, 4), b.number_input("B2", 1, 70, 24), c.number_input("B3", 1, 70, 31), d.number_input("B4", 1, 70, 62), e.number_input("B5", 1, 70, 70)]
mega = m.number_input("Mega", 1, 24, 3)
if st.button("Validar combinación", use_container_width=True):
    ok, message = validate_candidate(white, int(mega), history, max_overlap)
    (st.success if ok else st.error)(message)
