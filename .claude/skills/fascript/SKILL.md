---
name: fascript
description: Write Fascript (.fst) code for Minecraft server scripting. Use when the user asks to write, create, generate, or help with Fascript scripts, .fst files, or Minecraft server automation scripts using Fascript syntax.
argument-hint: "[description of what the script should do]"
---

# Fascript Code Writer

You are an expert Fascript developer. Fascript is a domain-specific scripting language for Minecraft Paper/Bukkit 1.21.10+ servers. Scripts use the `.fst` extension and are placed in the plugin's data folder.

---

## 1. Type System

Fascript has exactly 5 data types and a null value. Every variable MUST be declared with an explicit type keyword.

| Type | Keyword | Description | Example |
|------|---------|-------------|---------|
| Number | `number` | Double precision float (displayed as integer if whole) | `number x = 42` |
| String | `string` | Unicode text, double or single quotes | `string s = "hello"` |
| Boolean | `boolean` | `true` or `false` | `boolean b = true` |
| List | `list` | Mutable ordered collection, heterogeneous | `list l = [1, "a", true]` |
| Object | `object` | Mutable key-value map (string keys) | `object o = {"x": 10}` |

**Null:** Returned by functions without explicit return, missing arguments, and failed lookups. There is no `null` keyword - it exists only as an internal runtime value.

**Truthiness rules:**
- Falsy: `false`, `0`, `""` (empty string), `[]` (empty list), `{}` (empty object), `null`
- Truthy: everything else

**Type coercion:**
- In arithmetic (`-`, `*`, `/`, `%`): values are converted to numbers. Strings are parsed via `toDouble()`, booleans become `1.0`/`0.0`.
- In `+`: if EITHER operand is a string, both are converted to strings and concatenated. Otherwise, numeric addition.
- Comparison (`<`, `>`, `<=`, `>=`): both sides are converted to numbers.
- Equality (`==`, `!=`): no coercion. Different types are always `!=`.

---

## 2. Variable Declaration & Assignment

### Declaration (MUST include type)
```
[public|private] type variableName = initializer
```

```
number count   = 0
string name    = "BACKGWA"
boolean active = true
list items     = [1, 2, "hello"]
object pos     = {"x": 10, "y": 64, "z": -30}
```

### Scope modifiers
- `private` (default): accessible only within the declaring script
- `public`: accessible from all scripts via the global registry

```
public  string serverName = "My Server"
private number secret     = 42       // same as: number secret = 42
```

### Reassignment (no type keyword)
```
count = 5
name  = "TERIE"
```

### Index assignment (lists and objects)
```
items[0] = 99
pos["x"] = 20
pos["x"] += 5    // compound assignment on index works too
```

---

## 3. Operators

### Arithmetic
| Operator | Description | Example |
|----------|-------------|---------|
| `+` | Add / String concat | `10 + 3` => `13`, `"a" + "b"` => `"ab"` |
| `-` | Subtract | `10 - 3` => `7` |
| `*` | Multiply | `10 * 3` => `30` |
| `/` | Divide | `10 / 4` => `2.5` |
| `%` | Modulo | `10 % 3` => `1` |

### Compound assignment
```
a += 5    // a = a + 5
a -= 2    // a = a - 2
a *= 2    // a = a * 2
a /= 4    // a = a / 4
a %= 3    // a = a % 3
```

### Increment / Decrement
```
a++       // a = a + 1
a--       // a = a - 1
```

### Comparison (returns boolean)
| Operator | Description |
|----------|-------------|
| `==` | Equal (no type coercion, different types are always not equal) |
| `!=` | Not equal |
| `<` | Less than (numeric comparison) |
| `>` | Greater than |
| `<=` | Less than or equal |
| `>=` | Greater than or equal |

### Unary
- `-x` : Negation (converts to number first)

### NOT supported
- `&&`, `||`, `!` : No logical operators. Use nested `if` statements instead.
- `**` : No exponentiation.
- Ternary `? :` : Not supported.

---

## 4. Strings

### Regular strings
```
string s1 = "double quotes"
string s2 = 'single quotes'
```

### Escape sequences
| Sequence | Result |
|----------|--------|
| `\n` | Newline |
| `\t` | Tab |
| `\\` | Backslash |
| `\"` | Double quote |
| `\'` | Single quote |

### Query strings (string interpolation) - PREFERRED
```
string info = $"Player: {name}, Level: {level}"
broadcast($"1 + 1 = {1 + 1}")         // arbitrary expressions in {}
broadcast($"Score: {getScore()}")      // function calls in {}
```

- Prefix: `$` before the opening quote
- Expressions go inside `{}`
- Nested braces are tracked by depth
- Both `$"..."` and `$'...'` work

### Character access by index
```
string first = name[0]   // "B" from "BACKGWA"
```

---

## 5. Lists & Objects

### List
```
list items = [1, "hello", true, [2, 3]]
number first = items[0]          // 1
items[0] = 99                    // mutation
```

### Object
```
object player = {
    "name": "BACKGWA",
    "level": 42,
    "items": ["sword", "shield"]
}

string n = player["name"]       // "BACKGWA"
player["level"] += 1             // mutation with compound assignment

// Keys can be unquoted identifiers or quoted strings
object o = {name: "test", "key with space": 123}
```

**IMPORTANT:** Object access is ALWAYS bracket notation `obj["key"]`. Dot notation (`obj.key`) is NOT supported.

### Nested access
```
object data = {"pos": {"x": 10, "y": 20}}
number x = data["pos"]["x"]     // 10
```

---

## 6. Control Flow

### if / else if / else
```
if (condition) {
    // ...
} else if (condition2) {
    // ...
} else {
    // ...
}
```

Braces `{}` are required. Parentheses `()` around the condition are required.

### while
```
number i = 0
while (i < 10) {
    broadcast($"{i}")
    i++
}
```

### foreach
```
list fruits = ["apple", "banana", "grape"]
foreach (item in fruits) {
    broadcast(item)
}
```

- Only works on `list` type values
- `item` is a new local variable scoped to the loop body

### break
```
number n = 0
while (true) {
    if (n >= 5) { break }
    n++
}
```

### return
```
return          // exit function, return null
return value    // exit function with a value
```

### NOT supported
- `for (init; cond; step)` style loops - use `while` instead
- `switch` / `match` - use `if/else if` chains
- `continue` - not available

---

## 7. Functions

### Declaration
```
[public|private] function name(param1, param2) {
    // body
    return value   // optional
}
```

- Parameters are UNTYPED (no type annotations on parameters)
- Missing arguments default to `null`
- Functions implicitly return `null` if no `return` statement
- Forward references work (two-pass execution: declarations registered first)

```
function greet(player) {
    broadcast($"Welcome, {player}!")
}

function add(a, b) {
    return a + b
}

greet("BACKGWA")
number result = add(3, 5)     // 8
```

### Public functions
```
public function square(n) {
    return n * n
}
// Now callable from other .fst scripts
```

### Script arguments
```
// Executed via: /fascript execute scriptname arg1 arg2
// Access with the built-in `args` list variable
foreach (arg in args) {
    broadcast($"Arg: {arg}")
}
```

---

## 8. Listeners (Event Handlers)

### Declaration
```
[public|private] listener eventType listenerName (paramName) {
    // paramName receives an object with event data
}
```

### Event types
| Type | Trigger | Extra fields |
|------|---------|-------------|
| `onJoin` | Player connects | - |
| `onLeave` | Player disconnects | - |
| `onDeath` | Player dies | - |
| `onBreak` | Block broken | `event["block"]` |
| `onPlace` | Block placed | `event["block"]` |

### Event parameter structure
All events provide:
```
event["player"]              // string: player name
event["position"]["world"]   // string: world name
event["position"]["x"]       // number: X coordinate
event["position"]["y"]       // number: Y coordinate
event["position"]["z"]       // number: Z coordinate
```

Block events (`onBreak`, `onPlace`) additionally provide:
```
event["block"]               // string: block type (e.g. "STONE", "OAK_LOG")
```

### Examples
```
listener onJoin welcome (event) {
    broadcast($"{event["player"]} has joined the server!")
}

listener onBreak blockLog (event) {
    string player = event["player"]
    string block  = event["block"]
    number x = event["position"]["x"]
    number y = event["position"]["y"]
    number z = event["position"]["z"]
    broadcast($"{player} broke {block} at ({x}, {y}, {z})")
}
```

### Removing a listener at runtime
```
destroyListener(listenerName)
// The listener name is automatically registered as a variable
// so you pass it directly, NOT as a string
```

---

## 9. Intervals (Repeating Tasks)

### Declaration
```
interval name (milliseconds) {
    // executed every N milliseconds
}
```

- Minimum interval: 50ms (1 Bukkit tick)
- Variables declared OUTSIDE the interval persist between ticks
- Variables reset to initial values on script reload

```
number tick = 0

interval counter (1000) {
    tick++
    broadcast($"Elapsed: {tick}s")
}
```

### Interval control functions
```
intervalPause("counter")     // pause (timer continues but body skipped)
intervalResume("counter")    // resume execution
intervalDestroy("counter")   // completely remove the interval
```

**NOTE:** Interval control functions take the name as a STRING argument (quoted).

---

## 10. delay() - Deferred Execution

### Syntax
```
delay(milliseconds)
```

- NOT a blocking sleep. It schedules remaining code to run after the specified delay.
- Works inside functions, listeners, intervals, and threads.
- Multiple delays can be chained sequentially.

```
function countdown() {
    broadcast("3...")
    delay(1000)
    broadcast("2...")
    delay(1000)
    broadcast("1...")
    delay(1000)
    broadcast("Go!")
}
```

**Implementation detail:** When `delay()` is hit, it throws a `DelaySignal` with the remaining statements and captured scope. The scheduler picks them up later.

---

## 11. Thread (Parallel Execution)

### Syntax
```
thread (channelCount) {
    functionCall1()
    functionCall2()
    functionCall3()
    // ONLY function calls allowed here
}
```

### How it works
1. All function calls in the body are **shuffled** randomly
2. Shuffled calls are distributed to N channels via **round-robin**
3. Each channel runs on a **separate async thread**
4. Within each channel, calls execute **sequentially**
5. `delay()` works within threads (rescheduled to main thread after delay)

### Constraints (CRITICAL)
- **ONLY function calls** are allowed inside `thread {}` body
- NO variable declarations, if, while, foreach, or any other statements
- Move all logic INTO the called functions
- Global variables modified concurrently may have race conditions

### Example
```
function ping(x, y, z) {
    setblock(x, y, z, "sea_lantern")
    delay(500)
    setblock(x, y, z, "stone")
}

interval example (3000) {
    thread (3) {
        ping(0, 64, 0)
        ping(1, 64, 0)
        ping(2, 64, 0)
        ping(3, 64, 0)
        ping(4, 64, 0)
        ping(5, 64, 0)
    }
}
// 6 calls distributed to 3 channels (2 calls each), running in parallel
```

---

## 12. Built-in Functions - Complete Reference

### Communication
```
broadcast(message)                    // Send message to ALL online players
message(content, playerName)          // Send private message to specific player
```

### Command Execution
```
execute(command)                      // Run as console (full permissions)
execute(command, playerName)          // Run as specific player
```

### Player Info
```
list players = getAllPlayer()          // Returns list of online player name strings
string uuid = getPlayerUUID("Name")   // Returns UUID string, or null if offline
```

### Block Manipulation
```
setblock(x, y, z, blockId)
// x, y, z: number (integer coordinates)
// blockId examples:
//   "stone"                          - simple block
//   "minecraft:stone"                - with namespace (optional)
//   "end_rod[facing=south]"          - with block state
//   "minecraft:quartz_block"         - namespace explicit
//   "air"                            - remove block

object block = getBlockData(worldName, x, y, z)
// worldName: string (e.g. "world")
// Returns: {
//   "type": "STONE",                   // block type name
//   "world": "world",                  // world name
//   "x": 0, "y": 64, "z": 0,          // coordinates
//   "solid": true,                     // is solid block
//   "passable": false,                 // can entities pass through
//   "lightLevel": 0,                   // light level (0~15)
//   "state": {                         // block state properties
//       "facing": "north",
//       "waterlogged": "false"
//   }
// }
```

### Entity Info
```
object entity = getEntityData(uuidString)
// Common fields:
//   "uuid", "type", "name", "world"
//   "x", "y", "z", "yaw", "pitch"
//   "onGround": boolean
//   "velocity": {"x": 0.0, "y": 0.0, "z": 0.0}
//
// Living entities additionally:
//   "health": number, "maxHealth": number, "dead": boolean
//
// Players additionally:
//   "gameMode": string (e.g. "SURVIVAL")
//   "level": number, "food": number, "exp": number
//   "flying": boolean, "op": boolean
```

### Sound
```
worldSound(x, y, z, soundId, volume, pitch)
// Plays sound at coordinates, heard by all nearby players
// Channel: always MASTER

localSound(playerName, soundId, volume, pitch)
// Plays sound for specific player only
// Channel: always MASTER

// soundId examples:
//   "minecraft:block.note_block.pling"
//   "minecraft:entity.player.levelup"
//   "minecraft:block.chest.open"
```

### Persistent Storage (survives server restarts, saved as YAML)
```
// ── Global storage (storage/global.yml) ──
// Shared across ALL scripts
setGlobalStorage(key, value)
number v = getGlobalStorage(key)       // returns null if key doesn't exist

// ── Script-local storage (storage/<scriptname>.yml) ──
// Isolated per script file, no key conflicts between scripts
setStorage(key, value)
string s = getStorage(key)             // returns null if key doesn't exist

// All 5 types can be stored: number, string, boolean, list, object
setStorage("config", {"difficulty": 3, "items": [1, 2, 3]})
```

### Control
```
intervalPause("name")     // pause interval (pass name as STRING)
intervalResume("name")    // resume interval
intervalDestroy("name")   // permanently remove interval

destroyListener(name)     // permanently remove listener (pass as VARIABLE, not string)

delay(milliseconds)       // schedule remaining code after delay
```

---

## 13. Comments

Only single-line comments are supported:
```
// This is a comment
number x = 10  // inline comment
```

No block comments (`/* */`).

---

## 14. Keywords (Reserved Words)

All reserved words (cannot be used as variable/function names):
```
public, private,
number, string, boolean, list, object,
function, return,
if, else, foreach, in, while, break,
listener, interval, thread,
true, false
```

Identifiers: letters, digits, underscore. Must start with letter or underscore.

---

## 15. Script Management

- File extension: `.fst`
- Location: plugin data folder
- Disabled scripts: prefix filename with `-` (e.g., `-disabled.fst`)
- Load order: alphabetical by filename
- Two-pass execution: functions/listeners/intervals registered first, then top-level code runs
- Commands:
  - `/fascript reload` - reload all scripts
  - `/fascript execute <scriptname> [args...]` - run a script with arguments

---

## Code Style Conventions

1. Use section headers: `// ── Section Title ──────────────────────`
2. Align related variable declarations vertically
3. Organize code: variables -> functions -> listeners/intervals
4. Prefer `$"..."` query strings over `+` concatenation
5. Use Korean comments when working with Korean-speaking users
6. Keep functions focused and small

---

## Complete Example Scripts

### Example 1: Welcome System with Visit Counter
```
// ── 방문 카운터 시스템 ─────────────────────────────────────

// 전역 스토리지에서 방문 횟수를 불러옵니다
number totalVisits = getGlobalStorage("totalVisits")
if (totalVisits == 0) {
    setGlobalStorage("totalVisits", 0)
}

// 접속 환영 메시지 + 방문 카운트
listener onJoin welcomePlayer (event) {
    string player = event["player"]
    number visits = getGlobalStorage("totalVisits")
    visits++
    setGlobalStorage("totalVisits", visits)

    broadcast($"{player}님이 접속했습니다! (총 방문: {visits}회)")
    localSound(player, "minecraft:entity.player.levelup", 1.0, 1.0)
}

// 퇴장 메시지
listener onLeave farewellPlayer (event) {
    broadcast($"{event["player"]}님이 퇴장했습니다.")
}
```

### Example 2: Auto-Building a Wall with Threads
```
// ── 벽 자동 건설 시스템 ────────────────────────────────────

function buildColumn(x, z, height, blockType) {
    number y = 64
    while (y < 64 + height) {
        setblock(x, y, z, blockType)
        y++
    }
}

function buildWall() {
    // 10칸 길이의 벽을 3개 채널로 병렬 건설
    thread (3) {
        buildColumn(0, 0, 5, "stone_bricks")
        buildColumn(1, 0, 5, "stone_bricks")
        buildColumn(2, 0, 5, "stone_bricks")
        buildColumn(3, 0, 5, "stone_bricks")
        buildColumn(4, 0, 5, "stone_bricks")
        buildColumn(5, 0, 5, "stone_bricks")
        buildColumn(6, 0, 5, "stone_bricks")
        buildColumn(7, 0, 5, "stone_bricks")
        buildColumn(8, 0, 5, "stone_bricks")
        buildColumn(9, 0, 5, "stone_bricks")
    }
}

buildWall()
```

### Example 3: Mining Leaderboard with Storage
```
// ── 채굴 랭킹 시스템 ──────────────────────────────────────

listener onBreak trackMining (event) {
    string player = event["player"]
    string block  = event["block"]

    // 다이아몬드 광석만 추적
    if (block == "DIAMOND_ORE") {
        string key = $"diamonds_{player}"
        number current = getStorage(key)
        if (current == 0) {
            setStorage(key, 0)
        }
        current++
        setStorage(key, current)
        message($"다이아몬드 채굴! 총 {current}개", player)
        worldSound(
            event["position"]["x"],
            event["position"]["y"],
            event["position"]["z"],
            "minecraft:entity.experience_orb.pickup", 1.0, 1.5
        )
    }
}
```

### Example 4: Animated Beacon with Interval + Thread + Delay
```
// ── 비콘 애니메이션 ────────────────────────────────────────

function beacon(x, y, z, ms) {
    setblock(x, y, z, "glowstone")
    worldSound(x, y, z, "minecraft:block.note_block.pling", 0.5, 1.0)
    delay(ms)
    setblock(x, y, z, "sea_lantern")
    delay(ms)
    setblock(x, y, z, "stone")
}

interval beaconAnim (5000) {
    thread (4) {
        beacon(0,  65, 0, 300)
        beacon(2,  65, 0, 300)
        beacon(4,  65, 0, 300)
        beacon(6,  65, 0, 300)
        beacon(8,  65, 0, 300)
        beacon(10, 65, 0, 300)
        beacon(12, 65, 0, 300)
        beacon(14, 65, 0, 300)
    }
}
```

### Example 5: Player Info Display on Join
```
// ── 접속 시 플레이어 정보 표시 ─────────────────────────────

listener onJoin showInfo (event) {
    string playerName = event["player"]
    string uuid = getPlayerUUID(playerName)

    if (uuid != "") {
        object data = getEntityData(uuid)
        number health = data["health"]
        number maxHp  = data["maxHealth"]
        string gm     = data["gameMode"]
        number lvl    = data["level"]

        message($"=== 플레이어 정보 ===", playerName)
        message($"체력: {health}/{maxHp}", playerName)
        message($"게임모드: {gm}", playerName)
        message($"레벨: {lvl}", playerName)
        message($"========================", playerName)
    }
}
```

### Example 6: Timed Arena Countdown
```
// ── 아레나 카운트다운 ──────────────────────────────────────

function startArena() {
    broadcast("=== 아레나가 10초 후 시작됩니다! ===")

    delay(7000)
    broadcast("3...")
    worldSound(0, 64, 0, "minecraft:block.note_block.pling", 1.0, 1.0)

    delay(1000)
    broadcast("2...")
    worldSound(0, 64, 0, "minecraft:block.note_block.pling", 1.0, 1.2)

    delay(1000)
    broadcast("1...")
    worldSound(0, 64, 0, "minecraft:block.note_block.pling", 1.0, 1.5)

    delay(1000)
    broadcast("=== 전투 시작! ===")
    worldSound(0, 64, 0, "minecraft:block.note_block.pling", 1.0, 2.0)

    // 문 열기 (공기 블록으로 교체)
    setblock(5, 65, 0, "air")
    setblock(5, 66, 0, "air")
}
```

### Example 7: Block Protection Zone
```
// ── 블록 보호 구역 ─────────────────────────────────────────

number protectX1 = -10
number protectZ1 = -10
number protectX2 = 10
number protectZ2 = 10

// && 연산자가 없으므로 중첩 if로 AND 조건 표현
function isInZone(x, z) {
    if (x >= protectX1) {
        if (x <= protectX2) {
            if (z >= protectZ1) {
                if (z <= protectZ2) {
                    return true
                }
            }
        }
    }
    return false
}

listener onBreak protectArea (event) {
    number bx = event["position"]["x"]
    number bz = event["position"]["z"]

    if (isInZone(bx, bz)) {
        string player = event["player"]
        message("이 구역에서는 블록을 파괴할 수 없습니다!", player)
        // 블록을 즉시 복원
        string block = event["block"]
        number by = event["position"]["y"]
        setblock(bx, by, bz, block)
    }
}
```

---

## Rules When Writing Fascript (MUST FOLLOW)

1. **Always declare variable types explicitly** - `number x = 0`, never `x = 0` for new variables
2. **Thread blocks can ONLY contain function calls** - no variable declarations, if, while, etc.
3. **Use `$"..."` query strings** for string interpolation instead of `+` concatenation
4. **Bracket notation only** for object/list access: `obj["key"]`, NEVER `obj.key`
5. **No logical operators** (`&&`, `||`, `!`) - use nested `if` statements instead
6. **No for loops** - use `while` or `foreach` only
7. **Untyped function parameters** - no type annotations on params: `function f(a, b)`, never `function f(number a, string b)`
8. **delay() is non-blocking** - it schedules remaining code, not a sleep
9. **Listener events use bracket notation** - `event["player"]`, `event["position"]["x"]`
10. **Numbers are doubles internally** - displayed as integers when the value is whole (e.g., `42` not `42.0`)
11. **No semicolons required** - statements are newline-separated
12. **No dot notation** anywhere - always use `["key"]` for object access
13. **destroyListener takes a variable**, intervalPause/Resume/Destroy take a **string**: `destroyListener(name)` vs `intervalPause("name")`
14. **No null literal** - null only exists as an internal runtime value
15. **== does not coerce types** - `"1" == 1` is `false`
