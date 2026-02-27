package dev.terie.fascript.interval

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.DelaySignal
import dev.terie.fascript.lang.FascriptRuntimeError
import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.lang.Interpreter
import dev.terie.fascript.lang.IntervalNode
import dev.terie.fascript.lang.Node
import dev.terie.fascript.script.ScriptContext
import dev.terie.fascript.util.MessageUtil
import org.bukkit.scheduler.BukkitTask

// 이름 기반으로 interval을 관리합니다.
class IntervalManager(private val plugin: FascriptPlugin) {

    // 이름 → 인터벌 상태 맵
    private val intervals = mutableMapOf<String, IntervalState>()

    // interval 선언을 Bukkit 스케줄러에 등록합니다.
    fun schedule(decl: IntervalNode, sourceInterp: Interpreter, ctx: ScriptContext) {
        val millis = sourceInterp.evalExpr(decl.millis).toNumber().toLong()
        val ticks = (millis / 50L).coerceAtLeast(1L)

        val state = IntervalState(
            name = decl.name,
            body = decl.body,
            ctx = ctx
        )

        val task = plugin.server.scheduler.runTaskTimer(plugin, Runnable {
            if (state.isPaused) return@Runnable
            tickInterval(state)
        }, ticks, ticks)

        state.task = task
        intervals[decl.name] = state
    }

    private fun tickInterval(state: IntervalState) {
        val interp = Interpreter(state.ctx, emptyList())
        try {
            interp.executeStatements(state.body)
        } catch (d: DelaySignal) {
            scheduleDelayed(d, state)
        } catch (e: FascriptRuntimeError) {
            MessageUtil.runtimeDiagnostic(state.ctx.scriptName, "인터벌/${state.name}", "런타임 오류", e.message ?: "오류")
                .forEach { plugin.componentLogger.warn(it) }
        }
    }

    // delay() 이후 남은 구문을 지연 실행합니다.
    private fun scheduleDelayed(signal: DelaySignal, state: IntervalState) {
        val ticks = (signal.millis / 50L).coerceAtLeast(1L)
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val contInterp = Interpreter(state.ctx, emptyList())
            try {
                contInterp.executeStatements(signal.remaining)
            } catch (d: DelaySignal) {
                scheduleDelayed(d, state)
            } catch (e: FascriptRuntimeError) {
                MessageUtil.runtimeDiagnostic(state.ctx.scriptName, "인터벌/${state.name} (delay 이후)", "런타임 오류", e.message ?: "오류")
                    .forEach { plugin.componentLogger.warn(it) }
            }
        }, ticks)
    }

    // 인터벌을 일시 정지합니다. (타이머는 계속 실행되지만 틱마다 실행을 건너뜁니다.)
    fun pause(name: String) {
        intervals[name]?.isPaused = true
    }

    // 인터벌 일시 정지를 해제합니다.
    fun resume(name: String) {
        intervals[name]?.isPaused = false
    }

    // 인터벌을 완전히 제거합니다.
    fun destroy(name: String) {
        intervals.remove(name)?.task?.cancel()
    }

    // 모든 인터벌을 취소합니다. (리로드 시 호출)
    fun cancelAll() {
        intervals.values.forEach { it.task?.cancel() }
        intervals.clear()
    }
}

// 인터벌 하나의 실행 상태입니다.
private class IntervalState(
    val name: String,
    val body: List<Node>,
    val ctx: ScriptContext
) {
    var task: BukkitTask? = null
    var isPaused: Boolean = false
}
