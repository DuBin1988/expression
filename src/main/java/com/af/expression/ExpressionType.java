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
	Divide,			//除法
	Modulo,			//求余
	
	Concat,			//字符串连接
	
	Not,					//逻辑非
	And,					//逻辑与
	Or,					//逻辑或

	Constant,		//常数
	Identy,			//标识符
	
	Property,		//获取对象属性
	Call,				//函数调用

	Comma,			//逗号表达式
	
	Assign,			//赋值语句
	Condition;		//条件语句	
    
}
