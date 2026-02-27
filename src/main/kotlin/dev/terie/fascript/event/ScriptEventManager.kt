package dev.terie.fascript.event

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.ListenerDeclNode
import dev.terie.fascript.script.ScriptContext
import org.bukkit.event.HandlerList

// 스크립트 이벤트 리스너들을 관리합니다.
class ScriptEventManager(private val plugin: FascriptPlugin) {

    private val activeListeners = mutableListOf<ScriptEventListener>()

    // 이름 → 리스너 맵 (destroyListener 에서 사용합니다.)
    private val namedListeners = mutableMapOf<String, ScriptEventListener>()

    fun registerScriptListener(decl: ListenerDeclNode, ctx: ScriptContext) {
        val descriptor = EventTypeRegistry.get(decl.eventType)
        if (descriptor == null) {
            plugin.logger.warning("[${ctx.scriptName}] 알 수 없는 이벤트 타입: ${decl.eventType}")
            return
        }
        val listener = ScriptEventListener(plugin, decl, ctx, descriptor)
        activeListeners += listener
        namedListeners[decl.name] = listener
    }

    // 이름으로 특정 리스너를 영구적으로 제거합니다.
    fun destroyListener(name: String) {
        val listener = namedListeners.remove(name) ?: return
        HandlerList.unregisterAll(listener)
        activeListeners.remove(listener)
    }

    // 리로드 시 모든 리스너를 해제합니다.
    fun unregisterAll() {
        activeListeners.forEach { HandlerList.unregisterAll(it) }
        activeListeners.clear()
        namedListeners.clear()
    }
}
