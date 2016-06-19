package com.af.expression;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestComment extends TestCase {
	public void testOne() {
		String expression = ""
				+ "$测试\n"
				+ "//这里有注释\n"
				+ "继续";
		Program prog = new Program(expression);
		Delegate d = prog.parse();
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("b", 20);
		Object actual = d.invoke(params);
		assertEquals("测试\n\n继续", actual);
	}
}
