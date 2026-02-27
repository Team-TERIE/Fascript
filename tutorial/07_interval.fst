// ── 인터벌 선언 ────────────────────────────────────────
// 형식: interval 이름 (밀리초) { ... }
// 지정한 밀리초마다 블록 내부를 반복 실행합니다.

number tick = 0

interval counter (1000) {
    tick++
    broadcast($"경과 시간: {tick}초")
}

// ── 인터벌 제어 ────────────────────────────────────────
// intervalPause("counter")    → 일시 정지 (타이머는 유지)
// intervalResume("counter")   → 재개
// intervalDestroy("counter")  → 완전 제거

// ── delay() ────────────────────────────────────────────
// delay(밀리초) 이후의 코드를 지정한 시간 후에 실행합니다.
// 함수, 리스너, 인터벌 본문 내에서 사용할 수 있습니다.

function countdown() {
    broadcast("3...")
    delay(1000)
    broadcast("2...")
    delay(1000)
    broadcast("1...")
    delay(1000)
    broadcast("출발!")
}

// ── 인터벌과 변수 ──────────────────────────────────────
// interval 외부의 변수는 틱 간에 값이 유지됩니다. (위의 tick 참고)
// 스크립트를 리로드하면 변수는 초기값으로 초기화됩니다.
