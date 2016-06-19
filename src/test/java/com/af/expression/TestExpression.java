package com.af.expression;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class TestExpression extends TestCase {
	public void testParam() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("a", 5);
		execute("参数测试.exp", params);
	}
	
	public void testCall() {
		Map<String, Object> params = new HashMap<String, Object>();
		params.put("this", this);
		execute("函数测试.exp", params);
	}

	public void testString() {
		execute("字符串测试.exp");
	}
	
	public void testComment() {
		execute("注释测试.exp");
	}
	
	public void testCondition() {
		execute("条件表达式.exp");
	}	

	// 不带参数执行并检查结果
	private void execute(String name) {
		execute(name, new HashMap<String, Object>());
	}
	
	// 执行并检查程序结果
	private void execute(String name, Map<String, Object> params) {
		try {
			// 打开文件，获取执行内容
			String sourceName = "/expressions/" + name;
			InputStream is = this.getClass().getResourceAsStream(sourceName);
			Reader reader = new InputStreamReader(is);
			BufferedReader buffer = new BufferedReader(reader);

			String expression = readExpression(buffer);
			// 继续读取期望值
			String expected = buffer.readLine();

			buffer.close();
			reader.close();
			is.close();

			// 执行程序，获得实际值
			Program prog = new Program(expression);
			Delegate d = prog.parse();
			Object actual = d.invoke(params);

			assertEquals(expected.toString(), actual.toString());
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// 读取程序
	private String readExpression(BufferedReader reader) {
		try {
			// 读掉`source:`
			reader.readLine();
			String expression = "";
			String str = reader.readLine() + "\n";
			// `expected`部分为期望值
			while (!str.equals("expected:\n")) {
				expression += str;
				str = reader.readLine() + "\n";
			}
			return expression;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	// call函数，用于函数测试
	public String call(Object str) {
		System.out.println(str);
		return str + "test";
	}
}
