package com.af.expression;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestExpression extends TestCase {
	private TestObject a = new TestObject();
	private TestObject b = new TestObject();
	
	public void testOne() {
		String str = "a.age = b.getNumber() + (b.age.Sum(data.age+5) + (3.56).Round(2)) * 2;";
		// 用字符串构造
		Program prog = new Program(str);

		// 解析
		prog.parse();

		// getParamNames返回所有参数名，把找到的对象用putParam放回
		Set<String> objectNames = prog.getParamNames();
		for (String name : objectNames ) {
			// 根据name找到对象，需自己编写
			Object obj = findByName(name);
			// 把对象放回objectNames
			prog.putParam(name, obj);
		}
		
		//bindings里存放需监听的对象属性，当这些对象属性发生变化时，通知prog执行规则
		for (final BindInfo bind : prog.bindings) {
			// 找到绑定对象
			final TestObject obj = (TestObject)findByName(bind.Object);
			// 找到属性
			String name = bind.Path;
			// 注册属性监听
			obj.listners.add(new PropertyChanged(){
				public void invoke() {
					bind.setValue(obj.age);
				}
			});
		}
		
		//测试
		b.setAge(8);
		Assert.assertEquals(25.0, a.age);
	}
	
	private Object findByName(String name) {
		if(name.equals("a")) {
			return a;
		} else if(name.endsWith("b")) {
			return b;
		}
		return null;
	}
}
