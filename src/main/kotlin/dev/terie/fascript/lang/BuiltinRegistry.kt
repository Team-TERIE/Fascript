package dev.terie.fascript.lang

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.script.ScriptContext

// 내장 함수 실행에 필요한 컨텍스트입니다.
data class BuiltinContext(
    val plugin: FascriptPlugin,
    val scriptCtx: ScriptContext
)

// 내장 함수 타입입니다.
typealias BuiltinFn = (args: List<FascriptValue>, ctx: BuiltinContext) -> FascriptValue

// 내장 함수 레지스트리입니다.
// 새 내장 함수는 register()로 등록하면 인터프리터 수정 없이 사용할 수 있습니다.
object BuiltinRegistry {

    private val functions = mutableMapOf<String, BuiltinFn>()

    fun register(name: String, fn: BuiltinFn) {
        functions[name] = fn
    }

    fun get(name: String): BuiltinFn? = functions[name]

    fun has(name: String): Boolean = name in functions
}
