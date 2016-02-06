package com.af.expression;

import java.util.HashMap;
import java.util.Map;

//ִ�е�Ԫ������������Σ�Expression�����Ľ��
public class Delegate {
	// ��α�
	public Map<String, Object> objectNames = new HashMap<String, Object>();
	
	// ִ��ʱ��Expression
	private Expression exp;
	
	// ��Expression���죬ִ��ʱ���Expression
	public Delegate(Expression exp) {
		this.exp = exp;
	}
	
	// ִ�г���ִ��ǰ����������ʵ����
	public Object invoke() {
		// ����exp��ִ�й���
		return exp.invoke();
	}
}
