# Party of Two — the lore files

Party of Two («Отряд из двух») is an offline Android date app for couples in an illuminated-manuscript
fantasy look. The couple is a party of two heroes; a date is a quest out in the real world; the app is
the game master. Everything the app says comes from tab-separated files in `app/src/main/assets/lore/`.

## Format

UTF-8, one entry per line, exactly **7 tab-separated columns**, no header, no empty lines, no tabs or
newlines inside a field:

```
id	kind	school	rank	flags	en	ru
```

- `id` — stable and unique across all files (`q.road.01`, `s.001`, …). Never reuse or renumber.
- `kind` — one letter, see below.
- `school` — for quests one of `road hearth craft arts wild lore daring tender`; for campaigns the
  campaign key (`c1`, `c2`, `c3`); otherwise `*`.
- `rank` — quests: `C` copper (easy, cheap, an hour or two, often at home or nearby), `S` silver (an
  evening out or a bigger evening in), `G` gold (a whole day or something memorable that takes
  planning). Campaign chapters: the chapter number `1`–`7`; campaign headers `0`. Otherwise `*`.
- `flags` — comma-separated, or `-` for none. Quests: `h` can be done at home, `o` outdoors, `n` best
  in the evening, `m` costs some money. Fates: `+` or `-` (see below).
- `en`, `ru` — the text; parts are separated by `|` (no spaces needed around it, they are trimmed).

## Kinds

| kind | what | text parts |
|---|---|---|
| `Q` | a quest notice on the tavern board — a real date | `Title\|in which the party …\|story\|task\|task\|task[\|task]` |
| `S` | a secret objective, dealt face-down to one hero at the start of a quest | one sentence |
| `E` | a wandering encounter, drawn during a quest | `Name\|what to do now` |
| `T` | a question by the campfire (a rest during the quest) | one question |
| `R` | the raven's errand: a tiny thing for the day, between dates | one sentence |
| `F` | a fate of the d20: `+` flag = a natural 20 (boon), `-` = a natural 1 (mishap) | `Name\|what happens` |
| `K` | a campaign header | `Campaign name\|premise paragraph` |
| `C` | a campaign chapter (a date too) | `Title\|in which the party …\|prelude\|task\|task\|task\|epilogue` |

### Voice

- Fantasy-flavoured framing of **real, ordinary, doable** things: a walk becomes a "patrol of the
  eastern marches", a cheap café becomes a tavern. Warm, witty, a bit tongue-in-cheek, never cringe.
  The real activity must always be perfectly clear from the text.
- Works anywhere in the world (no specific cities, shops or brands), mostly cheap or free, legal,
  safe, kind. Gentle romance is fine; nothing sexual.
- **Never** ask the heroes to rate, score, describe or list traits of each other ("what is your
  partner's weakness"). Questions are about one's own memories, dreams, imagination, and the two of
  you as a party.
- Tasks are concrete and checkable ("buy each other a drink under 5 coins without showing it" — not
  "have fun").
- Party-addressed text (quests, encounters, campfire questions, ravens, fates, campaigns): EN "you";
  RU plural «вы». RU must never use «(а)»-style gender patches.
- Secret objectives are read by one hero alone: EN "you" + "your companion" for the other; RU «ты».
  For gendered Russian forms use marks: `{masc|fem}` picks by **the companion's** gender
  (`{своего спутника|свою спутницу}`), `[masc|fem]` by **the reader's own** gender (`[сделал|сделала]`).
  Avoid them where a neutral phrasing reads naturally.
- In `in which the party …` parts write a lowercase clause that completes "Chapter VII, in which the
  party …" / «Глава VII, в которой отряд …» (RU in past tense: «…, в которой отряд заблудился в
  тумане» — so the RU part starts with «в которой отряд»; the EN part starts with "in which the party").
- Russian typography: «ёлочки», em dash —, no English quotes.
- Money is "coins" / «монеты», never a currency.
