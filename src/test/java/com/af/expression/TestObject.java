package com.af.expression;

import java.util.ArrayList;
import java.util.List;

public class TestObject {
	public double age;
	
	public List<PropertyChanged> listners = new ArrayList<PropertyChanged>();
	
	public void setAge(double age) {
		this.age = age;
		// ֪ͨ���м����������Ա仯��
		for(PropertyChanged listner : listners) {
			listner.invoke();
		}
	}
	
	public double getNumber() {
		return 3;
	}
}
