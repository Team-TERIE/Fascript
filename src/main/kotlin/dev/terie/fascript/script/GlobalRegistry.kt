package dev.terie.fascript.script

import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.lang.FunctionDeclNode

// public으로 선언된 변수와 함수를 모든 스크립트에서 접근할 수 있도록 관리합니다.
// 리로드 시 clear()로 초기화합니다.
object GlobalRegistry {

    // public 전역 변수
    val variables: MutableMap<String, FascriptValue> = mutableMapOf()

    // public 전역 함수
    val functions: MutableMap<String, FunctionDeclNode> = mutableMapOf()

    fun clear() {
        variables.clear()
        functions.clear()
    }
}
