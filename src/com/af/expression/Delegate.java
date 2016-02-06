package com.af.expression;

import java.util.HashMap;
import java.util.Map;

//执行单元，保存所有虚参，Expression编译后的结果
public class Delegate {
	// 虚参表
	public Map<String, Object> objectNames = new HashMap<String, Object>();
	
	// 执行时的Expression
	private Expression exp;
	
	// 用Expression构造，执行时最顶层Expression
	public Delegate(Expression exp) {
		this.exp = exp;
	}
	
	// 执行程序，执行前，参数必须实例化
	public Object invoke() {
		// 调用exp的执行过程
		return exp.invoke();
	}
}
