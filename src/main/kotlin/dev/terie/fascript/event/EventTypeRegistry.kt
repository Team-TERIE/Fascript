package dev.terie.fascript.event

import dev.terie.fascript.lang.FascriptValue
import org.bukkit.event.Event

// 하나의 Fascript 이벤트 타입을 설명합니다.
data class EventDescriptor(
    // 대응하는 Paper 이벤트 클래스
    val eventClass: Class<out Event>,
    // 이벤트에서 event 오브젝트를 추출하는 람다
    val paramsExtractor: (Event) -> FascriptValue
)

// 지원하는 이벤트 타입들을 관리합니다.
// 새 이벤트는 register()로 등록하면 소스 수정 없이 확장할 수 있습니다.
object EventTypeRegistry {

    private val registry = mutableMapOf<String, EventDescriptor>()

    fun register(typeName: String, descriptor: EventDescriptor) {
        registry[typeName] = descriptor
    }

    fun get(typeName: String): EventDescriptor? = registry[typeName]

    fun has(typeName: String): Boolean = typeName in registry
}
