package dev.terie.fascript.lang

// 렉서가 생성하는 토큰의 종류입니다.
enum class TokenType {
    // 리터럴
    NUMBER, STRING, QUERY_STRING, BOOLEAN,

    // 식별자
    IDENTIFIER,

    // 타입 키워드
    NUMBER_TYPE, STRING_TYPE, BOOLEAN_TYPE, LIST_TYPE, OBJECT_TYPE,

    // 범위 한정자
    PUBLIC, PRIVATE,

    // 선언 키워드
    FUNCTION, RETURN,

    // 제어 흐름 키워드
    IF, ELSE, FOREACH, IN, WHILE, BREAK,

    // 스크립트 구조 키워드
    LISTENER, INTERVAL, THREAD,

    // 연산자 - 산술
    PLUS, MINUS, STAR, SLASH, PERCENT,

    // 연산자 - 복합 할당
    PLUS_ASSIGN, MINUS_ASSIGN, STAR_ASSIGN, SLASH_ASSIGN, PERCENT_ASSIGN,

    // 연산자 - 증감
    PLUS_PLUS, MINUS_MINUS,

    // 연산자 - 할당 및 비교
    ASSIGN, EQ, NEQ, LT, GT, LTE, GTE,

    // 구분자
    LPAREN, RPAREN,
    LBRACE, RBRACE,
    LBRACKET, RBRACKET,
    COMMA, SEMICOLON, COLON,

    EOF
}

data class Token(
    val type: TokenType,
    val value: String,
    val line: Int
)
