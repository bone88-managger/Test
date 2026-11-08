"""Motor del simulador Mega Millions. No predice sorteos ni garantiza que nadie gane."""
from __future__ import annotations
from dataclasses import dataclass
from datetime import date
import csv
import io
import re
import secrets
from typing import Iterable

WHITE_MIN, WHITE_MAX, WHITE_COUNT = 1, 70, 5
MEGA_MIN, MEGA_MAX = 1, 24

@dataclass(frozen=True)
class Draw:
    date: str
    white: tuple[int, int, int, int, int]
    mega: int

    def __post_init__(self):
        if len(self.white) != WHITE_COUNT or tuple(sorted(self.white)) != self.white:
            raise ValueError("Se requieren cinco bolas blancas distintas y ordenadas.")
        if len(set(self.white)) != WHITE_COUNT or any(n < WHITE_MIN or n > WHITE_MAX for n in self.white):
            raise ValueError("Las bolas blancas deben ser cinco números distintos entre 1 y 70.")
        if not MEGA_MIN <= self.mega <= MEGA_MAX:
            raise ValueError("La Mega Ball debe estar entre 1 y 24.")

    @property
    def values(self) -> tuple[int, ...]:
        return self.white + (self.mega,)

    @property
    def key(self) -> tuple[tuple[int, ...], int]:
        return self.white, self.mega


def _int(value: str) -> int:
    return int(value.strip())


def load_csv(text: str) -> list[Draw]:
    """Lee CSV con columnas date, white1..white5, mega_ball."""
    reader = csv.DictReader(io.StringIO(text))
    required = {"date", "white1", "white2", "white3", "white4", "white5", "mega_ball"}
    if not reader.fieldnames or not required.issubset(set(reader.fieldnames)):
        raise ValueError("El CSV debe incluir: date, white1, white2, white3, white4, white5, mega_ball.")
    draws = []
    for line, row in enumerate(reader, start=2):
        try:
            whites = tuple(_int(row[f"white{i}"]) for i in range(1, 6))
            draws.append(Draw((row.get("date") or "").strip(), whites, _int(row["mega_ball"])))
        except Exception as exc:
            raise ValueError(f"Fila CSV {line}: {exc}") from exc
    return draws


def load_pdf(pdf_bytes: bytes) -> list[Draw]:
    """Importador heurístico para PDFs con filas tipo fecha + 5 blancas + Mega Ball.
    Verifica los resultados importados; revisa manualmente el CSV exportado.
    """
    try:
        import pdfplumber
    except ImportError as exc:
        raise RuntimeError("Instala pdfplumber para importar PDF: pip install pdfplumber") from exc
    date_re = r"(?:\d{1,2}/\d{1,2}/\d{2,4})"
    number_re = r"(\d{1,2})[\s,]+(\d{1,2})[\s,]+(\d{1,2})[\s,]+(\d{1,2})[\s,]+(\d{1,2})[\s,]+(\d{1,2})"
    pattern = re.compile(rf"({date_re})\s+.{0,80}?{number_re}", re.S)
    found: dict[tuple[str, tuple[int, ...]], Draw] = {}
    with pdfplumber.open(io.BytesIO(pdf_bytes)) as pdf:
        for page in pdf.pages:
            content = page.extract_text() or ""
            # Procesa cada línea para evitar mezclar números de sorteos contiguos.
            for line in content.splitlines():
                match = re.search(rf"^\s*({date_re})\s+(.+)$", line)
                if not match:
                    continue
                nums = [int(x) for x in re.findall(r"\b\d{1,2}\b", match.group(2))]
                if len(nums) < 6:
                    continue
                try:
                    whites = tuple(sorted(nums[:5]))
                    draw = Draw(match.group(1), whites, nums[5])
                    found[(draw.date, draw.values)] = draw
                except ValueError:
                    continue
    if not found:
        raise ValueError("No encontré filas reconocibles. Convierte el PDF a CSV y usa el formato indicado.")
    return list(found.values())


def overlap_count(candidate: Iterable[int], historical: Iterable[int]) -> int:
    """Cuenta valores numéricos compartidos únicos (sin distinguir color/posición)."""
    return len(set(candidate) & set(historical))


def validate_candidate(white: Iterable[int], mega: int, history: Iterable[Draw], max_overlap: int = 2) -> tuple[bool, str]:
    whites = tuple(white)
    if len(whites) != 5 or len(set(whites)) != 5 or tuple(sorted(whites)) != whites:
        return False, "Las cinco blancas deben ser distintas y estar ordenadas."
    if any(n < WHITE_MIN or n > WHITE_MAX for n in whites) or not MEGA_MIN <= mega <= MEGA_MAX:
        return False, "Números fuera de los rangos actuales (blancas 1–70, Mega Ball 1–24)."
    candidate = set(whites + (mega,))
    for draw in history:
        if overlap_count(candidate, draw.values) > max_overlap:
            return False, f"Supera {max_overlap} coincidencias con el sorteo {draw.date}."
    return True, "Candidato válido según las restricciones configuradas."


def generate(history: list[Draw], max_overlap: int = 2, max_attempts: int = 250_000) -> Draw:
    """Genera usando el CSPRNG del sistema y rechazo de candidatos no válidos."""
    existing = {draw.key for draw in history}
    for _ in range(max_attempts):
        white = tuple(sorted(secrets.SystemRandom().sample(range(WHITE_MIN, WHITE_MAX + 1), WHITE_COUNT)))
        mega = secrets.randbelow(MEGA_MAX - MEGA_MIN + 1) + MEGA_MIN
        candidate = Draw("SIMULADO", white, mega)
        if candidate.key in existing:
            continue
        valid, _ = validate_candidate(white, mega, history, max_overlap)
        if valid:
            return candidate
    raise RuntimeError(
        f"No se encontró una combinación válida en {max_attempts:,} intentos. "
        "Puede que las restricciones sean demasiado estrictas para el historial cargado."
    )


def export_csv(draws: Iterable[Draw]) -> str:
    out = io.StringIO()
    writer = csv.writer(out)
    writer.writerow(["date", "white1", "white2", "white3", "white4", "white5", "mega_ball"])
    for d in draws:
        writer.writerow([d.date, *d.white, d.mega])
    return out.getvalue()
