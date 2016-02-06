package com.af.expression;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestGeneralExpression extends TestCase {
	public void testOne() {
		String str = "a + 5 > 9";
		// 用字符串构造
		Program prog = new Program(str);

		// 解析
		Delegate d = prog.parse();

		// getParamNames返回所有参数名，把找到的对象用putParam放回
		Set<String> objectNames = prog.getParamNames();
		for (String name : objectNames ) {
			// 根据name找到对象，需自己编写
			Object obj = findByName(name);
			// 把对象放回objectNames
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
