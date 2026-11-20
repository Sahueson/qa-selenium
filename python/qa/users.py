"""Usuarios QA: la fuente única es users.json de ivan-qa-lab-shared (el mismo que usa el reset)."""
import json
from dataclasses import dataclass
from functools import cache

from qa.env import shared_dir


@dataclass(frozen=True)
class QaUser:
    key: str
    email: str
    nickname: str


@cache
def _users() -> dict[str, QaUser]:
    data = json.loads((shared_dir() / "reset-staging" / "users.json").read_text(encoding="utf-8"))
    return {u["key"]: QaUser(u["key"], u["email"], u["nickname"]) for u in data}


def qa_user(key: str) -> QaUser:
    try:
        return _users()[key]
    except KeyError:
        raise KeyError(f"Usuario QA desconocido: {key}") from None
