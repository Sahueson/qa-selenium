from datetime import date, timedelta


def in_days(days: int) -> str:
    """Fecha local YYYY-MM-DD dentro de `days` días (como <input type="date">)."""
    return (date.today() + timedelta(days=days)).isoformat()
