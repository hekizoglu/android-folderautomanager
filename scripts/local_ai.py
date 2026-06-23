# -*- coding: utf-8 -*-
"""
Local AI helper — http://localhost:20128/v1
Kullanım: python scripts/local_ai.py "sorun veya görev"
Opsiyonel: python scripts/local_ai.py "soru" --model gh/claude-haiku-4.5
"""
import sys
import json
import urllib.request
import urllib.error
import os

ENDPOINT = os.getenv("LOCAL_AI_ENDPOINT", "http://localhost:20128/v1")
API_KEY  = os.getenv("LOCAL_AI_KEY",      "sk-9509305a5fdc5787-gxd6x8-f48488f8")
MODEL    = os.getenv("LOCAL_AI_MODEL",    "all99")

def ask(prompt: str, model: str = MODEL, system: str = "") -> str:
    messages = []
    if system:
        messages.append({"role": "system", "content": system})
    messages.append({"role": "user", "content": prompt})

    payload = json.dumps({
        "model": model,
        "messages": messages,
        "max_tokens": 2048,
        "stream": False
    }).encode("utf-8")

    req = urllib.request.Request(
        f"{ENDPOINT}/chat/completions",
        data=payload,
        headers={
            "Authorization": f"Bearer {API_KEY}",
            "Content-Type": "application/json"
        }
    )
    try:
        with urllib.request.urlopen(req, timeout=60) as resp:
            data = json.loads(resp.read().decode("utf-8"))
            msg = data["choices"][0]["message"]
            return msg.get("content") or msg.get("reasoning_content", "")
    except urllib.error.URLError as e:
        return f"HATA: endpoint erişilemiyor — {e}"
    except (KeyError, json.JSONDecodeError) as e:
        return f"HATA: yanıt parse edilemedi — {e}"

if __name__ == "__main__":
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding="utf-8", errors="replace")
    if len(sys.argv) < 2:
        print("Kullanım: python scripts/local_ai.py 'soru' [--model MODEL_ADI]")
        sys.exit(1)

    prompt = sys.argv[1]
    model  = MODEL
    if "--model" in sys.argv:
        idx = sys.argv.index("--model")
        model = sys.argv[idx + 1]

    print(f"[Model: {model}]")
    result = ask(prompt, model=model)
    print(result)
