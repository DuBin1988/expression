package com.af.expression;

import java.util.ArrayList;
import java.util.List;

//�������Ϣ
public class BindInfo extends Object {
    //Դ������
	public String Object;
    
	//��·��
    public String Path;
    
    //�󶨽��
    private Object value;
    
    //�������Ա仯��Delegate
    public List<Delegate> delegates = new ArrayList<Delegate>();
    
    public BindInfo(String object, String path) {
    	this.Object = object;
    	this.Path = path;
    }

    //���ð�ֵ������ֵ�����仯ʱ������ע���Delegate����
    public void setValue(Object v) {
    	for(Delegate del : delegates) {
    		del.invoke();
    	}
    }
    
    //�Ƿ�����ж�
    public boolean equals(Object obj)
    {
        BindInfo other = (BindInfo)obj;
        return this.Object.equals(other.Object) && this.Path.equals(other.Path);
    }
}
