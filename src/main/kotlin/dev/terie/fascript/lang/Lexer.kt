package dev.terie.fascript.lang

// 소스 코드 문자열을 토큰 리스트로 변환합니다.
class Lexer(private val source: String) {

    private var pos = 0
    private var line = 1
    private val tokens = mutableListOf<Token>()

    fun tokenize(): List<Token> {
        while (pos < source.length) {
            skipWhitespaceAndComments()
            if (pos >= source.length) break
            readNextToken()
        }
        tokens += Token(TokenType.EOF, "", line)
        return tokens
    }

    // 공백과 // 주석을 건너뜁니다.
    private fun skipWhitespaceAndComments() {
        while (pos < source.length) {
            when {
                source[pos] == '\n' -> { line++; pos++ }
                source[pos].isWhitespace() -> pos++
                pos + 1 < source.length && source[pos] == '/' && source[pos + 1] == '/' -> {
                    while (pos < source.length && source[pos] != '\n') pos++
                }
                else -> return
            }
        }
    }

    private fun readNextToken() {
        val c = source[pos]
        when {
            // $"..." 또는 $'...' 쿼리 문자열
            c == '$' && pos + 1 < source.length &&
                    (source[pos + 1] == '"' || source[pos + 1] == '\'') -> readQueryString()

            c == '"' || c == '\'' -> readString(c)
            c.isDigit() -> readNumber()
            c.isLetter() || c == '_' -> readIdentifierOrKeyword()
            else -> readSymbol()
        }
    }

    private fun readQueryString() {
        val startLine = line
        pos++ // $ 건너뜀
        val quote = source[pos++] // 여는 따옴표
        val sb = StringBuilder()
        var depth = 0

        while (pos < source.length) {
            val c = source[pos]
            if (c == '\n') line++

            if (depth == 0 && c == quote) {
                pos++ // 닫는 따옴표
                break
            }
            if (c == '{') depth++
            if (c == '}') depth--
            sb.append(c)
            pos++
        }
        tokens += Token(TokenType.QUERY_STRING, sb.toString(), startLine)
    }

    private fun readString(quote: Char) {
        val startLine = line
        pos++ // 여는 따옴표
        val sb = StringBuilder()

        while (pos < source.length && source[pos] != quote) {
            if (source[pos] == '\n') line++
            // 이스케이프 처리
            if (source[pos] == '\\' && pos + 1 < source.length) {
                pos++
                when (source[pos]) {
                    'n'  -> sb.append('\n')
                    't'  -> sb.append('\t')
                    '\\' -> sb.append('\\')
                    '"'  -> sb.append('"')
                    '\'' -> sb.append('\'')
                    'u'  -> {
                        val hex = source.substring(pos + 1, minOf(pos + 5, source.length))
                        val code = hex.toIntOrNull(16)
                        if (hex.length == 4 && code != null) {
                            sb.append(code.toChar())
                            pos += 4
                        } else {
                            sb.append('u')
                        }
                    }
                    else -> sb.append(source[pos])
                }
            } else {
                sb.append(source[pos])
            }
            pos++
        }
        pos++ // 닫는 따옴표
        tokens += Token(TokenType.STRING, sb.toString(), startLine)
    }

    private fun readNumber() {
        val startLine = line
        val start = pos
        while (pos < source.length && source[pos].isDigit()) pos++
        // 소수점 처리
        if (pos < source.length && source[pos] == '.' &&
            pos + 1 < source.length && source[pos + 1].isDigit()
        ) {
            pos++
            while (pos < source.length && source[pos].isDigit()) pos++
        }
        tokens += Token(TokenType.NUMBER, source.substring(start, pos), startLine)
    }

    private fun readIdentifierOrKeyword() {
        val startLine = line
        val start = pos
        while (pos < source.length && (source[pos].isLetterOrDigit() || source[pos] == '_')) pos++
        val word = source.substring(start, pos)
        val type = KEYWORDS[word] ?: TokenType.IDENTIFIER
        tokens += Token(type, word, startLine)
    }

    private fun readSymbol() {
        val startLine = line
        val c = source[pos]
        val next = if (pos + 1 < source.length) source[pos + 1] else '\u0000'

        val (type, len) = when {
            c == '+' && next == '+' -> Pair(TokenType.PLUS_PLUS, 2)
            c == '-' && next == '-' -> Pair(TokenType.MINUS_MINUS, 2)
            c == '+' && next == '=' -> Pair(TokenType.PLUS_ASSIGN, 2)
            c == '-' && next == '=' -> Pair(TokenType.MINUS_ASSIGN, 2)
            c == '*' && next == '=' -> Pair(TokenType.STAR_ASSIGN, 2)
            c == '/' && next == '=' -> Pair(TokenType.SLASH_ASSIGN, 2)
            c == '%' && next == '=' -> Pair(TokenType.PERCENT_ASSIGN, 2)
            c == '=' && next == '=' -> Pair(TokenType.EQ, 2)
            c == '!' && next == '=' -> Pair(TokenType.NEQ, 2)
            c == '<' && next == '=' -> Pair(TokenType.LTE, 2)
            c == '>' && next == '=' -> Pair(TokenType.GTE, 2)
            c == '<' -> Pair(TokenType.LT, 1)
            c == '>' -> Pair(TokenType.GT, 1)
            c == '=' -> Pair(TokenType.ASSIGN, 1)
            c == '+' -> Pair(TokenType.PLUS, 1)
            c == '-' -> Pair(TokenType.MINUS, 1)
            c == '*' -> Pair(TokenType.STAR, 1)
            c == '/' -> Pair(TokenType.SLASH, 1)
            c == '%' -> Pair(TokenType.PERCENT, 1)
            c == '(' -> Pair(TokenType.LPAREN, 1)
            c == ')' -> Pair(TokenType.RPAREN, 1)
            c == '{' -> Pair(TokenType.LBRACE, 1)
            c == '}' -> Pair(TokenType.RBRACE, 1)
            c == '[' -> Pair(TokenType.LBRACKET, 1)
            c == ']' -> Pair(TokenType.RBRACKET, 1)
            c == ',' -> Pair(TokenType.COMMA, 1)
            c == ';' -> Pair(TokenType.SEMICOLON, 1)
            c == ':' -> Pair(TokenType.COLON, 1)
            else -> throw FascriptParseError("$line 번째 줄: 알 수 없는 문자 '$c'")
        }

        tokens += Token(type, source.substring(pos, pos + len), startLine)
        pos += len
    }

    companion object {
        private val KEYWORDS = mapOf(
            "public"  to TokenType.PUBLIC,
            "private" to TokenType.PRIVATE,
            "number"  to TokenType.NUMBER_TYPE,
            "string"  to TokenType.STRING_TYPE,
            "boolean" to TokenType.BOOLEAN_TYPE,
            "list"    to TokenType.LIST_TYPE,
            "object"  to TokenType.OBJECT_TYPE,
            "function" to TokenType.FUNCTION,
            "return"  to TokenType.RETURN,
            "if"      to TokenType.IF,
            "else"    to TokenType.ELSE,
            "foreach" to TokenType.FOREACH,
            "in"      to TokenType.IN,
            "while"   to TokenType.WHILE,
            "break"   to TokenType.BREAK,
            "listener" to TokenType.LISTENER,
            "interval" to TokenType.INTERVAL,
            "thread"  to TokenType.THREAD,
            "true"    to TokenType.BOOLEAN,
            "false"   to TokenType.BOOLEAN,
        )
    }
}
