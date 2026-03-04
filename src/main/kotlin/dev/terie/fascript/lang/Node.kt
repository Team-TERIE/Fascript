package dev.terie.fascript.lang

// AST(추상 구문 트리) 노드의 최상위 타입입니다.
sealed class Node

// 프로그램 전체를 나타냅니다.
data class ProgramNode(val statements: List<Node>) : Node()

// 선언 범위 한정자: public은 모든 스크립트에서, private는 해당 스크립트에서만 접근 가능합니다.
enum class Scope { PUBLIC, PRIVATE }

// --- 선언문 ---

data class VarDeclNode(
    val typeName: String,   // "number", "string", "boolean", "list"
    val name: String,
    val initializer: Node,
    val scope: Scope = Scope.PRIVATE
) : Node()

data class FunctionDeclNode(
    val name: String,
    val params: List<String>,
    val body: List<Node>,
    val scope: Scope = Scope.PRIVATE
) : Node()

// [public|private] listener onJoin a (params) { ... }
data class ListenerDeclNode(
    val eventType: String,
    val name: String,
    val paramsName: String,
    val body: List<Node>,
    val scope: Scope = Scope.PRIVATE
) : Node()

// interval a (1000) { ... }
data class IntervalNode(
    val name: String,
    val millis: Node,
    val body: List<Node>,
    val scope: Scope = Scope.PRIVATE
) : Node()

// --- 실행문 ---

data class AssignNode(
    val name: String,
    val op: String,   // "=", "+=", "-=", "*=", "/=", "%="
    val value: Node
) : Node()

data class IndexAssignNode(
    val name: String,
    val index: Node,
    val op: String,
    val value: Node
) : Node()

// ++, -- 연산자
data class IncrDecrNode(val name: String, val op: String) : Node()

data class ReturnNode(val value: Node?) : Node()
class BreakNode : Node()

// --- 제어 흐름 ---

data class IfNode(
    val condition: Node,
    val thenBody: List<Node>,
    val elseIfClauses: List<Pair<Node, List<Node>>>,
    val elseBody: List<Node>?
) : Node()

data class ForeachNode(
    val itemName: String,
    val iterable: Node,
    val body: List<Node>
) : Node()

data class WhileNode(
    val condition: Node,
    val body: List<Node>
) : Node()

// thread (N) { ... } 병렬 실행 블록
data class ThreadNode(
    val channelCount: Node,
    val body: List<Node>
) : Node()

// --- 표현식 ---

data class NumberLiteralNode(val value: Double) : Node()
data class StringLiteralNode(val value: String) : Node()
data class BoolLiteralNode(val value: Boolean) : Node()
data class ListLiteralNode(val elements: List<Node>) : Node()
// {"key": value, ...} 오브젝트 리터럴
data class ObjectLiteralNode(val pairs: List<Pair<String, Node>>) : Node()
data class IdentifierNode(val name: String) : Node()
data class IndexAccessNode(val target: Node, val index: Node) : Node()
data class BinaryOpNode(val left: Node, val op: String, val right: Node) : Node()
data class UnaryOpNode(val op: String, val operand: Node) : Node()
data class CallNode(val name: String, val args: List<Node>) : Node()

// $"..." Query String 노드
data class QueryStringNode(val parts: List<QueryPart>) : Node()

// Query String의 구성 요소
sealed class QueryPart
data class LiteralPart(val text: String) : QueryPart()
data class ExprPart(val expr: Node) : QueryPart()
