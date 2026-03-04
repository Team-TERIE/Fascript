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

// ── 블록 설치 ──────────────────────────────────────────
// setblock(x, y, z, 블록ID)
// 지정한 좌표에 블록을 설치합니다.
// 블록 ID는 minecraft: 네임스페이스를 생략할 수 있습니다.
// 블록 상태(BlockState)는 대괄호로 표기합니다.

setblock(0, 64, 0, "stone")                      // 돌 설치
setblock(0, 64, 0, "sea_lantern")                // 씨 랜턴 설치
setblock(0, 64, 0, "air")                        // 블록 제거
setblock(0, 64, 0, "end_rod[facing=south]")      // 블록 상태 지정
setblock(0, 64, 0, "minecraft:quartz_block")     // 네임스페이스 명시

// ── 사운드 재생 ─────────────────────────────────────────
// worldSound(x, y, z, 사운드ID, 볼륨, 피치)
// 지정한 좌표에서 소리를 재생합니다. 범위 내 모든 플레이어에게 들립니다.
// 채널은 master로 고정됩니다.

worldSound(0, 64, 0, "minecraft:block.note_block.pling", 1.0, 1.0)
worldSound(0, 64, 0, "presencefootsteps:pf_presence.glass.glass_hit", 0.1, 0.25)

// localSound(플레이어명, 사운드ID, 볼륨, 피치)
// 지정한 플레이어에게만 소리를 재생합니다. 채널은 master로 고정됩니다.

localSound("BACKGWA", "minecraft:entity.player.levelup", 1.0, 1.0)
localSound("BACKGWA", "minecraft:block.chest.open", 0.5, 1.2)

// ── 스토리지 (→ 08_storage.fst 참고) ──────────────────
// setGlobalStorage(key, value) / getGlobalStorage(key)
// setStorage(key, value)       / getStorage(key)

// ── 인터벌 제어 (→ 07_interval.fst 참고) ──────────────
// intervalPause(name), intervalResume(name), intervalDestroy(name)

// ── 리스너 제거 (→ 06_listener.fst 참고) ──────────────
// destroyListener(name)

// ── 지연 실행 (→ 07_interval.fst 참고) ────────────────
// delay(밀리초)
