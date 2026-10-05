# Guide data format (0.1.7.33 manual, GuideME)

Write ONE JSON file: an object keyed by block id (path without the `mio_icif:` namespace, exactly as listed).

```json
{
  "producer/block_powder_elc": {
    "kind": "machine",
    "function": {"en": ["..."], "zh": ["..."], "ja": ["..."]},
    "usage":    {"en": ["..."], "zh": ["..."], "ja": ["..."]},
    "notes":    {"en": ["..."], "zh": ["..."], "ja": ["..."]}
  }
}
```

* `kind`: one of `machine`, `generator`, `storage`, `cable`, `transformer`, `multiblock`, `utility`, `reactor`.
* `function`: 1-3 lines. What the block does, its inputs and outputs. Descriptive sentences.
* `usage`: 2-6 lines. One concrete use case written as a numbered procedure WITHOUT the numbers
  (the generator adds them). Each line = ONE instruction in the imperative ("Connect ...", "Put ...").
* `notes` (optional, 0-3 lines): warnings or limits. Start a safety line with "WARNING:" / "警告：" / "警告：".
* Every list must have the SAME number of lines in en, zh and ja (line i is the translation of line i).
* Do NOT write power numbers (EU/t, capacity, voltage limits, ticks): the generator inserts a
  measured specification table. You may name a voltage tier (LV/MV/HV/EV/IV) or say "the machine explodes on overvoltage".
* Read the Java class of each block (src/main/java/com/miophas/singularity_iteration/common/...) and
  the en_us lang tooltips to get the behaviour right. Do not guess. If a block is only a structural part, say so.

## Writing rules (ASD-STE100 / asd-ste100-skill-zh)

General: active voice, one action per sentence, one meaning per word, one word per meaning, no marketing
words, no filler, no vague quantities, no semicolons, no contractions.

| | instruction (usage) | description (function/notes) |
|---|---|---|
| en | max 20 words | max 25 words |
| zh | max 25 characters | max 40 characters |
| ja | max 40 characters | max 60 characters |

English: imperative verbs for steps. Use "the machine", not "it", at the start of a new line. No "-ing" nouns
("charging" -> "to charge"). No "please", "simply", "just", "etc.", "various", "several", "in order to", "utilize".
Avoid passive ("is used", "are produced"); allowed stative forms: "is connected/full/empty/installed/required/enabled".

Chinese: no 被, no 进行/加以/予以 + noun, no 强大/无缝/赋能/闭环/完美, no 需要注意的是/在一定程度上,
no 若干/一些/相关/等等, no ；. Use 。 at the end of every sentence.

Japanese: 常体（〜する／〜だ）. No passive 〜される／〜られる for actions (write the actor: 「機械が〜する」).
No など／等／いくつかの, no ；. End every sentence with 。.

Glossary (use exactly these terms):

| en | zh | ja |
|---|---|---|
| EU, EU/t | EU, EU/t | EU, EU/t |
| voltage tier (LV, MV, HV, EV, IV, LuV) | 电压等级 | 電圧Tier |
| packet | 电力包 | パケット |
| cable | 导线 | ケーブル |
| machine | 机器 | 機械 |
| storage block | 储电方块 | 蓄電ブロック |
| generator | 发电机 | 発電機 |
| slot | 槽位 | スロット |
| upgrade | 升级 | アップグレード |
| battery | 电池 | バッテリー |
| overvoltage | 过压 | 過電圧 |
| GUI | 界面 | GUI |
| wrench | 扳手 | レンチ |

Check your file with: `python3 tools/ste_lint.py tools/guide_data/<your file>.json` and fix every problem
until it prints "STE lint: 0 problem(s)". Do not edit any other file.
