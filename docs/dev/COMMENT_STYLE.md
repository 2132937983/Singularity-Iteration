# Code comment style (EN / ZH / JA) — 0.1.8.1

## English

### Rules

1. English is the source text. It follows ASD-STE100 (Simplified Technical English):
   * Use the active voice.
   * Write one instruction or one fact in each sentence.
   * Keep a sentence to 20 words for an instruction and 25 words for a description.
   * Use one word for one meaning (for example "room" for free capacity, "credit" for delivered energy).
   * Do not use vague words ("simply", "basically", "etc.", "and/or").
2. Chinese (ZH) and Japanese (JA) translate the same content. They do not add or remove facts.
3. Code identifiers, `{@code ...}`, `{@link ...}`, units (EU, EU/t, tick, mB, HU, KU) and numbers stay the same in all three languages.
4. Javadoc tags (`@param`, `@return`, `@throws`, `@deprecated`) are in English only.

### Formats

Type and method comments:

```java
/**
 * <p><b>EN:</b> Moves pending drops into storage. Returns true when the list is empty.
 * <p><b>ZH:</b> 把待存掉落物移入存储。列表为空时返回 true。
 * <p><b>JA:</b> 保留ドロップを格納庫へ移す。リストが空なら true を返す。
 *
 * @return true when no drop waits
 */
```

Short field or accessor comments (one line):

```java
/** EN: Ticks between two crop cycles. ZH: 两个作物周期之间的 tick 数。JA: 作物サイクル間の tick 数。 */
```

Block comments in a method:

```java
// EN: Cap by the remaining budget, so a rounding error cannot overspend.
// ZH: 以剩余预算为上限，舍入误差不会超支。
// JA: 残り予算で上限をかけるので、丸め誤差で使い過ぎない。
```

### Scope and check

* `docs/dev/trilingual_manifest.txt` lists the files in scope: the public API entry points (every Javadoc block), the complex algorithms and the main classes (the type comment).
* `tools/doc_lint_181.py <repo root>` checks the manifest. Errors: a missing language, a wrong order (EN, ZH, JA), a ZH or JA part without CJK text, a `// EN:` comment without `// ZH:` and `// JA:`. Warnings: an English sentence of more than 25 words, a word from the non-STE list.
* Add a file to the manifest when you write or change a public API type or an algorithm class.

## 中文

### 规则

1. 英文为原文，遵循 ASD-STE100（简明技术英语）：使用主动语态；每句只写一条指令或一个事实；指令句不超过 20 词，说明句不超过 25 词；一个词只表示一个意思（例如 "room" 表示剩余容量，"credit" 表示输送的能量）；不使用含糊的词（"simply"、"basically"、"etc."、"and/or"）。
2. 中文（ZH）与日文（JA）翻译相同的内容，不增加也不删减事实。
3. 代码标识符、`{@code ...}`、`{@link ...}`、单位（EU、EU/t、tick、mB、HU、KU）与数字在三种语言中保持一致。
4. Javadoc 标签（`@param`、`@return`、`@throws`、`@deprecated`）只用英文。

### 格式

见上方英文部分的三个示例：类型与方法注释使用 `<p><b>EN:</b>` / `<p><b>ZH:</b>` / `<p><b>JA:</b>` 三段；字段等短注释写在一行内；方法内的块注释使用连续的 `// EN:`、`// ZH:`、`// JA:` 行。

### 范围与检查

* `docs/dev/trilingual_manifest.txt` 列出适用范围：公共 API 入口（每个 Javadoc 块）、复杂算法与主要类（类型注释）。
* `tools/doc_lint_181.py <仓库根目录>` 检查清单中的文件。错误：缺少某种语言、顺序不是 EN、ZH、JA、ZH 或 JA 部分没有中日文字、`// EN:` 后面没有 `// ZH:` 与 `// JA:`。警告：英文句子超过 25 词、使用了非 STE 词汇。
* 编写或修改公共 API 类型或算法类时，请把文件加入清单。

## 日本語

### 規則

1. 英語を原文とし、ASD-STE100（簡易技術英語）に従う：能動態を使う。1 文に 1 つの指示または事実だけを書く。指示文は 20 語、説明文は 25 語以内にする。1 つの語は 1 つの意味だけに使う（例："room" は空き容量、"credit" は送られたエネルギー）。あいまいな語（"simply"、"basically"、"etc."、"and/or"）を使わない。
2. 中国語（ZH）と日本語（JA）は同じ内容を訳す。事実を加えたり減らしたりしない。
3. コードの識別子、`{@code ...}`、`{@link ...}`、単位（EU、EU/t、tick、mB、HU、KU）、数値は 3 言語で同じにする。
4. Javadoc タグ（`@param`、`@return`、`@throws`、`@deprecated`）は英語だけで書く。

### 書式

上の英語の節の 3 つの例を参照：型とメソッドのコメントは `<p><b>EN:</b>` / `<p><b>ZH:</b>` / `<p><b>JA:</b>` の 3 段落。フィールドなどの短いコメントは 1 行。メソッド内のブロックコメントは `// EN:`、`// ZH:`、`// JA:` を続けて書く。

### 対象と検査

* `docs/dev/trilingual_manifest.txt` に対象ファイルを列挙する：公開 API の入口（すべての Javadoc ブロック）、複雑なアルゴリズムと主要クラス（型のコメント）。
* `tools/doc_lint_181.py <リポジトリのルート>` が一覧のファイルを検査する。エラー：言語の欠落、順序が EN・ZH・JA でない、ZH・JA 部分に漢字・かながない、`// EN:` の後に `// ZH:` と `// JA:` がない。警告：25 語を超える英文、非 STE 語の使用。
* 公開 API の型やアルゴリズムのクラスを書く・変えるときは、ファイルを一覧に追加する。
