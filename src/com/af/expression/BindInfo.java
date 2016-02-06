package com.af.expression;

import java.util.ArrayList;
import java.util.List;

//保存绑定信息
public class BindInfo extends Object {
    //源对象名
	public String Object;
    
	//绑定路径
    public String Path;
    
    //绑定结果
    private Object value;
    
    //监听属性变化的Delegate
    public List<Delegate> delegates = new ArrayList<Delegate>();
    
    public BindInfo(String object, String path) {
    	this.Object = object;
    	this.Path = path;
    }

    //设置绑定值，当绑定值发生变化时，调用注册的Delegate运行
    public void setValue(Object v) {
    	for(Delegate del : delegates) {
    		del.invoke();
    	}
    }
    
    //是否相等判断
    public boolean equals(Object obj)
    {
        BindInfo other = (BindInfo)obj;
        return this.Object.equals(other.Object) && this.Path.equals(other.Path);
    }
}
