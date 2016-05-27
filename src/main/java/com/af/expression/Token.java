package com.af.expression;

public class Token {
	// Token类型
	public TokenType Type;

	// Token值
	public Object Value;

	// 单词在串中的起始位置，包括空格
	public int StartPos;

	public Token(TokenType type, Object value, int startPos) {
		Type = type;
		Value = value;
		StartPos = startPos;
	}

	public String toString() {
		return Type.toString() + Value;
	}
}
