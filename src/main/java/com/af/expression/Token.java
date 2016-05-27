package com.af.expression;

public class Token {
	// Token����
	public TokenType Type;

	// Tokenֵ
	public Object Value;

	// �����ڴ��е���ʼλ�ã������ո�
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
