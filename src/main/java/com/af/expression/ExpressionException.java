package com.af.expression;

// 表达式执行异常，将显示执行异常的位置信息
public class ExpressionException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public ExpressionException(String source, int pos) {
		super(getMessage(source, pos));
	}

	public ExpressionException(String source, int pos, Exception cause) {
		super(getMessage(source, pos), cause);
	}
	
	private static String getMessage(String source, int pos) {
		return source.substring(0, pos) + " <- "
				+ source.substring(pos, source.length());
	}
}
