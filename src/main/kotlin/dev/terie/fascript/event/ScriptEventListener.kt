package dev.terie.fascript.event

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.DelaySignal
import dev.terie.fascript.lang.FascriptRuntimeError
import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.lang.Interpreter
import dev.terie.fascript.lang.ListenerDeclNode
import dev.terie.fascript.lang.ReturnSignal
import dev.terie.fascript.script.ScriptContext
import dev.terie.fascript.util.MessageUtil
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener

// 하나의 listener 선언에 대응하는 Bukkit 이벤트 리스너입니다.
// EventDescriptor의 paramsExtractor 람다를 사용하여 이벤트 타입에 무관하게 동작합니다.
class ScriptEventListener(
    private val plugin: FascriptPlugin,
    private val decl: ListenerDeclNode,
    private val ctx: ScriptContext,
    private val descriptor: EventDescriptor
) : Listener {

    init {
        // EventExecutor로 이벤트 클래스를 동적 등록합니다.
        plugin.server.pluginManager.registerEvent(
            descriptor.eventClass,
            this,
            EventPriority.NORMAL,
            { _, event ->
                if (descriptor.eventClass.isInstance(event)) {
                    dispatch(descriptor.paramsExtractor(event))
                }
            },
            plugin
        )
    }

    // 이벤트 발생 시 Fascript 코드를 실행합니다.
    private fun dispatch(eventObj: FascriptValue) {
        val interp = Interpreter(ctx, emptyList())
        interp.delayScheduler = { d -> scheduleDelayed(d, eventObj) }
        interp.setGlobalVar(decl.paramsName, eventObj)
        try {
            interp.executeStatements(decl.body)
        } catch (d: DelaySignal) {
            scheduleDelayed(d, eventObj)
        } catch (_: ReturnSignal) {
            return
        } catch (e: FascriptRuntimeError) {
            MessageUtil.runtimeDiagnostic(ctx.scriptName, "리스너/${decl.eventType}", "런타임 오류", e.message ?: "오류")
                .forEach { plugin.componentLogger.warn(it) }
        }
    }

    private fun scheduleDelayed(signal: DelaySignal, eventObj: FascriptValue) {
        val ticks = (signal.millis / 50L).coerceAtLeast(1L)
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val contInterp = Interpreter(ctx, emptyList(), signal.capturedScopes)
            contInterp.delayScheduler = { d -> scheduleDelayed(d, eventObj) }
            contInterp.setGlobalVar(decl.paramsName, eventObj)
            try {
                contInterp.executeStatements(signal.remaining)
            } catch (d: DelaySignal) {
                scheduleDelayed(d, eventObj)
            } catch (_: ReturnSignal) {
                return@Runnable
            } catch (e: FascriptRuntimeError) {
                MessageUtil.runtimeDiagnostic(ctx.scriptName, "리스너/${decl.eventType} (delay 이후)", "런타임 오류", e.message ?: "오류")
                    .forEach { plugin.componentLogger.warn(it) }
            }
        }, ticks)
    }
}
