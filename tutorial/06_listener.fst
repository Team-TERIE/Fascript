// ── 리스너 선언 ────────────────────────────────────────
// 형식: listener 이벤트타입 이름 (파라미터) { ... }
//
// 지원 이벤트:
//   onJoin   - 플레이어 접속
//   onLeave  - 플레이어 퇴장
//   onDeath  - 플레이어 사망
//   onBreak  - 블록 파괴
//   onPlace  - 블록 설치
//
// 이벤트 파라미터 공통 필드:
//   event["player"]           → 플레이어 이름
//   event["position"]["world"] → 월드 이름
//   event["position"]["x/y/z"] → 좌표
//
// onBreak / onPlace 추가 필드:
//   event["block"]            → 블록 종류 (예: "STONE")

listener onJoin welcome (event) {
    broadcast($"{event["player"]}님이 접속했습니다!")
}

listener onLeave goodbye (event) {
    string player = event["player"]
    string world  = event["position"]["world"]
    broadcast($"{player}님이 {world}에서 퇴장했습니다.")
}

listener onBreak blockLog (event) {
    string player = event["player"]
    string block  = event["block"]
    broadcast($"{player}님이 {block} 블록을 부쉈습니다.")
}

// ── 리스너 제거 ────────────────────────────────────────
// destroyListener(welcome)   → welcome 리스너를 제거합니다.
