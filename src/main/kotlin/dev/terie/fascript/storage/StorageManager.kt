package dev.terie.fascript.storage

import dev.terie.fascript.FascriptPlugin
import dev.terie.fascript.lang.FascriptValue
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

// 스크립트별 및 전역 영구 저장소를 관리합니다.
// plugins/TERIE-Fascript/storage/<name>.yml 로 저장됩니다.
class StorageManager(private val plugin: FascriptPlugin) {

    private val storageDir = File(plugin.dataFolder, "storage")

    init {
        if (!storageDir.exists()) storageDir.mkdirs()
    }

    // 키에 값을 저장합니다.
    fun set(storageName: String, key: String, value: FascriptValue) {
        val file = File(storageDir, "$storageName.yml")
        val config = if (file.exists()) YamlConfiguration.loadConfiguration(file)
                     else YamlConfiguration()
        config.set(key, toYaml(value))
        try {
            config.save(file)
        } catch (e: Exception) {
            plugin.logger.warning("[$storageName] 스토리지 저장 오류: ${e.message}")
        }
    }

    // 키의 값을 가져옵니다. 없으면 FNull을 반환합니다.
    fun get(storageName: String, key: String): FascriptValue {
        val file = File(storageDir, "$storageName.yml")
        if (!file.exists()) return FascriptValue.FNull
        return fromYaml(YamlConfiguration.loadConfiguration(file).get(key))
    }

    // FascriptValue를 YAML 저장 가능한 타입으로 변환합니다.
    private fun toYaml(value: FascriptValue): Any? = when (value) {
        is FascriptValue.FNumber  -> value.v
        is FascriptValue.FString  -> value.v
        is FascriptValue.FBoolean -> value.v
        is FascriptValue.FList    -> value.v.map { toYaml(it) }
        is FascriptValue.FObject  -> value.v.mapValues { toYaml(it.value) }
        is FascriptValue.FNull    -> null
    }

    // YAML 값을 FascriptValue로 변환합니다.
    private fun fromYaml(raw: Any?): FascriptValue = when (raw) {
        null       -> FascriptValue.FNull
        is Boolean -> FascriptValue.FBoolean(raw)
        is Int     -> FascriptValue.FNumber(raw.toDouble())
        is Long    -> FascriptValue.FNumber(raw.toDouble())
        is Double  -> FascriptValue.FNumber(raw)
        is Float   -> FascriptValue.FNumber(raw.toDouble())
        is String  -> FascriptValue.FString(raw)
        is List<*> -> FascriptValue.FList(raw.map { fromYaml(it) }.toMutableList())
        is Map<*, *> -> FascriptValue.FObject(
            raw.entries.associate { it.key.toString() to fromYaml(it.value) }.toMutableMap()
        )
        else       -> FascriptValue.FString(raw.toString())
    }
}
