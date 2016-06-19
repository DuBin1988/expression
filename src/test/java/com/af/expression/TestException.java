package com.af.expression;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestException extends TestCase {
	public void testOne() {
		String expression = "$测试\n{b}\n{a.name}$,\n 3+4";
		Program prog = new Program(expression);
		Delegate d = prog.parse();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("b", 20);
		Object actual = d.invoke(params);		
	}
}
