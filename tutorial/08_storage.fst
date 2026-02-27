// ── 전역 스토리지 ──────────────────────────────────────
// 모든 스크립트에서 공유합니다. (storage/global.yml)
// 서버 재시작 후에도 값이 유지됩니다.

setGlobalStorage("visits", 0)
number visits = getGlobalStorage("visits")

listener onJoin countVisit (event) {
    number v = getGlobalStorage("visits")
    v++
    setGlobalStorage("visits", v)
    broadcast($"서버 총 방문 횟수: {v}회")
}

// ── 스크립트별 스토리지 ────────────────────────────────
// 이 스크립트 전용입니다. (storage/08_storage.yml)
// 다른 스크립트와 키가 겹쳐도 충돌하지 않습니다.

setStorage("lastPlayer", "없음")

listener onJoin trackLast (event) {
    string prev = getStorage("lastPlayer")
    setStorage("lastPlayer", event["player"])
    broadcast($"이전 접속자: {prev}")
}

// ── 저장 가능한 타입 ───────────────────────────────────
// number, string, boolean, list, object 모두 저장 가능합니다.

setStorage("score", 100)
setStorage("tags",  ["vip", "admin"])
setStorage("info",  { "level": 5, "rank": "gold" })
