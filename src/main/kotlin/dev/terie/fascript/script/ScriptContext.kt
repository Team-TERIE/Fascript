package dev.terie.fascript.script

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.FunctionDeclNode
import dev.terie.fascript.lang.Interpreter
import dev.terie.fascript.lang.IntervalNode
import dev.terie.fascript.lang.ListenerDeclNode
import dev.terie.fascript.lang.FascriptValue

// 스크립트 파일 하나의 런타임 상태를 관리합니다.
// globalScope는 인터프리터와 공유되어 인터벌 실행 간 변수 상태를 유지합니다.
class ScriptContext(
    val plugin: FascriptPlugin,
    val scriptName: String
) {
    // 이 스크립트의 전역 변수 스코프
    val globalScope: MutableMap<String, FascriptValue> = mutableMapOf()

    // 이 스크립트에서 선언된 함수들
    private val functions = mutableMapOf<String, FunctionDeclNode>()

    fun registerFunction(decl: FunctionDeclNode) {
        functions[decl.name] = decl
    }

    fun getFunction(name: String): FunctionDeclNode? = functions[name]

    fun registerListener(decl: ListenerDeclNode) {
        plugin.eventManager.registerScriptListener(decl, this)
    }

    fun registerInterval(decl: IntervalNode, interp: Interpreter) {
        plugin.intervalManager.schedule(decl, interp, this)
    }
}
