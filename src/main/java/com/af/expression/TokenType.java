package com.af.expression;

public enum TokenType {
    Int("Int"), 
    Double("Double"), 
    Bool("Bool"), 
    String("String"), 
    Identy("Identy"), 
    Oper("Oper"), 
    End("End"), 
    Null("Null");
    
    private String name;
    
    private TokenType(String name)
    {
    	this.name = name;
    }
    
    public String toString() {
    	return this.name;
    }
}
