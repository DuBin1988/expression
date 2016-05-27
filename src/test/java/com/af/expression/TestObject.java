package com.af.expression;

import java.util.ArrayList;
import java.util.List;

public class TestObject {
	public double age;
	
	public List<PropertyChanged> listners = new ArrayList<PropertyChanged>();
	
	public void setAge(double age) {
		this.age = age;
		// 通知所有监听器，属性变化了
		for(PropertyChanged listner : listners) {
			listner.invoke();
		}
	}
	
	public double getNumber() {
		return 3;
	}
}
