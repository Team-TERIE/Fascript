package dev.terie.fascript.lang

// Fascript 런타임의 모든 값 타입입니다.
sealed class FascriptValue {

    data class FNumber(val v: Double) : FascriptValue()
    data class FString(val v: String) : FascriptValue()
    data class FBoolean(val v: Boolean) : FascriptValue()
    data class FList(val v: MutableList<FascriptValue>) : FascriptValue()
    data class FObject(val v: MutableMap<String, FascriptValue>) : FascriptValue()
    object FNull : FascriptValue()

    override fun toString(): String = when (this) {
        is FNumber  -> if (v % 1.0 == 0.0) v.toLong().toString() else v.toString()
        is FString  -> v
        is FBoolean -> v.toString()
        is FList    -> "[${v.joinToString(", ")}]"
        is FObject  -> "{${v.entries.joinToString(", ") { "\"${it.key}\": ${it.value}" }}}"
        is FNull    -> "null"
    }

    fun isTruthy(): Boolean = when (this) {
        is FBoolean -> v
        is FNumber  -> v != 0.0
        is FString  -> v.isNotEmpty()
        is FList    -> v.isNotEmpty()
        is FObject  -> v.isNotEmpty()
        is FNull    -> false
    }

    // 산술 연산에 사용할 숫자 값으로 변환합니다.
    fun toNumber(): Double = when (this) {
        is FNumber  -> v
        is FString  -> v.toDoubleOrNull()
            ?: throw FascriptRuntimeError("문자열을 숫자로 변환할 수 없습니다: \"$v\"")
        is FBoolean -> if (v) 1.0 else 0.0
        else        -> throw FascriptRuntimeError("이 값은 숫자로 변환할 수 없습니다: $this")
    }
}

// 스크립트 실행 중 발생하는 오류입니다.
open class FascriptRuntimeError(message: String) : Exception(message)

// 파싱 중 발생하는 오류입니다. line은 오류가 발생한 소스 줄 번호입니다.
class FascriptParseError(message: String, val line: Int = 0) : Exception(message)

// 파일명, 줄 번호, 소스 코드, 제목이 포함된 진단 오류입니다.
// FascriptRuntimeError의 서브클래스로, 기존 catch 블록과 호환됩니다.
class FascriptDiagnosticError(
    val fileName: String,
    val line: Int,          // 0 = 소스 줄 정보 없음
    val sourceLine: String,
    val title: String,
    message: String,
    val isWarning: Boolean = false
) : FascriptRuntimeError(message)

// return 문 처리를 위한 제어 흐름 신호입니다.
class ReturnSignal(val value: FascriptValue) : Throwable()

// break 문 처리를 위한 제어 흐름 신호입니다.
class BreakSignal : Throwable()

// delay() 호출 후 남은 구문들을 지연 실행하기 위한 신호입니다.
class DelaySignal(val millis: Long, val remaining: List<Node>) : Throwable()
