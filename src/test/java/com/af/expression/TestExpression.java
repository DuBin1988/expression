package com.af.expression;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import junit.framework.TestCase;

public class TestExpression extends TestCase {
	public void testThree() {
		execute("JSON对象.exp");
	}

	// 执行并检查程序结果
	private void execute(String name) {
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
			Object actual = d.invoke();

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
			String str = reader.readLine();
			// `expected`部分为期望值
			while (!str.equals("expected:")) {
				expression += str;
				str = reader.readLine();
			}
			return expression;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
