package dev.terie.fascript.lang

import dev.terie.fascript.script.GlobalRegistry
import dev.terie.fascript.script.ScriptContext

// AST를 실행하는 인터프리터입니다.
class Interpreter(
    private val context: ScriptContext,
    private val scriptArgs: List<FascriptValue> = emptyList()
) {

    // 스코프 스택. 가장 앞이 현재 스코프입니다.
    // context.globalScope를 최상위 스코프로 공유하여 인터벌 간 변수 상태를 유지합니다.
    private val scopeStack = ArrayDeque<MutableMap<String, FascriptValue>>().also {
        it.addFirst(context.globalScope)
    }

    // 프로그램을 실행합니다.
    // 함수/리스너/인터벌 선언은 먼저 등록 후, 실행 가능한 구문만 순서대로 실행합니다.
    fun execute(program: ProgramNode) {
        // 1패스: 함수, 리스너, 인터벌을 미리 등록합니다. (선언 순서와 무관하게 호출 가능)
        for (stmt in program.statements) {
            when (stmt) {
                is FunctionDeclNode -> {
                    context.registerFunction(stmt)
                    // public 함수는 전역 레지스트리에도 등록합니다.
                    if (stmt.scope == Scope.PUBLIC) GlobalRegistry.functions[stmt.name] = stmt
                }
                is ListenerDeclNode -> {
                    // 리스너 이름을 변수로 등록하여 destroyListener(a) 에서 사용할 수 있습니다.
                    context.globalScope[stmt.name] = FascriptValue.FString(stmt.name)
                    if (stmt.scope == Scope.PUBLIC) GlobalRegistry.variables[stmt.name] = FascriptValue.FString(stmt.name)
                    context.registerListener(stmt)
                }
                is IntervalNode     -> {
                    // 인터벌 이름을 변수로 등록하여 intervalPause 등에서 사용할 수 있습니다.
                    context.globalScope[stmt.name] = FascriptValue.FString(stmt.name)
                    if (stmt.scope == Scope.PUBLIC) GlobalRegistry.variables[stmt.name] = FascriptValue.FString(stmt.name)
                    context.registerInterval(stmt, this)
                }
                else -> Unit
            }
        }
        // 2패스: 실행 가능한 구문을 실행합니다.
        val executable = program.statements.filter {
            it !is FunctionDeclNode && it !is ListenerDeclNode && it !is IntervalNode
        }
        executeStatements(executable)
    }

    // 문 목록을 순서대로 실행합니다.
    // delay() 호출 시 DelaySignal을 던져 남은 구문들을 지연 실행 예약합니다.
    fun executeStatements(statements: List<Node>, startIndex: Int = 0) {
        for (i in startIndex until statements.size) {
            val stmt = statements[i]
            // delay는 특수 제어 흐름으로 처리합니다.
            if (stmt is CallNode && stmt.name == "delay") {
                val ms = evalExpr(stmt.args.firstOrNull()
                    ?: throw FascriptRuntimeError("delay()에는 밀리초 인자가 필요합니다.")).toNumber().toLong()
                throw DelaySignal(ms, statements.drop(i + 1))
            }
            executeStatement(stmt)
        }
    }

    // 단일 구문을 실행합니다.
    fun executeStatement(node: Node) {
        when (node) {
            is VarDeclNode   -> executeVarDecl(node)
            is AssignNode    -> executeAssign(node)
            is IndexAssignNode -> executeIndexAssign(node)
            is IncrDecrNode  -> executeIncrDecr(node)
            is IfNode        -> executeIf(node)
            is WhileNode     -> executeWhile(node)
            is ForeachNode   -> executeForeach(node)
            is ReturnNode    -> throw ReturnSignal(node.value?.let { evalExpr(it) } ?: FascriptValue.FNull)
            is BreakNode     -> throw BreakSignal()
            is CallNode      -> executeCall(node)
            else             -> evalExpr(node) // 표현식을 구문으로 사용
        }
    }

    // 식을 평가하여 FascriptValue를 반환합니다.
    fun evalExpr(node: Node): FascriptValue {
        return when (node) {
            is NumberLiteralNode -> FascriptValue.FNumber(node.value)
            is StringLiteralNode -> FascriptValue.FString(node.value)
            is BoolLiteralNode   -> FascriptValue.FBoolean(node.value)
            is ListLiteralNode   -> FascriptValue.FList(node.elements.map { evalExpr(it) }.toMutableList())
            is ObjectLiteralNode -> FascriptValue.FObject(
                node.pairs.associate { (k, v) -> k to evalExpr(v) }.toMutableMap()
            )
            is QueryStringNode   -> evalQueryString(node)
            is IdentifierNode    -> lookupVar(node.name)
            is IndexAccessNode   -> evalIndexAccess(node)
            is BinaryOpNode      -> evalBinaryOp(node)
            is UnaryOpNode       -> evalUnary(node)
            is CallNode          -> executeCall(node)
            else -> throw FascriptRuntimeError("평가할 수 없는 노드입니다: ${node::class.simpleName}")
        }
    }

    // --- 변수 ---

    private fun executeVarDecl(node: VarDeclNode) {
        val value = evalExpr(node.initializer)
        scopeStack.first()[node.name] = value
        // public 변수는 전역 레지스트리에도 등록합니다.
        if (node.scope == Scope.PUBLIC) GlobalRegistry.variables[node.name] = value
    }

    private fun executeAssign(node: AssignNode) {
        val current = if (node.op == "=") FascriptValue.FNull else lookupVar(node.name)
        val rhs = evalExpr(node.value)
        setVar(node.name, applyAssignOp(node.op, current, rhs))
    }

    // 할당 연산자를 적용하여 결과 값을 반환합니다.
    private fun applyAssignOp(op: String, current: FascriptValue, rhs: FascriptValue): FascriptValue =
        when (op) {
            "="  -> rhs
            "+=" -> applyAdd(current, rhs)
            "-=" -> FascriptValue.FNumber(current.toNumber() - rhs.toNumber())
            "*=" -> FascriptValue.FNumber(current.toNumber() * rhs.toNumber())
            "/=" -> FascriptValue.FNumber(current.toNumber() / rhs.toNumber())
            "%=" -> FascriptValue.FNumber(current.toNumber() % rhs.toNumber())
            else -> throw FascriptRuntimeError("알 수 없는 할당 연산자: $op")
        }

    private fun executeIndexAssign(node: IndexAssignNode) {
        val target = lookupVar(node.name)
        val indexVal = evalExpr(node.index)
        val rhs = evalExpr(node.value)

        when (target) {
            is FascriptValue.FObject -> {
                val key = indexVal.toString()
                val current = target.v[key] ?: FascriptValue.FNull
                target.v[key] = applyAssignOp(node.op, current, rhs)
                return
            }
            is FascriptValue.FList -> {
                val index = indexVal.toNumber().toInt()
                val current = target.v.getOrNull(index) ?: FascriptValue.FNull
                if (index < target.v.size) {
                    target.v[index] = applyAssignOp(node.op, current, rhs)
                } else {
                    throw FascriptRuntimeError("리스트 인덱스 범위를 벗어났습니다: $index")
                }
                return
            }
            else -> throw FascriptRuntimeError("${node.name}은(는) 오브젝트 또는 리스트가 아닙니다.")
        }
    }

    private fun executeIncrDecr(node: IncrDecrNode) {
        val current = lookupVar(node.name)
        val result = when (node.op) {
            "++" -> FascriptValue.FNumber(current.toNumber() + 1)
            "--" -> FascriptValue.FNumber(current.toNumber() - 1)
            else -> throw FascriptRuntimeError("알 수 없는 증감 연산자: ${node.op}")
        }
        setVar(node.name, result)
    }

    // --- 제어 흐름 ---

    private fun executeIf(node: IfNode) {
        if (evalExpr(node.condition).isTruthy()) {
            executeBlock(node.thenBody)
            return
        }
        for ((cond, body) in node.elseIfClauses) {
            if (evalExpr(cond).isTruthy()) {
                executeBlock(body)
                return
            }
        }
        node.elseBody?.let { executeBlock(it) }
    }

    private fun executeWhile(node: WhileNode) {
        try {
            while (evalExpr(node.condition).isTruthy()) {
                try {
                    executeBlock(node.body)
                } catch (_: BreakSignal) {
                    return
                }
            }
        } catch (_: BreakSignal) {
            // while 외부의 break 처리
        }
    }

    private fun executeForeach(node: ForeachNode) {
        val iterable = evalExpr(node.iterable) as? FascriptValue.FList
            ?: throw FascriptRuntimeError("foreach는 리스트에만 사용할 수 있습니다.")
        pushScope()
        try {
            for (item in iterable.v) {
                scopeStack.first()[node.itemName] = item
                try {
                    executeStatements(node.body)
                } catch (_: BreakSignal) {
                    return
                }
            }
        } finally {
            popScope()
        }
    }

    // 블록을 새 스코프에서 실행합니다.
    private fun executeBlock(statements: List<Node>) {
        pushScope()
        try {
            executeStatements(statements)
        } finally {
            popScope()
        }
    }

    // --- 함수 호출 ---

    private fun executeCall(node: CallNode): FascriptValue {
        val evaluatedArgs = node.args.map { evalExpr(it) }
        val builtinCtx = BuiltinContext(context.plugin, context)

        // 내장 함수 우선 확인
        BuiltinRegistry.get(node.name)?.let { return it(evaluatedArgs, builtinCtx) }

        // 사용자 정의 함수 확인 (로컬 우선, 없으면 public 전역 함수 조회)
        val funcDecl = context.getFunction(node.name)
            ?: GlobalRegistry.functions[node.name]
            ?: throw FascriptRuntimeError("함수를 찾을 수 없습니다: ${node.name}")
        return callUserFunction(funcDecl, evaluatedArgs)
    }

    private fun callUserFunction(decl: FunctionDeclNode, args: List<FascriptValue>): FascriptValue {
        pushScope()
        decl.params.forEachIndexed { i, param ->
            scopeStack.first()[param] = args.getOrElse(i) { FascriptValue.FNull }
        }
        return try {
            executeStatements(decl.body)
            FascriptValue.FNull
        } catch (ret: ReturnSignal) {
            ret.value
        } finally {
            popScope()
        }
    }

    // --- 표현식 평가 ---

    private fun evalIndexAccess(node: IndexAccessNode): FascriptValue {
        val indexVal = evalExpr(node.index)
        return when (val target = evalExpr(node.target)) {
            is FascriptValue.FObject -> target.v[indexVal.toString()] ?: FascriptValue.FNull
            is FascriptValue.FList   -> target.v.getOrNull(indexVal.toNumber().toInt()) ?: FascriptValue.FNull
            is FascriptValue.FString -> {
                val ch = target.v.getOrNull(indexVal.toNumber().toInt())
                    ?: return FascriptValue.FNull
                FascriptValue.FString(ch.toString())
            }
            else -> throw FascriptRuntimeError("인덱스 접근은 오브젝트, 리스트, 문자열에만 사용할 수 있습니다.")
        }
    }

    private fun evalBinaryOp(node: BinaryOpNode): FascriptValue {
        val left = evalExpr(node.left)
        val right = evalExpr(node.right)
        return when (node.op) {
            "+"  -> applyAdd(left, right)
            "-"  -> FascriptValue.FNumber(left.toNumber() - right.toNumber())
            "*"  -> FascriptValue.FNumber(left.toNumber() * right.toNumber())
            "/"  -> FascriptValue.FNumber(left.toNumber() / right.toNumber())
            "%"  -> FascriptValue.FNumber(left.toNumber() % right.toNumber())
            "==" -> FascriptValue.FBoolean(valuesEqual(left, right))
            "!=" -> FascriptValue.FBoolean(!valuesEqual(left, right))
            "<"  -> FascriptValue.FBoolean(left.toNumber() < right.toNumber())
            ">"  -> FascriptValue.FBoolean(left.toNumber() > right.toNumber())
            "<=" -> FascriptValue.FBoolean(left.toNumber() <= right.toNumber())
            ">=" -> FascriptValue.FBoolean(left.toNumber() >= right.toNumber())
            else -> throw FascriptRuntimeError("알 수 없는 연산자: ${node.op}")
        }
    }

    private fun applyAdd(left: FascriptValue, right: FascriptValue): FascriptValue {
        // 하나라도 문자열이면 문자열 연결로 처리합니다.
        return if (left is FascriptValue.FString || right is FascriptValue.FString) {
            FascriptValue.FString(left.toString() + right.toString())
        } else {
            FascriptValue.FNumber(left.toNumber() + right.toNumber())
        }
    }

    private fun valuesEqual(a: FascriptValue, b: FascriptValue): Boolean {
        return when {
            a is FascriptValue.FNumber  && b is FascriptValue.FNumber  -> a.v == b.v
            a is FascriptValue.FString  && b is FascriptValue.FString  -> a.v == b.v
            a is FascriptValue.FBoolean && b is FascriptValue.FBoolean -> a.v == b.v
            a is FascriptValue.FNull    && b is FascriptValue.FNull    -> true
            else -> false
        }
    }

    private fun evalUnary(node: UnaryOpNode): FascriptValue {
        return when (node.op) {
            "-" -> FascriptValue.FNumber(-evalExpr(node.operand).toNumber())
            else -> throw FascriptRuntimeError("알 수 없는 단항 연산자: ${node.op}")
        }
    }

    private fun evalQueryString(node: QueryStringNode): FascriptValue {
        val sb = StringBuilder()
        for (part in node.parts) {
            when (part) {
                is LiteralPart -> sb.append(part.text)
                is ExprPart    -> sb.append(evalExpr(part.expr).toString())
            }
        }
        return FascriptValue.FString(sb.toString())
    }

    // --- 스코프 관리 ---

    private fun pushScope() = scopeStack.addFirst(mutableMapOf())

    private fun popScope() {
        if (scopeStack.size > 1) scopeStack.removeFirst()
    }

    fun setGlobalVar(name: String, value: FascriptValue) {
        context.globalScope[name] = value
    }

    private fun setVar(name: String, value: FascriptValue) {
        // 기존 스코프에서 먼저 찾아 업데이트합니다.
        for (scope in scopeStack) {
            if (scope.containsKey(name)) {
                scope[name] = value
                return
            }
        }
        // 없으면 현재 스코프에 추가합니다.
        scopeStack.first()[name] = value
    }

    private fun lookupVar(name: String): FascriptValue {
        // args는 예약 식별자로 스크립트 실행 인자를 반환합니다.
        if (name == "args") return FascriptValue.FList(scriptArgs.toMutableList())
        for (scope in scopeStack) {
            scope[name]?.let { return it }
        }
        // 로컬 스코프에 없으면 public 전역 변수를 조회합니다.
        GlobalRegistry.variables[name]?.let { return it }
        throw FascriptRuntimeError("정의되지 않은 변수입니다: $name")
    }
}
