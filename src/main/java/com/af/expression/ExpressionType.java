package com.af.expression;

public enum ExpressionType {
	
	GreaterThan,	//>
	GreaterThanOrEqual,	//>=
	LessThan,		//<
	LessThanOrEqual,			//<=
	Equal,				//==
	NotEqual,		//!=
	
	Add,					//+
	Subtract,		//-
	Multiply,		//*
	Divide,			//����
	Modulo,			//����
	
	Concat,			//�ַ�������
	
	Not,					//�߼���
	And,					//�߼���
	Or,					//�߼���

	Constant,		//����
	Identy,			//��ʶ��
	
	Property,		//��ȡ��������
	Call,				//��������

	Comma,			//���ű��ʽ
	
	Assign,			//��ֵ���
	Condition;		//�������	
    
}
