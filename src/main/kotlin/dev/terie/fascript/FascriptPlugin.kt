package dev.terie.fascript

import dev.terie.fascript.command.FascriptCommand
import dev.terie.fascript.command.FascriptTabCompleter
import dev.terie.fascript.event.EventDescriptor
import dev.terie.fascript.event.EventTypeRegistry
import dev.terie.fascript.event.ScriptEventManager
import dev.terie.fascript.interval.IntervalManager
import dev.terie.fascript.lang.BuiltinRegistry
import dev.terie.fascript.lang.FascriptValue
import dev.terie.fascript.script.ScriptManager
import dev.terie.fascript.storage.StorageManager
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.LivingEntity
import org.bukkit.entity.Player
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID

class FascriptPlugin : JavaPlugin() {

    lateinit var scriptManager: ScriptManager
    lateinit var eventManager: ScriptEventManager
    lateinit var intervalManager: IntervalManager
    lateinit var storageManager: StorageManager

    override fun onEnable() {
        if (!dataFolder.exists()) dataFolder.mkdirs()

        intervalManager = IntervalManager(this)
        eventManager = ScriptEventManager(this)
        storageManager = StorageManager(this)
        scriptManager = ScriptManager(this)

        registerEventTypes()
        registerBuiltins()
        registerCommands()

        scriptManager.loadAll()
        logger.info("Fascript 플러그인이 활성화되었습니다.")
    }

    override fun onDisable() {
        intervalManager.cancelAll()
        eventManager.unregisterAll()
        logger.info("Fascript 플러그인이 비활성화되었습니다.")
    }

    // 지원하는 Fascript 이벤트 타입을 등록합니다.
    // 새 이벤트는 여기에 추가하면 됩니다.
    private fun registerEventTypes() {
        EventTypeRegistry.register("onJoin",
            EventDescriptor(PlayerJoinEvent::class.java) { event ->
                val p = (event as PlayerJoinEvent).player
                buildPlayerEvent(p.name, p.location)
            }
        )
        EventTypeRegistry.register("onLeave",
            EventDescriptor(PlayerQuitEvent::class.java) { event ->
                val p = (event as PlayerQuitEvent).player
                buildPlayerEvent(p.name, p.location)
            }
        )
        EventTypeRegistry.register("onDeath",
            EventDescriptor(PlayerDeathEvent::class.java) { event ->
                val p = (event as PlayerDeathEvent).player
                buildPlayerEvent(p.name, p.location)
            }
        )
        EventTypeRegistry.register("onBreak",
            EventDescriptor(BlockBreakEvent::class.java) { event ->
                val e = event as BlockBreakEvent
                buildBlockEvent(e.player.name, e.block.location, e.block.type.name)
            }
        )
        EventTypeRegistry.register("onPlace",
            EventDescriptor(BlockPlaceEvent::class.java) { event ->
                val e = event as BlockPlaceEvent
                buildBlockEvent(e.player.name, e.block.location, e.block.type.name)
            }
        )
    }

    // 위치 정보 오브젝트: {"world": "...", "x": 0, "y": 0, "z": 0}
    private fun buildPosition(loc: org.bukkit.Location): FascriptValue.FObject =
        FascriptValue.FObject(mutableMapOf(
            "world" to FascriptValue.FString(loc.world?.name ?: ""),
            "x"     to FascriptValue.FNumber(loc.x),
            "y"     to FascriptValue.FNumber(loc.y),
            "z"     to FascriptValue.FNumber(loc.z)
        ))

    // 플레이어 이벤트 오브젝트: {"player": "...", "position": {...}}
    private fun buildPlayerEvent(name: String, loc: org.bukkit.Location): FascriptValue.FObject =
        FascriptValue.FObject(mutableMapOf(
            "player"   to FascriptValue.FString(name),
            "position" to buildPosition(loc)
        ))

    // 블록 이벤트 오브젝트: {"player": "...", "position": {...}, "block": "..."}
    private fun buildBlockEvent(
        name: String, loc: org.bukkit.Location, blockType: String
    ): FascriptValue.FObject =
        FascriptValue.FObject(mutableMapOf(
            "player"   to FascriptValue.FString(name),
            "position" to buildPosition(loc),
            "block"    to FascriptValue.FString(blockType)
        ))

    // 내장 함수를 등록합니다.
    // 새 내장 함수는 여기에 추가하면 됩니다.
    private fun registerBuiltins() {
        BuiltinRegistry.register("broadcast") { args, _ ->
            val msg = args.firstOrNull()?.toString() ?: ""
            Bukkit.broadcast(Component.text(msg))
            FascriptValue.FNull
        }

        BuiltinRegistry.register("message") { args, _ ->
            val msg = args.getOrNull(0)?.toString() ?: ""
            val playerName = args.getOrNull(1)?.toString() ?: ""
            Bukkit.getPlayerExact(playerName)?.sendMessage(Component.text(msg))
            FascriptValue.FNull
        }

        BuiltinRegistry.register("execute") { args, _ ->
            val cmd = args.getOrNull(0)?.toString() ?: return@register FascriptValue.FNull
            val playerName = args.getOrNull(1)?.toString()
            if (playerName != null) {
                Bukkit.getPlayerExact(playerName)?.performCommand(cmd)
            } else {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd)
            }
            FascriptValue.FNull
        }

        BuiltinRegistry.register("setblock") { args, _ ->
            val x = (args.getOrNull(0) as? FascriptValue.FNumber)?.v?.toInt() ?: return@register FascriptValue.FNull
            val y = (args.getOrNull(1) as? FascriptValue.FNumber)?.v?.toInt() ?: return@register FascriptValue.FNull
            val z = (args.getOrNull(2) as? FascriptValue.FNumber)?.v?.toInt() ?: return@register FascriptValue.FNull
            val block = args.getOrNull(3)?.toString() ?: return@register FascriptValue.FNull
            val world = Bukkit.getWorlds().firstOrNull() ?: return@register FascriptValue.FNull
            val location = org.bukkit.Location(world, x.toDouble(), y.toDouble(), z.toDouble())
            val blockData = Bukkit.createBlockData(if (block.contains(":")) block else "minecraft:$block")
            location.block.blockData = blockData
            FascriptValue.FNull
        }

        BuiltinRegistry.register("localSound") { args, _ ->
            val playerName = args.getOrNull(0)?.toString() ?: return@register FascriptValue.FNull
            val soundId = args.getOrNull(1)?.toString() ?: return@register FascriptValue.FNull
            val volume = (args.getOrNull(2) as? FascriptValue.FNumber)?.v?.toFloat() ?: 1.0f
            val pitch = (args.getOrNull(3) as? FascriptValue.FNumber)?.v?.toFloat() ?: 1.0f
            val player = Bukkit.getPlayerExact(playerName) ?: return@register FascriptValue.FNull
            player.playSound(player.location, soundId, org.bukkit.SoundCategory.MASTER, volume, pitch)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("worldSound") { args, _ ->
            val x = (args.getOrNull(0) as? FascriptValue.FNumber)?.v?.toDouble() ?: return@register FascriptValue.FNull
            val y = (args.getOrNull(1) as? FascriptValue.FNumber)?.v?.toDouble() ?: return@register FascriptValue.FNull
            val z = (args.getOrNull(2) as? FascriptValue.FNumber)?.v?.toDouble() ?: return@register FascriptValue.FNull
            val soundId = args.getOrNull(3)?.toString() ?: return@register FascriptValue.FNull
            val volume = (args.getOrNull(4) as? FascriptValue.FNumber)?.v?.toFloat() ?: 1.0f
            val pitch = (args.getOrNull(5) as? FascriptValue.FNumber)?.v?.toFloat() ?: 1.0f
            val world = Bukkit.getWorlds().firstOrNull() ?: return@register FascriptValue.FNull
            val location = org.bukkit.Location(world, x, y, z)
            world.playSound(location, soundId, org.bukkit.SoundCategory.MASTER, volume, pitch)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("intervalPause") { args, ctx ->
            val name = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            ctx.plugin.intervalManager.pause(name)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("intervalResume") { args, ctx ->
            val name = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            ctx.plugin.intervalManager.resume(name)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("intervalDestroy") { args, ctx ->
            val name = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            ctx.plugin.intervalManager.destroy(name)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("destroyListener") { args, ctx ->
            val name = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            ctx.plugin.eventManager.destroyListener(name)
            FascriptValue.FNull
        }

        // 전역 스토리지 (storage/global.yml)
        BuiltinRegistry.register("setGlobalStorage") { args, ctx ->
            val key   = args.getOrNull(0)?.toString() ?: return@register FascriptValue.FNull
            val value = args.getOrNull(1) ?: FascriptValue.FNull
            ctx.plugin.storageManager.set("global", key, value)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("getGlobalStorage") { args, ctx ->
            val key = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            ctx.plugin.storageManager.get("global", key)
        }

        // 스크립트별 스토리지 (storage/<scriptname>.yml)
        BuiltinRegistry.register("setStorage") { args, ctx ->
            val key   = args.getOrNull(0)?.toString() ?: return@register FascriptValue.FNull
            val value = args.getOrNull(1) ?: FascriptValue.FNull
            val name  = ctx.scriptCtx.scriptName.removeSuffix(".fst")
            ctx.plugin.storageManager.set(name, key, value)
            FascriptValue.FNull
        }

        BuiltinRegistry.register("getStorage") { args, ctx ->
            val key  = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            val name = ctx.scriptCtx.scriptName.removeSuffix(".fst")
            ctx.plugin.storageManager.get(name, key)
        }

        // 플레이어 관련 함수
        BuiltinRegistry.register("getAllPlayer") { _, _ ->
            val names = Bukkit.getOnlinePlayers().map { FascriptValue.FString(it.name) }
            FascriptValue.FList(names.toMutableList())
        }

        BuiltinRegistry.register("getPlayerUUID") { args, _ ->
            val playerName = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            val player = Bukkit.getPlayerExact(playerName) ?: return@register FascriptValue.FNull
            FascriptValue.FString(player.uniqueId.toString())
        }

        // 블록 데이터 조회: getBlockData(world, x, y, z)
        // 소숫점 좌표는 자동으로 정수로 보정합니다.
        // 반환: {"type","world","x","y","z","solid","passable","lightLevel","state":{...}}
        BuiltinRegistry.register("getBlockData") { args, _ ->
            val worldName = args.getOrNull(0)?.toString() ?: return@register FascriptValue.FNull
            val x = args.getOrNull(1)?.toNumber()?.toInt() ?: return@register FascriptValue.FNull
            val y = args.getOrNull(2)?.toNumber()?.toInt() ?: return@register FascriptValue.FNull
            val z = args.getOrNull(3)?.toNumber()?.toInt() ?: return@register FascriptValue.FNull

            val world = Bukkit.getWorld(worldName) ?: return@register FascriptValue.FNull
            val block = world.getBlockAt(x, y, z)
            val blockData = block.blockData

            // blockData.asString() 예시: "minecraft:oak_stairs[facing=north,half=bottom,waterlogged=false]"
            // 대괄호 안의 key=value 쌍을 FObject로 파싱합니다.
            val stateMap = mutableMapOf<String, FascriptValue>()
            val stateStr = blockData.asString
            val bracketStart = stateStr.indexOf('[')
            if (bracketStart != -1) {
                val props = stateStr.substring(bracketStart + 1, stateStr.lastIndexOf(']'))
                for (prop in props.split(',')) {
                    val eq = prop.indexOf('=')
                    if (eq != -1) {
                        stateMap[prop.substring(0, eq)] = FascriptValue.FString(prop.substring(eq + 1))
                    }
                }
            }

            FascriptValue.FObject(mutableMapOf(
                "type"       to FascriptValue.FString(block.type.name),
                "world"      to FascriptValue.FString(worldName),
                "x"          to FascriptValue.FNumber(x.toDouble()),
                "y"          to FascriptValue.FNumber(y.toDouble()),
                "z"          to FascriptValue.FNumber(z.toDouble()),
                "solid"      to FascriptValue.FBoolean(block.type.isSolid),
                "passable"   to FascriptValue.FBoolean(block.isPassable),
                "lightLevel" to FascriptValue.FNumber(block.lightLevel.toDouble()),
                "state"      to FascriptValue.FObject(stateMap)
            ))
        }

        // 엔티티 데이터 조회: getEntityData(UUID)
        // 반환: {"uuid","type","name","world","x","y","z","yaw","pitch","onGround","velocity":{...}}
        // LivingEntity 추가: "health","maxHealth","dead"
        // Player 추가: "gameMode","level","food","exp","flying","op"
        BuiltinRegistry.register("getEntityData") { args, _ ->
            val uuidStr = args.firstOrNull()?.toString() ?: return@register FascriptValue.FNull
            val uuid = try { UUID.fromString(uuidStr) } catch (_: IllegalArgumentException) {
                return@register FascriptValue.FNull
            }
            val entity = Bukkit.getEntity(uuid) ?: return@register FascriptValue.FNull
            val loc = entity.location

            val map = mutableMapOf<String, FascriptValue>(
                "uuid"     to FascriptValue.FString(entity.uniqueId.toString()),
                "type"     to FascriptValue.FString(entity.type.name),
                "name"     to FascriptValue.FString(entity.name),
                "world"    to FascriptValue.FString(entity.world.name),
                "x"        to FascriptValue.FNumber(loc.x),
                "y"        to FascriptValue.FNumber(loc.y),
                "z"        to FascriptValue.FNumber(loc.z),
                "yaw"      to FascriptValue.FNumber(loc.yaw.toDouble()),
                "pitch"    to FascriptValue.FNumber(loc.pitch.toDouble()),
                "onGround" to FascriptValue.FBoolean(entity.isOnGround),
                "velocity" to FascriptValue.FObject(mutableMapOf(
                    "x" to FascriptValue.FNumber(entity.velocity.x),
                    "y" to FascriptValue.FNumber(entity.velocity.y),
                    "z" to FascriptValue.FNumber(entity.velocity.z)
                ))
            )

            if (entity is LivingEntity) {
                @Suppress("DEPRECATION")
                map["health"]    = FascriptValue.FNumber(entity.health)
                @Suppress("DEPRECATION")
                map["maxHealth"] = FascriptValue.FNumber(entity.maxHealth)
                map["dead"]      = FascriptValue.FBoolean(entity.isDead)
            }

            if (entity is Player) {
                map["gameMode"] = FascriptValue.FString(entity.gameMode.name)
                map["level"]    = FascriptValue.FNumber(entity.level.toDouble())
                map["food"]     = FascriptValue.FNumber(entity.foodLevel.toDouble())
                map["exp"]      = FascriptValue.FNumber(entity.exp.toDouble())
                map["flying"]   = FascriptValue.FBoolean(entity.isFlying)
                map["op"]       = FascriptValue.FBoolean(entity.isOp)
            }

            FascriptValue.FObject(map)
        }
    }

    private fun registerCommands() {
        getCommand("fascript")?.let {
            it.setExecutor(FascriptCommand(this))
            it.tabCompleter = FascriptTabCompleter(this)
        }
    }
}
