#!/usr/bin/env python3
"""提交内容机械检查：TEMP-PROBE 探针 / 术语避免词 / lang 健康度。

针对两类历史上真实入库过的问题（2026-09 调试探针随 commit 入库、2026-10
审查抓出 README/lang 里的避免词）做的确定性检查：

1. TEMP-PROBE —— 调试探针必须带 TEMP-PROBE 标记，且带标记的行不允许出现在
   任何被提交的 .java 里。只查标记本身：探针日志文案千变万化，标记是唯一可靠信号。
2. 避免词 —— 扫 README.md 与 lang 值。词表的判定口径唯一源是仓库根 CONTEXT.md
   的 _Avoid_ 行，本文件只固化其可机械执行的子集；判断性词条（鸡、监守者、warden、
   ready、有马、上马、"红色马铠"里的红色马、泛词"点"）不进机械检查，交给审查。
   大狗 用负向前瞻 大狗(?!叫)，避免把规范词 大狗叫 本身误报。
3. lang 健康度 —— zh_cn / en_us 必须可解析且键位完全对齐（lang JSON 写坏语法
   gradle build 不报错，是静默坏包）。

CONTEXT.md 是本地不入库文件（隐私边界）；但本脚本不含任何词表之外的信息，
可以安全入库，CI 上三项检查全跑。

用法：
  python scripts/check_terminology.py            # 全量（CI / 手动）
  python scripts/check_terminology.py --staged   # 只查暂存内容（pre-commit 用；
                                                 #   lang 健康度仍读磁盘，CI 会兜底）
"""
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
LANG_DIR = "src/main/resources/assets/big-dog-adventure/lang"
README = "README.md"

AVOID_PATTERNS = [
    ("合唱", "合奏"),
    ("齐唱", "合奏"),
    (r"大狗(?!叫)", "大狗叫"),
    ("骑乘", "骑马"),
    ("叮咚鸡", "鸡法师"),
    ("音波狗", "大狗叫"),
    ("守位者", "坚守者"),
    ("口罩", "护符"),
    ("面罩", "护符"),
    ("面具", "护符"),
    ("棉花", "元素作物/元素花"),
    ("棉签", "元素核心"),
    ("草药汤", "魔力药"),
    ("草药庇护", "魔力庇护"),
    ("红马", "魔化马"),
    ("绿马", "升华马"),
    ("双倍马", "魔化马"),
]

LANG_DIR_PREFIX = LANG_DIR + "/"


def staged_names():
    out = subprocess.run(
        ["git", "diff", "--cached", "--name-only", "--diff-filter=d"],
        cwd=ROOT, capture_output=True, text=True, check=True,
    ).stdout
    return [n.replace("\\", "/") for n in out.splitlines() if n.strip()]


def index_content(name):
    r = subprocess.run(
        ["git", "show", ":" + name],
        cwd=ROOT, capture_output=True, text=True, encoding="utf-8", errors="replace",
    )
    if r.returncode == 0:
        return r.stdout
    p = ROOT / name
    return p.read_text(encoding="utf-8") if p.is_file() else ""


def scan_avoid_text(name, text, problems):
    for pat, canonical in AVOID_PATTERNS:
        for m in re.finditer(pat, text):
            line = text.count("\n", 0, m.start()) + 1
            problems.append(f"{name}:{line}  出现「{m.group(0)}」，规范词「{canonical}」")


def scan_avoid_lang(name, data, problems):
    for key, value in data.items():
        for pat, canonical in AVOID_PATTERNS:
            for m in re.finditer(pat, value):
                problems.append(f"{name} [{key}]  值里出现「{m.group(0)}」，规范词「{canonical}」")


def check_temp_probe(problems):
    if "--staged" in sys.argv[1:]:
        files = [(n, index_content(n)) for n in staged_names() if n.endswith(".java")]
    else:
        files = [
            (p.relative_to(ROOT).as_posix(), p.read_text(encoding="utf-8", errors="replace"))
            for p in (ROOT / "src").rglob("*.java")
        ]
    for name, text in files:
        for i, line in enumerate(text.splitlines(), 1):
            if "TEMP-PROBE" in line:
                problems.append(f"{name}:{i}  TEMP-PROBE 调试探针（要么删掉，要么它不该带这个标记）")
    return len(files)


def check_avoid_words(problems):
    n_files = 0
    if "--staged" in sys.argv[1:]:
        names = [n for n in staged_names() if n == README or n.startswith(LANG_DIR_PREFIX)]
        for n in names:
            n_files += 1
            text = index_content(n)
            if n.endswith(".json"):
                try:
                    scan_avoid_lang(n, json.loads(text), problems)
                except json.JSONDecodeError:
                    pass  # 语法问题由 lang 健康度报告
            else:
                scan_avoid_text(n, text, problems)
    else:
        n_files = 1
        scan_avoid_text(README, (ROOT / README).read_text(encoding="utf-8"), problems)
        for p in sorted((ROOT / LANG_DIR).glob("*.json")):
            n_files += 1
            scan_avoid_lang(p.relative_to(ROOT).as_posix(), json.loads(p.read_text(encoding="utf-8")), problems)
    return n_files


def check_lang_health(problems):
    lang_dir = ROOT / LANG_DIR
    parsed = {}
    for p in sorted(lang_dir.glob("*.json")):
        try:
            parsed[p.name] = json.loads(p.read_text(encoding="utf-8"))
        except json.JSONDecodeError as e:
            problems.append(f"lang/{p.name}  JSON 解析失败：{e}")
    names = sorted(parsed)
    for a, b in zip(names, names[1:]):
        only_a = sorted(set(parsed[a]) - set(parsed[b]))
        only_b = sorted(set(parsed[b]) - set(parsed[a]))
        if only_a:
            problems.append(f"lang/{b}  缺键：{', '.join(only_a)}")
        if only_b:
            problems.append(f"lang/{a}  缺键：{', '.join(only_b)}")
    return len(parsed)


def main():
    try:
        sys.stdout.reconfigure(encoding="utf-8", errors="replace")
    except AttributeError:
        pass
    problems = []
    n_java = check_temp_probe(problems)
    n_avoid = check_avoid_words(problems)
    n_lang = check_lang_health(problems)
    if problems:
        for p in problems:
            print("✗ " + p)
        return 1
    mode = "staged" if "--staged" in sys.argv[1:] else "全量"
    print(f"✓ [{mode}] TEMP-PROBE：{n_java} 个 .java 干净；避免词：{n_avoid} 个文件干净；lang：{n_lang} 个文件可解析且键位对齐")
    return 0


if __name__ == "__main__":
    sys.exit(main())
