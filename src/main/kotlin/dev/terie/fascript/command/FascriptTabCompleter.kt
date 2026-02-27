package dev.terie.fascript.command

import dev.terie.fascript.FascriptPlugin
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter

class FascriptTabCompleter(private val plugin: FascriptPlugin) : TabCompleter {

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): List<String> {
        return when (args.size) {
            1 -> SUBCOMMANDS.filter { it.startsWith(args[0], ignoreCase = true) }

            2 -> when (args[0].lowercase()) {
                "enable"  -> (listOf("all") + plugin.scriptManager.listDisabledScripts())
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                "disable" -> (listOf("all") + plugin.scriptManager.listActiveScripts())
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                "execute" -> plugin.scriptManager.listActiveScripts()
                    .filter { it.startsWith(args[1], ignoreCase = true) }
                "reload"  -> RELOAD_OPTIONS.filter { it.startsWith(args[1], ignoreCase = true) }
                else      -> emptyList()
            }

            else -> emptyList()
        }
    }

    companion object {
        private val SUBCOMMANDS = listOf("enable", "disable", "execute", "interpret", "list", "reload")
        private val RELOAD_OPTIONS = listOf("all", "scripts")
    }
}
