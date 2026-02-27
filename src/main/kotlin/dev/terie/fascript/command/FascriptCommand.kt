package dev.terie.fascript.command

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.FascriptDiagnosticError
import dev.terie.fascript.lang.FascriptRuntimeError
import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.util.MessageUtil
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender

class FascriptCommand(private val plugin: FascriptPlugin) : CommandExecutor {

    override fun onCommand(
        sender: CommandSender,
        command: Command,
        label: String,
        args: Array<out String>
    ): Boolean {
        if (args.isEmpty()) {
            sendHint(sender)
            return true
        }

        when (args[0].lowercase()) {
            "enable"    -> handleEnable(sender, args)
            "disable"   -> handleDisable(sender, args)
            "execute"   -> handleExecute(sender, args)
            "interpret" -> handleInterpret(sender, args)
            "list"      -> handleList(sender)
            "reload"    -> handleReload(sender, args)
            else -> {
                sender.sendMessage(MessageUtil.error("알 수 없는 명령어: ${args[0]}"))
                sendHint(sender)
            }
        }
        return true
    }

    // 모든 하위 명령어 힌트를 표시합니다.
    private fun sendHint(sender: CommandSender) {
        sender.sendMessage(MessageUtil.info("사용 가능한 명령어:"))
        sender.sendMessage(MessageUtil.info("  /fascript enable [all|파일명]"))
        sender.sendMessage(MessageUtil.info("  /fascript disable [all|파일명]"))
        sender.sendMessage(MessageUtil.info("  /fascript execute <파일명> [인자...]"))
        sender.sendMessage(MessageUtil.info("  /fascript interpret <코드> [인자...]"))
        sender.sendMessage(MessageUtil.info("  /fascript list"))
        sender.sendMessage(MessageUtil.info("  /fascript reload [all|scripts]"))
    }

    private fun handleEnable(sender: CommandSender, args: Array<out String>) {
        val target = args.getOrElse(1) { "all" }
        if (target == "all") {
            plugin.scriptManager.enableAll()
            sender.sendMessage(MessageUtil.info("모든 스크립트를 활성화했습니다."))
        } else {
            if (plugin.scriptManager.enableScript(target)) {
                sender.sendMessage(MessageUtil.info("$target 을(를) 활성화했습니다."))
            } else {
                sender.sendMessage(MessageUtil.error("$target 을(를) 찾을 수 없습니다."))
            }
        }
    }

    private fun handleDisable(sender: CommandSender, args: Array<out String>) {
        val target = args.getOrElse(1) { "all" }
        if (target == "all") {
            plugin.scriptManager.disableAll()
            sender.sendMessage(MessageUtil.info("모든 스크립트를 비활성화했습니다."))
        } else {
            if (plugin.scriptManager.disableScript(target)) {
                sender.sendMessage(MessageUtil.info("$target 을(를) 비활성화했습니다."))
            } else {
                sender.sendMessage(MessageUtil.error("$target 을(를) 찾을 수 없습니다."))
            }
        }
    }

    private fun handleExecute(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(MessageUtil.error("사용법: /fascript execute <파일명> [인자...]"))
            return
        }
        val fileName = args[1]
        val scriptArgs = args.drop(2).map { parseArg(it) }
        try {
            if (!plugin.scriptManager.executeScript(fileName, scriptArgs)) {
                sender.sendMessage(MessageUtil.error("$fileName 을(를) 찾을 수 없거나 비활성화되어 있습니다."))
            }
        } catch (e: FascriptRuntimeError) {
            if (e is FascriptDiagnosticError) {
                MessageUtil.diagnostic(e.fileName, e.line, e.sourceLine, e.title, e.message ?: "오류", e.isWarning)
                    .forEach { sender.sendMessage(it) }
            } else {
                MessageUtil.runtimeDiagnostic(fileName, "실행", "런타임 오류", e.message ?: "오류")
                    .forEach { sender.sendMessage(it) }
            }
        }
    }

    private fun handleInterpret(sender: CommandSender, args: Array<out String>) {
        if (args.size < 2) {
            sender.sendMessage(MessageUtil.error("사용법: /fascript interpret <코드> [인자...]"))
            return
        }
        val code = args[1].removeSurrounding("\"").removeSurrounding("'")
        val scriptArgs = args.drop(2).map { parseArg(it) }
        try {
            plugin.scriptManager.interpretInline(code, scriptArgs)
        } catch (e: FascriptRuntimeError) {
            if (e is FascriptDiagnosticError) {
                MessageUtil.diagnostic(e.fileName, e.line, e.sourceLine, e.title, e.message ?: "오류", e.isWarning)
                    .forEach { sender.sendMessage(it) }
            } else {
                MessageUtil.runtimeDiagnostic("<inline>", "interpret", "런타임 오류", e.message ?: "오류")
                    .forEach { sender.sendMessage(it) }
            }
        }
    }

    private fun handleList(sender: CommandSender) {
        val scripts = plugin.scriptManager.listActiveScripts()

        // 헤더는 노란색으로 표시합니다.
        sender.sendMessage(Component.text("# Fascript").color(NamedTextColor.YELLOW))

        if (scripts.isEmpty()) {
            sender.sendMessage(Component.text("  (스크립트 없음)").color(NamedTextColor.WHITE))
            return
        }

        // 각 항목은 흰색이며, 클릭 시 /fascript execute 명령어를 채팅창에 제안합니다.
        for (name in scripts) {
            val item = Component.text("- $name")
                .color(NamedTextColor.WHITE)
                .clickEvent(ClickEvent.suggestCommand("/fascript execute $name "))
            sender.sendMessage(item)
        }
    }

    private fun handleReload(sender: CommandSender, args: Array<out String>) {
        val target = args.getOrElse(1) { "all" }
        when (target) {
            "all", "scripts" -> {
                plugin.scriptManager.loadAll()
                sender.sendMessage(MessageUtil.info("스크립트를 다시 로드했습니다."))
            }
            else -> sender.sendMessage(MessageUtil.error("reload 대상은 all 또는 scripts 입니다."))
        }
    }

    // 인자 문자열을 적절한 FascriptValue로 변환합니다.
    private fun parseArg(s: String): FascriptValue {
        s.toDoubleOrNull()?.let { return FascriptValue.FNumber(it) }
        if (s == "true")  return FascriptValue.FBoolean(true)
        if (s == "false") return FascriptValue.FBoolean(false)
        return FascriptValue.FString(s)
    }
}
