package com.af.expression;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestGeneralExpression extends TestCase {
	public void testOne() {
		String str = "a + 5 > 9";
		// ���ַ�������
		Program prog = new Program(str);

		// ����
		Delegate d = prog.parse();

		// getParamNames�������в����������ҵ��Ķ�����putParam�Ż�
		Set<String> objectNames = prog.getParamNames();
		for (String name : objectNames ) {
			// ����name�ҵ��������Լ���д
			Object obj = findByName(name);
			// �Ѷ���Ż�objectNames
			prog.putParam(name, obj);
		}
		
		Object result = d.invoke();
	}
	
	private Object findByName(String name) {
		if(name.equals("a")) {
			return 50;
		} else if(name.endsWith("b")) {
			return 60;
		}
		return null;
	}
}
