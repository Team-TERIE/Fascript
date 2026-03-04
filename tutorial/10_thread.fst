// ── thread 블록 ─────────────────────────────────────────
// 형식: thread (채널 수) { 함수 호출들... }
// 본문의 함수 호출들을 N개의 채널로 나누어 병렬로 실행합니다.
// 각 채널은 독립된 스레드에서 실행되며, 서로 영향을 주지 않습니다.

// ── 기본 사용법 ─────────────────────────────────────────
// thread 없이 사용하면 순차 실행됩니다.

function ping(x, y, z) {
    setblock(x, y, z, "sea_lantern")
    delay(500)
    setblock(x, y, z, "stone")
}

// thread (3)으로 감싸면 3개의 채널로 나뉘어 병렬 실행됩니다.
// 아래 9개 호출은 3개씩 묶여 동시에 시작됩니다.

interval example (3000) {
    thread (3) {
        ping(0, 64, 0)
        ping(1, 64, 0)
        ping(2, 64, 0)
        ping(3, 64, 0)
        ping(4, 64, 0)
        ping(5, 64, 0)
        ping(6, 64, 0)
        ping(7, 64, 0)
        ping(8, 64, 0)
    }
}

// ── 분배 방식 ────────────────────────────────────────────
// 함수 호출들은 랜덤 셔플 후 라운드로빈으로 채널에 분배됩니다.
// thread (2) 이고 호출이 6개면:
//   채널 1: 호출 A, C, E
//   채널 2: 호출 B, D, F
// (셔플로 인해 실행 순서는 매번 달라질 수 있습니다)

// ── delay와 함께 사용 ────────────────────────────────────
// 각 채널 내에서 delay는 기존과 동일하게 동작합니다.
// delay 이후의 코드는 지정한 시간 후 실행됩니다.
// 채널 간 delay는 서로 독립적으로 처리됩니다.

function blinker(x, y, z, ms) {
    worldSound(x, y, z, "minecraft:block.note_block.pling", 0.5, 1.0)
    setblock(x, y, z, "glowstone")
    delay(ms)
    setblock(x, y, z, "stone")
    delay(ms)
}

interval blink_demo (4000) {
    thread (4) {
        blinker(0, 64,  0, 800)
        blinker(2, 64,  0, 800)
        blinker(4, 64,  0, 800)
        blinker(6, 64,  0, 800)
        blinker(8, 64,  0, 800)
        blinker(10, 64, 0, 800)
        blinker(12, 64, 0, 800)
        blinker(14, 64, 0, 800)
    }
}

// ── 주의사항 ─────────────────────────────────────────────
// - thread 내부에는 함수 호출만 사용할 수 있습니다.
//   (변수 선언, if, while 등은 thread 밖에 작성하세요)
// - 전역 변수를 thread 내부에서 동시에 수정하면 값이 꼬일 수 있습니다.
// - delay가 없는 단순 함수 나열은 thread 없이도 충분합니다.
