// ── 메시지 ─────────────────────────────────────────────
broadcast("전체 채팅에 메시지 전송")
message("귓속말 내용", "BACKGWA")   // message(내용, 플레이어명)

// ── 명령 실행 ──────────────────────────────────────────
execute("time set day")              // 콘솔 권한으로 실행
execute("fly", "BACKGWA")           // 특정 플레이어로 실행

// ── 플레이어 ───────────────────────────────────────────
list players = getAllPlayer()        // 온라인 플레이어 이름 목록
foreach (p in players) {
    broadcast($"온라인: {p}")
}

string uuid = getPlayerUUID("BACKGWA")  // UUID 문자열 반환 (오프라인이면 null)

// ── 블록 정보 ──────────────────────────────────────────
// getBlockData(월드, x, y, z) → object
object block = getBlockData("world", 0, 64, 0)
// block["type"]        → 블록 종류 (예: "STONE")
// block["solid"]       → 솔리드 여부 (boolean)
// block["passable"]    → 통과 가능 여부 (boolean)
// block["lightLevel"]  → 빛 밝기 (0~15)
// block["state"]       → 블록 상태 오브젝트 (예: facing, waterlogged 등)

// ── 엔티티 정보 ────────────────────────────────────────
// getEntityData(UUID) → object
object entity = getEntityData(uuid)
// 공통:  entity["type"], entity["name"], entity["world"]
//        entity["x"], entity["y"], entity["z"]
//        entity["yaw"], entity["pitch"], entity["onGround"]
//        entity["velocity"]["x/y/z"]
// 생물:  entity["health"], entity["maxHealth"], entity["dead"]
// 플레이어: entity["gameMode"], entity["level"], entity["food"]
//           entity["exp"], entity["flying"], entity["op"]

// ── 스토리지 (→ 08_storage.fst 참고) ──────────────────
// setGlobalStorage(key, value) / getGlobalStorage(key)
// setStorage(key, value)       / getStorage(key)

// ── 인터벌 제어 (→ 07_interval.fst 참고) ──────────────
// intervalPause(name), intervalResume(name), intervalDestroy(name)

// ── 리스너 제거 (→ 06_listener.fst 참고) ──────────────
// destroyListener(name)

// ── 지연 실행 (→ 07_interval.fst 참고) ────────────────
// delay(밀리초)
