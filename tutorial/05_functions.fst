// ── 함수 선언 ──────────────────────────────────────────
function greet(player) {
    broadcast($"환영합니다, {player}님!")
}

function add(a, b) {
    return a + b
}

// ── 호출 ───────────────────────────────────────────────
greet("BACKGWA")

number result = add(3, 5)
broadcast(result)   // 8

// ── public 함수: 다른 스크립트에서도 호출 가능 ─────────
public function square(n) {
    return n * n
}

// ── 스크립트 인자: args ────────────────────────────────
// /fascript execute 파일명 인자1 인자2 ...
// args[0], args[1] 으로 접근합니다.

function showArgs() {
    foreach (arg in args) {
        broadcast($"인자: {arg}")
    }
}
showArgs()
