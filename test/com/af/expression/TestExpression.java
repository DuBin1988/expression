package com.af.expression;

import java.util.Set;

import junit.framework.Assert;
import junit.framework.TestCase;

public class TestExpression extends TestCase {
	private TestObject a = new TestObject();
	private TestObject b = new TestObject();
	
	public void testOne() {
		String str = "a.age = b.getNumber() + (b.age.Sum(data.age+5) + (3.56).Round(2)) * 2;";
		// ���ַ�������
		Program prog = new Program(str);

		// ����
		prog.parse();

		// getParamNames�������в����������ҵ��Ķ�����putParam�Ż�
		Set<String> objectNames = prog.getParamNames();
		for (String name : objectNames ) {
			// ����name�ҵ��������Լ���д
			Object obj = findByName(name);
			// �Ѷ���Ż�objectNames
			prog.putParam(name, obj);
		}
		
		//bindings����������Ķ������ԣ�����Щ�������Է����仯ʱ��֪ͨprogִ�й���
		for (final BindInfo bind : prog.bindings) {
			// �ҵ��󶨶���
			final TestObject obj = (TestObject)findByName(bind.Object);
			// �ҵ�����
			String name = bind.Path;
			// ע�����Լ���
			obj.listners.add(new PropertyChanged(){
				public void invoke() {
					bind.setValue(obj.age);
				}
			});
		}
		
		//����
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
