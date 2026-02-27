// ── 변수 선언 ─────────────────────────────────────────
// 형식: 타입 변수명 = 값

number count  = 0
string name   = "BACKGWA"
boolean active = true
list   items  = [1, 2, "hello"]
object pos    = { "x": 10, "y": 64, "z": -30 }

// ── 값 변경 ────────────────────────────────────────────
count = 5
name  = "TERIE"

// ── 리스트 / 오브젝트 접근 ─────────────────────────────
number first = items[0]          // 1
string world = pos["x"]          // 10

// ── 접근 범위 한정자 ────────────────────────────────────
// public  : 다른 스크립트에서도 참조 가능
// private : 이 스크립트 안에서만 사용 (기본값)

public  string serverName = "My Server"
private number secret     = 42
