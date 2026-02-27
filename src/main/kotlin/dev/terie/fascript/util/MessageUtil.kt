package dev.terie.fascript.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

// 일관된 메세지 형식을 제공합니다.
// BOLD를 사용하지 않으며, 오류는 빨간색, 경고는 노란색으로 표시합니다.
object MessageUtil {

    fun info(msg: String): Component =
        Component.text("[Fascript] $msg").color(NamedTextColor.WHITE)

    fun error(msg: String): Component =
        Component.text("[Fascript] $msg").color(NamedTextColor.RED)

    // 파싱 오류 진단 메세지 (줄 번호 및 소스 코드 포함)
    // isWarning = true 이면 YELLOW, false 이면 RED 로 표시합니다.
    //
    // 출력 예시:
    //
    // # 파싱 오류
    //    | 파일 이름 : script.fst
    // 03 | broadcast("hello"
    //    |
    //  ')' 토큰이 필요하지만 EOF(이)가 있습니다.
    //
    fun diagnostic(
        fileName: String,
        line: Int,
        sourceLine: String,
        title: String,
        message: String,
        isWarning: Boolean = false
    ): List<Component> {
        val accent = if (isWarning) NamedTextColor.YELLOW else NamedTextColor.RED
        val width = maxOf(2, line.toString().length)
        val lineNumStr = line.toString().padStart(width, '0')
        val pad = " ".repeat(width)

        return buildList {
            add(Component.empty())
            // 제목: # 제목
            add(Component.text("# $title").color(accent))
            // 헤더: "   | 파일 이름 : filename.fst"
            add(
                Component.text("$pad | ").color(NamedTextColor.AQUA)
                    .append(Component.text("파일 이름").color(NamedTextColor.YELLOW))
                    .append(Component.text(" : ").color(NamedTextColor.GRAY))
                    .append(Component.text(fileName).color(NamedTextColor.WHITE))
            )
            // 소스 줄: "03 | code" (라인 넘버는 여기 한 번만 표시)
            if (sourceLine.isNotBlank()) {
                add(
                    Component.text("$lineNumStr | ").color(NamedTextColor.AQUA)
                        .append(Component.text(sourceLine).color(NamedTextColor.WHITE))
                )
            }
            // 구분선: "   | "
            add(Component.text("$pad | ").color(NamedTextColor.AQUA))
            // 오류 설명 (한 칸 들여쓰기)
            for (msgLine in message.lines()) {
                add(Component.text(" $msgLine").color(accent))
            }
            add(Component.empty())
        }
    }

    // 런타임 오류 진단 메세지 (소스 코드 없음)
    // 리스너/인터벌처럼 소스 줄 정보를 알 수 없을 때 사용합니다.
    //
    // 출력 예시:
    //
    // # 런타임 오류
    //    | 파일 이름 : script.fst  (리스너/onJoin)
    //    |
    //  정의되지 않은 변수입니다: foo
    //
    fun runtimeDiagnostic(
        scriptName: String,
        context: String,
        title: String,
        message: String,
        isWarning: Boolean = false
    ): List<Component> {
        val accent = if (isWarning) NamedTextColor.YELLOW else NamedTextColor.RED
        val pad = "   "

        return buildList {
            add(Component.empty())
            // 제목: # 제목
            add(Component.text("# $title").color(accent))
            // 헤더: "   | 파일 이름 : filename.fst  (리스너/onJoin)"
            add(
                Component.text("$pad | ").color(NamedTextColor.AQUA)
                    .append(Component.text("파일 이름").color(NamedTextColor.YELLOW))
                    .append(Component.text(" : ").color(NamedTextColor.GRAY))
                    .append(Component.text(scriptName).color(NamedTextColor.WHITE))
                    .append(Component.text("  ($context)").color(NamedTextColor.GRAY))
            )
            // 구분선: "   | "
            add(Component.text("$pad | ").color(NamedTextColor.AQUA))
            // 오류 설명 (한 칸 들여쓰기)
            for (msgLine in message.lines()) {
                add(Component.text(" $msgLine").color(accent))
            }
            add(Component.empty())
        }
    }
}
