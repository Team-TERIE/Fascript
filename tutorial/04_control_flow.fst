// ── if / else if / else ────────────────────────────────
number x = 5

if (x > 10) {
    broadcast("10 초과")
} else if (x == 5) {
    broadcast("정확히 5")
} else {
    broadcast("5 미만")
}

// ── while ──────────────────────────────────────────────
number i = 0
while (i < 3) {
    broadcast($"{i}번 반복")
    i++
}

// ── foreach ────────────────────────────────────────────
list fruits = ["사과", "바나나", "포도"]
foreach (fruit in fruits) {
    broadcast(fruit)
}

// ── break ──────────────────────────────────────────────
number n = 0
while (true) {
    if (n >= 3) { break }
    n++
}

// ── return (함수 내에서) ───────────────────────────────
// return        → 값 없이 즉시 종료
// return 값     → 값을 반환하고 종료
