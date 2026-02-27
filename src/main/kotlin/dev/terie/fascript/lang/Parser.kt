package dev.terie.fascript.lang

// 토큰 리스트를 AST로 변환하는 재귀 하강 파서입니다.
class Parser(private val tokens: List<Token>) {

    private var pos = 0

    private val current get() = tokens[pos]
    private val currentType get() = tokens[pos].type

    fun parse(): ProgramNode {
        val statements = mutableListOf<Node>()
        while (currentType != TokenType.EOF) {
            skipSemicolons()
            if (currentType == TokenType.EOF) break
            statements += parseStatement()
            skipSemicolons()
        }
        return ProgramNode(statements)
    }

    private fun skipSemicolons() {
        while (currentType == TokenType.SEMICOLON) advance()
    }

    private fun parseStatement(): Node {
        // public / private 범위 한정자를 먼저 확인합니다.
        val scope = when (currentType) {
            TokenType.PUBLIC  -> { advance(); Scope.PUBLIC }
            TokenType.PRIVATE -> { advance(); Scope.PRIVATE }
            else              -> null
        }

        return when (currentType) {
            TokenType.NUMBER_TYPE,
            TokenType.STRING_TYPE,
            TokenType.BOOLEAN_TYPE,
            TokenType.LIST_TYPE,
            TokenType.OBJECT_TYPE -> parseVarDecl(scope ?: Scope.PRIVATE)
            TokenType.FUNCTION  -> parseFunctionDecl(scope ?: Scope.PRIVATE)
            TokenType.LISTENER  -> parseListenerDecl(scope ?: Scope.PRIVATE)
            TokenType.INTERVAL  -> parseIntervalDecl(scope ?: Scope.PRIVATE)
            TokenType.IF        -> parseIf()
            TokenType.FOREACH   -> parseForeach()
            TokenType.WHILE     -> parseWhile()
            TokenType.RETURN    -> parseReturn()
            TokenType.BREAK     -> { advance(); BreakNode() }
            TokenType.IDENTIFIER -> parseIdentifierStatement()
            else                -> parseExpression()
        }
    }

    // IDENTIFIER로 시작하는 구문: 할당, 증감, 함수 호출 중 하나입니다.
    private fun parseIdentifierStatement(): Node {
        val name = advance().value

        return when (currentType) {
            // 인덱스 할당: name[index] = value
            TokenType.LBRACKET -> {
                advance()
                val index = parseExpression()
                expect(TokenType.RBRACKET)
                val op = parseAssignOp()
                IndexAssignNode(name, index, op, parseExpression())
            }
            // 일반 할당
            TokenType.ASSIGN,
            TokenType.PLUS_ASSIGN,
            TokenType.MINUS_ASSIGN,
            TokenType.STAR_ASSIGN,
            TokenType.SLASH_ASSIGN,
            TokenType.PERCENT_ASSIGN -> {
                val op = parseAssignOp()
                AssignNode(name, op, parseExpression())
            }
            // 증감
            TokenType.PLUS_PLUS  -> { advance(); IncrDecrNode(name, "++") }
            TokenType.MINUS_MINUS -> { advance(); IncrDecrNode(name, "--") }
            // 함수 호출
            TokenType.LPAREN -> {
                advance()
                val args = parseArgList()
                expect(TokenType.RPAREN)
                CallNode(name, args)
            }
            // 그 외: 식별자를 표현식의 시작으로 처리
            else -> {
                // 식별자를 스택에 되돌리고 표현식으로 파싱
                pos--
                parseExpression()
            }
        }
    }

    private fun parseAssignOp(): String {
        val op = when (currentType) {
            TokenType.ASSIGN        -> "="
            TokenType.PLUS_ASSIGN   -> "+="
            TokenType.MINUS_ASSIGN  -> "-="
            TokenType.STAR_ASSIGN   -> "*="
            TokenType.SLASH_ASSIGN  -> "/="
            TokenType.PERCENT_ASSIGN -> "%="
            else -> throw FascriptParseError("할당 연산자가 필요합니다.", current.line)
        }
        advance()
        return op
    }

    private fun parseVarDecl(scope: Scope = Scope.PRIVATE): VarDeclNode {
        val typeName = when (currentType) {
            TokenType.NUMBER_TYPE  -> { advance(); "number" }
            TokenType.STRING_TYPE  -> { advance(); "string" }
            TokenType.BOOLEAN_TYPE -> { advance(); "boolean" }
            TokenType.LIST_TYPE    -> { advance(); "list" }
            TokenType.OBJECT_TYPE  -> { advance(); "object" }
            else -> throw FascriptParseError("타입 키워드(number, string, boolean, list, object)가 필요합니다.", current.line)
        }
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.ASSIGN)
        val init = parseExpression()
        return VarDeclNode(typeName, name, init, scope)
    }

    private fun parseFunctionDecl(scope: Scope = Scope.PRIVATE): FunctionDeclNode {
        expect(TokenType.FUNCTION)
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LPAREN)
        val params = mutableListOf<String>()
        if (currentType != TokenType.RPAREN) {
            params += expect(TokenType.IDENTIFIER).value
            while (currentType == TokenType.COMMA) {
                advance()
                params += expect(TokenType.IDENTIFIER).value
            }
        }
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return FunctionDeclNode(name, params, body, scope)
    }

    // [public|private] listener onJoin a (params) { ... }
    private fun parseListenerDecl(scope: Scope = Scope.PRIVATE): ListenerDeclNode {
        expect(TokenType.LISTENER)
        val eventType = expect(TokenType.IDENTIFIER).value
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LPAREN)
        val paramsName = expect(TokenType.IDENTIFIER).value
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return ListenerDeclNode(eventType, name, paramsName, body, scope)
    }

    private fun parseIntervalDecl(scope: Scope = Scope.PRIVATE): IntervalNode {
        expect(TokenType.INTERVAL)
        val name = expect(TokenType.IDENTIFIER).value
        expect(TokenType.LPAREN)
        val millis = parseExpression()
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return IntervalNode(name, millis, body, scope)
    }

    private fun parseIf(): IfNode {
        expect(TokenType.IF)
        expect(TokenType.LPAREN)
        val condition = parseExpression()
        expect(TokenType.RPAREN)
        val thenBody = parseBlock()

        val elseIfClauses = mutableListOf<Pair<Node, List<Node>>>()
        var elseBody: List<Node>? = null

        while (currentType == TokenType.ELSE) {
            advance()
            if (currentType == TokenType.IF) {
                advance()
                expect(TokenType.LPAREN)
                val cond = parseExpression()
                expect(TokenType.RPAREN)
                elseIfClauses += Pair(cond, parseBlock())
            } else {
                elseBody = parseBlock()
                break
            }
        }

        return IfNode(condition, thenBody, elseIfClauses, elseBody)
    }

    private fun parseForeach(): ForeachNode {
        expect(TokenType.FOREACH)
        expect(TokenType.LPAREN)
        val itemName = expect(TokenType.IDENTIFIER).value
        expect(TokenType.IN)
        val iterable = parseExpression()
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return ForeachNode(itemName, iterable, body)
    }

    private fun parseWhile(): WhileNode {
        expect(TokenType.WHILE)
        expect(TokenType.LPAREN)
        val condition = parseExpression()
        expect(TokenType.RPAREN)
        val body = parseBlock()
        return WhileNode(condition, body)
    }

    private fun parseReturn(): ReturnNode {
        expect(TokenType.RETURN)
        val value = if (currentType != TokenType.RBRACE && currentType != TokenType.EOF
            && currentType != TokenType.SEMICOLON) {
            parseExpression()
        } else null
        return ReturnNode(value)
    }

    private fun parseBlock(): List<Node> {
        expect(TokenType.LBRACE)
        val stmts = mutableListOf<Node>()
        while (currentType != TokenType.RBRACE && currentType != TokenType.EOF) {
            skipSemicolons()
            if (currentType == TokenType.RBRACE) break
            stmts += parseStatement()
            skipSemicolons()
        }
        expect(TokenType.RBRACE)
        return stmts
    }

    // 비교 연산자 우선순위 (가장 낮음)
    private fun parseExpression(): Node = parseComparison()

    private fun parseComparison(): Node {
        var left = parseAdditive()
        while (currentType in COMPARISON_OPS) {
            val op = advance().value
            left = BinaryOpNode(left, op, parseAdditive())
        }
        return left
    }

    private fun parseAdditive(): Node {
        var left = parseMultiplicative()
        while (currentType == TokenType.PLUS || currentType == TokenType.MINUS) {
            val op = advance().value
            left = BinaryOpNode(left, op, parseMultiplicative())
        }
        return left
    }

    private fun parseMultiplicative(): Node {
        var left = parseUnary()
        while (currentType == TokenType.STAR ||
               currentType == TokenType.SLASH ||
               currentType == TokenType.PERCENT) {
            val op = advance().value
            left = BinaryOpNode(left, op, parseUnary())
        }
        return left
    }

    private fun parseUnary(): Node {
        if (currentType == TokenType.MINUS) {
            advance()
            return UnaryOpNode("-", parseUnary())
        }
        return parsePostfix(parsePrimary())
    }

    // 식별자 뒤에 오는 [] 인덱스 접근을 처리합니다.
    private fun parsePostfix(base: Node): Node {
        var node = base
        while (true) {
            node = when (currentType) {
                TokenType.LBRACKET -> {
                    advance()
                    val index = parseExpression()
                    expect(TokenType.RBRACKET)
                    IndexAccessNode(node, index)
                }
                else -> return node
            }
        }
    }

    private fun parsePrimary(): Node {
        return when (currentType) {
            TokenType.NUMBER  -> NumberLiteralNode(advance().value.toDouble())
            TokenType.STRING  -> StringLiteralNode(advance().value)
            TokenType.BOOLEAN -> BoolLiteralNode(advance().value == "true")

            TokenType.QUERY_STRING -> {
                val raw = advance().value
                parseQueryString(raw)
            }

            TokenType.LBRACKET -> {
                advance()
                val elements = mutableListOf<Node>()
                if (currentType != TokenType.RBRACKET) {
                    elements += parseExpression()
                    while (currentType == TokenType.COMMA) {
                        advance()
                        elements += parseExpression()
                    }
                }
                expect(TokenType.RBRACKET)
                ListLiteralNode(elements)
            }

            // {"key": value, ...} 오브젝트 리터럴
            TokenType.LBRACE -> {
                advance()
                val pairs = mutableListOf<Pair<String, Node>>()
                if (currentType != TokenType.RBRACE) {
                    fun parseKey(): String = when (currentType) {
                        TokenType.STRING     -> advance().value
                        TokenType.IDENTIFIER -> advance().value
                        else -> throw FascriptParseError(
                            "오브젝트 키는 문자열 또는 식별자여야 합니다.", current.line)
                    }
                    pairs += Pair(parseKey(), run { expect(TokenType.COLON); parseExpression() })
                    while (currentType == TokenType.COMMA) {
                        advance()
                        if (currentType == TokenType.RBRACE) break // trailing comma 허용
                        pairs += Pair(parseKey(), run { expect(TokenType.COLON); parseExpression() })
                    }
                }
                expect(TokenType.RBRACE)
                ObjectLiteralNode(pairs)
            }

            TokenType.LPAREN -> {
                advance()
                val expr = parseExpression()
                expect(TokenType.RPAREN)
                expr
            }

            TokenType.IDENTIFIER -> {
                val name = advance().value
                if (currentType == TokenType.LPAREN) {
                    advance()
                    val args = parseArgList()
                    expect(TokenType.RPAREN)
                    CallNode(name, args)
                } else {
                    IdentifierNode(name)
                }
            }

            else -> throw FascriptParseError(
                "예상치 못한 토큰 '${current.value}'", current.line
            )
        }
    }

    private fun parseArgList(): List<Node> {
        if (currentType == TokenType.RPAREN) return emptyList()
        val args = mutableListOf(parseExpression())
        while (currentType == TokenType.COMMA) {
            advance()
            args += parseExpression()
        }
        return args
    }

    // $"..." 쿼리 문자열의 {식} 부분을 재귀적으로 파싱합니다.
    private fun parseQueryString(raw: String): QueryStringNode {
        val parts = mutableListOf<QueryPart>()
        var i = 0
        val sb = StringBuilder()

        while (i < raw.length) {
            if (raw[i] == '{') {
                // 리터럴 부분을 먼저 저장합니다.
                if (sb.isNotEmpty()) {
                    parts += LiteralPart(sb.toString())
                    sb.clear()
                }
                // 닫는 중괄호를 찾습니다. (중첩 고려)
                var depth = 1
                i++
                val exprStart = i
                while (i < raw.length && depth > 0) {
                    if (raw[i] == '{') depth++
                    if (raw[i] == '}') depth--
                    if (depth > 0) i++
                }
                val exprSource = raw.substring(exprStart, i)
                i++ // 닫는 }

                // 식을 별도 파서로 파싱합니다.
                val exprTokens = Lexer(exprSource).tokenize()
                val exprNode = Parser(exprTokens).parseExpression()
                parts += ExprPart(exprNode)
            } else {
                sb.append(raw[i])
                i++
            }
        }
        if (sb.isNotEmpty()) parts += LiteralPart(sb.toString())
        return QueryStringNode(parts)
    }

    private fun advance(): Token {
        val tok = tokens[pos]
        if (pos < tokens.size - 1) pos++
        return tok
    }

    private fun expect(type: TokenType): Token {
        if (currentType != type) {
            throw FascriptParseError(
                "'${type.name}' 토큰이 필요하지만 '${current.value}'(이)가 있습니다.", current.line
            )
        }
        return advance()
    }

    companion object {
        private val COMPARISON_OPS = setOf(
            TokenType.EQ, TokenType.NEQ,
            TokenType.LT, TokenType.GT,
            TokenType.LTE, TokenType.GTE
        )
    }
}
