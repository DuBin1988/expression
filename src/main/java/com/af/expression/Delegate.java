package com.af.expression;

import java.util.HashMap;
import java.util.Map;

//执行单元，保存所有虚参，Expression编译后的结果
public class Delegate {
	// 参数值及变量值存放处
	public Map<String, Object> objectNames = new HashMap<String, Object>();

	// 执行时的Expression
	private Expression exp;

	// 用Expression构造，执行时最顶层Expression
	public Delegate(Expression exp) {
		this.exp = exp;
	}

	// 无参执行过程
	public Object invoke() {
		return this.invoke(new HashMap<String, Object>());
	}
	
	// 执行程序，执行前，参数必须实例化
	// - params：执行时，所带参数值
	public Object invoke(Map<String, Object> params) {
		// 把初始参数给参数表
		this.objectNames = params;
		// 沿根Expression节点遍历，把delegate传递下去
		putDelegate(this.exp);

		// 调用exp的执行过程
		return exp.invoke();
	}
	
	// 沿根节点递归，传递delegate的过程
	private void putDelegate(Expression parent) {
		for (Expression child : parent.children) {
			// 有些节点会放空的子节点
			if (child == null) {
				continue;
			}
			child.delegate = this;
			putDelegate(child);
		}
	}
}
