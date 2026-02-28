package dev.terie.fascript.script

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.DelaySignal
import dev.terie.fascript.lang.FascriptDiagnosticError
import dev.terie.fascript.lang.FascriptParseError
import dev.terie.fascript.lang.FascriptRuntimeError
import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.lang.Interpreter
import dev.terie.fascript.lang.Lexer
import dev.terie.fascript.lang.Parser
import dev.terie.fascript.util.MessageUtil
import java.io.File

// 스크립트 파일의 로드, 활성화/비활성화, 실행을 관리합니다.
class ScriptManager(private val plugin: FascriptPlugin) {

    private val scriptDir = plugin.dataFolder

    // 모든 활성 스크립트(.fst, - 로 시작하지 않는)를 이름 순으로 로드합니다.
    fun loadAll() {
        // 기존 인터벌과 이벤트 리스너를 먼저 정리합니다.
        plugin.intervalManager.cancelAll()
        plugin.eventManager.unregisterAll()
        // public 전역 선언을 초기화합니다. (이름 순 재로드로 재등록됩니다)
        GlobalRegistry.clear()

        val files = scriptDir
            .listFiles { f -> f.name.endsWith(".fst") && !f.name.startsWith("-") }
            ?.sortedBy { it.name }
            ?: emptyList()

        var loaded = 0
        for (file in files) {
            if (loadScript(file)) loaded++
        }
        plugin.logger.info("스크립트 ${loaded}개를 로드했습니다.")
    }

    private fun loadScript(file: File): Boolean {
        return try {
            val ctx = ScriptContext(plugin, file.name)
            val interp = runScript(file.readText(Charsets.UTF_8), ctx, emptyList())
            interp != null
        } catch (e: FascriptDiagnosticError) {
            MessageUtil.diagnostic(e.fileName, e.line, e.sourceLine, e.title, e.message ?: "오류", e.isWarning)
                .forEach { plugin.componentLogger.warn(it) }
            false
        } catch (e: FascriptRuntimeError) {
            MessageUtil.runtimeDiagnostic(file.name, "로드", "런타임 오류", e.message ?: "오류")
                .forEach { plugin.componentLogger.warn(it) }
            false
        } catch (e: Exception) {
            plugin.logger.warning("[${file.name}] 예상치 못한 오류: ${e.message}")
            false
        }
    }

    // 지정된 스크립트 파일을 인자와 함께 실행합니다.
    // 성공하면 true, 파일을 찾을 수 없으면 false, 오류 시 FascriptRuntimeError를 던집니다.
    fun executeScript(fileName: String, args: List<FascriptValue>): Boolean {
        val file = File(scriptDir, fileName)
        if (!file.exists() || !file.name.endsWith(".fst") || file.name.startsWith("-")) {
            return false
        }
        val ctx = ScriptContext(plugin, fileName)
        runScript(file.readText(Charsets.UTF_8), ctx, args)
        return true
    }

    // 인라인 코드를 즉시 해석하여 실행합니다.
    fun interpretInline(code: String, args: List<FascriptValue>) {
        val ctx = ScriptContext(plugin, "<inline>")
        runScript(code, ctx, args)
    }

    // 소스코드를 파싱하고 실행합니다.
    // delay() 처리를 포함합니다.
    private fun runScript(
        source: String,
        ctx: ScriptContext,
        args: List<FascriptValue>
    ): Interpreter? {
        val tokens = Lexer(source).tokenize()
        val ast = Parser(tokens).parse()
        val interp = Interpreter(ctx, args)
        interp.delayScheduler = { d -> scheduleDelayed(d, ctx, args) }

        try {
            interp.execute(ast)
        } catch (d: DelaySignal) {
            // delay() 이후 남은 구문을 지연 실행합니다.
            scheduleDelayed(d, ctx, args)
        } catch (e: FascriptParseError) {
            val sourceLine = if (e.line > 0) source.lines().getOrElse(e.line - 1) { "" } else ""
            throw FascriptDiagnosticError(ctx.scriptName, e.line, sourceLine, "파싱 오류", e.message ?: "파싱 오류")
        }
        return interp
    }

    // delay() 이후의 남은 구문을 Bukkit 스케줄러로 지연 실행합니다.
    private fun scheduleDelayed(
        signal: DelaySignal,
        ctx: ScriptContext,
        args: List<FascriptValue>
    ) {
        val ticks = (signal.millis / 50L).coerceAtLeast(1L)
        plugin.server.scheduler.runTaskLater(plugin, Runnable {
            val contInterp = Interpreter(ctx, args, signal.capturedScopes)
            contInterp.delayScheduler = { d -> scheduleDelayed(d, ctx, args) }
            try {
                contInterp.executeStatements(signal.remaining)
            } catch (d: DelaySignal) {
                scheduleDelayed(d, ctx, args)
            } catch (e: FascriptRuntimeError) {
                MessageUtil.runtimeDiagnostic(ctx.scriptName, "delay 이후", "런타임 오류", e.message ?: "오류")
                    .forEach { plugin.componentLogger.warn(it) }
            }
        }, ticks)
    }

    // -name.fst → name.fst 이름 변경으로 스크립트를 활성화합니다.
    fun enableScript(name: String): Boolean {
        val disabled = File(scriptDir, "-$name")
        val enabled = File(scriptDir, name)
        if (!disabled.exists()) return false
        return disabled.renameTo(enabled).also { if (it) loadAll() }
    }

    fun enableAll() {
        scriptDir.listFiles { f -> f.name.startsWith("-") && f.name.endsWith(".fst") }
            ?.forEach { it.renameTo(File(scriptDir, it.name.removePrefix("-"))) }
        loadAll()
    }

    // name.fst → -name.fst 이름 변경으로 스크립트를 비활성화합니다.
    fun disableScript(name: String): Boolean {
        val enabled = File(scriptDir, name)
        val disabled = File(scriptDir, "-$name")
        if (!enabled.exists() || name.startsWith("-")) return false
        return enabled.renameTo(disabled).also { if (it) loadAll() }
    }

    fun disableAll() {
        scriptDir.listFiles { f -> !f.name.startsWith("-") && f.name.endsWith(".fst") }
            ?.forEach { it.renameTo(File(scriptDir, "-${it.name}")) }
        loadAll()
    }

    // 현재 활성화된 스크립트 파일 이름 목록을 반환합니다.
    fun listActiveScripts(): List<String> =
        scriptDir.listFiles { f -> f.name.endsWith(".fst") && !f.name.startsWith("-") }
            ?.map { it.name }
            ?.sorted()
            ?: emptyList()

    // 비활성화된 스크립트의 원래 이름 목록을 반환합니다. (-name.fst → name.fst)
    fun listDisabledScripts(): List<String> =
        scriptDir.listFiles { f -> f.name.startsWith("-") && f.name.endsWith(".fst") }
            ?.map { it.name.removePrefix("-") }
            ?.sorted()
            ?: emptyList()
}

