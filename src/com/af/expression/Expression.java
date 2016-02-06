package com.af.expression;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Expression {
	// 节点类型
	public ExpressionType type;

	// 节点值
	public Object value;

	// 子节点
	public List<Expression> children = new ArrayList<Expression>();

	// 运行时对应的Delegate，Delegate中保存有实参
	public Delegate delegate;

	private Expression(ExpressionType type) {
		this.type = type;
	}

	private Expression(ExpressionType type, Object value) {
		this.type = type;
		this.value = value;
	}

	// 编译，产生可执行单元Delegate，编译时，将获取所有虚参
	public Delegate Compile() {
		Delegate result = new Delegate(this);
		// 沿树进行遍历，找到所有对象虚参名，保存到result中
		Expression exp = this;
		List<String> names = this.getParams(result);
		for (String name : names) {
			result.objectNames.put(name, null);
		}
		return result;
	}

	// 递归获取虚参名
	private List<String> getParams(Delegate del) {
		List<String> result = new ArrayList<String>();
		// 标识符，代表虚参
		if (this.type == ExpressionType.Identy) {
			result.add((String) this.value);
			// 有虚参，标记Delegate, 从Delegate中获取实参
			this.delegate = del;
		}
		// 递归获取子节点
		for (Expression exp : this.children) {
			result.addAll(exp.getParams(del));
		}
		return result;
	}

	// 执行
	public Object invoke() {
		switch (type) {
		case Or: // 逻辑运算
		case And: {
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			boolean l = Boolean.parseBoolean(left.invoke().toString());
			boolean r = Boolean.parseBoolean(right.invoke().toString());
			switch (type) {
			case Or:
				return l || r;
			case And:
				return l && r;
			}
		}
		case Not: {
			Expression left = this.children.get(0);
			boolean l = Boolean.parseBoolean(left.invoke().toString());
			return !l;
		}
		case Add: // 算数运算
		case Subtract:
		case Multiply:
		case Divide:
		case Modulo: {
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			double l = Double.parseDouble(left.invoke().toString());
			double r = Double.parseDouble(right.invoke().toString());
			switch (type) {
			case Add:
				return l + r;
			case Subtract:
				return l - r;
			case Multiply:
				return l * r;
			case Divide:
				return l / r;
			case Modulo:
				return l % r;
			}
		}
		case Concat: { // 字符串连接
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			Object l = left.invoke();
			Object r = right.invoke();
			return l.toString() + r.toString();
		}
		case GreaterThan: // 比较运算
		case GreaterThanOrEqual:
		case LessThan:
		case LessThanOrEqual: {
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			Object l = left.invoke();
			Object r = right.invoke();
			// 两个字符串比较
			if (l instanceof String && r instanceof String) {
				int result = ((String) l).compareTo((String) r);
				switch (type) {
				case GreaterThan:
					return result > 0;
				case GreaterThanOrEqual:
					return result >= 0;
				case LessThan:
					return result < 0;
				case LessThanOrEqual:
					return result <= 0;
				}
			}
			// 字符串与数字比较，把字符串转换成数字
			else {
				Double dl = Double.parseDouble(l.toString());
				Double dr = Double.parseDouble(r.toString());
				switch (type) {
				case GreaterThan:
					return dl > dr;
				case GreaterThanOrEqual:
					return dl >= dr;
				case LessThan:
					return dl < dr;
				case LessThanOrEqual:
					return dl <= dr;
				}
			}
		}
		case Equal: // 相等比较
		case NotEqual: {
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			Object l = left.invoke();
			Object r = right.invoke();
			switch (type) {
			case Equal:
				return l.equals(r);
			case NotEqual:
				return !l.equals(r);
			}
		}
		case Constant: { // 常数
			// 数字
			if (this.value instanceof Double || this.value instanceof Integer) {
				return Double.parseDouble(this.value.toString());
			}
			// 其它常数，直接返回值
			return this.value;
		}
		case Identy: { // 标识符，获取实参对象
			return this.delegate.objectNames.get((String) this.value);
		}
		case Condition: { // 条件语句
			// 条件
			Expression condExp = this.children.get(0);
			// 为真时表达式
			Expression isTrue = this.children.get(1);
			// 为假时表达式
			Expression isFalse = this.children.get(2);
			boolean cond = (Boolean) condExp.invoke();
			// 为真，返回为真的表达式，否则，返回为假的表达式
			if (cond) {
				return isTrue.invoke();
			} else {
				return isFalse.invoke();
			}
		}
		case Property: { // 获取属性值
			Expression objExp = this.children.get(0);
			// 获取对象
			Object obj = objExp.invoke();
			// 属性名
			String name = (String) this.value;
			// 利用反射机制获得属性值
			try {
				return obj.getClass().getField(name).get(obj);
			} catch (Exception e) {
				throw new RuntimeException("属性值获取异常：" + name);
			}
		}
		case Call: { // 函数调用
			Expression objExp = this.children.get(0);
			// 获取对象
			Object obj = objExp.invoke();
			// 函数名
			String name = (String) this.value;
			// 获得参数计算结果
			List<Object> params = new ArrayList<Object>();
			for (int i = 1; i < this.children.size(); i++) {
				Expression paramExp = this.children.get(i);
				params.add(paramExp.invoke());
			}
			// 转换参数类型
			Class[] types = new Class[params.size()];
			for (int i = 0; i < params.size(); i++) {
				types[i] = params.get(i).getClass();
			}
			try {
				// 利用反射机制获得函数
				return obj.getClass().getMethod(name, types).invoke(obj,
						params.toArray());
			} catch (Exception e) {
				throw new RuntimeException("函数调用错误：name");
			}
		}
		case Assign: { // 属性赋值
			// 要赋值的对象
			Expression left = this.children.get(0);
			Object obj = left.invoke();
			// 属性值
			Expression right = this.children.get(1);
			Object value = right.invoke();
			// 获取属性
			String name = (String) this.value;
			try {
				Field field = obj.getClass().getField(name);
				// 给属性设置值
				field.set(obj, value);
				return value;
			} catch (Exception e) {
				throw new RuntimeException("属性赋值错误：" + name);
			}
		}
		case Comma: { // 逗号表达式
			Object value = 0;
			for(Expression child : this.children) {
				value = child.invoke();
			}
			return value;
		}
		}
		throw new RuntimeException("无效操作");
	}

	// 产生常数
	public static Expression Constant(Object value) {
		Expression result = new Expression(ExpressionType.Constant, value);
		return result;
	}

	// 产生标识符
	public static Expression Identy(Object value) {
		Expression result = new Expression(ExpressionType.Identy, value);
		return result;
	}

	// 获取对象属性
	public static Expression Property(Expression objExp, String name) {
		Expression result = new Expression(ExpressionType.Property, name);
		result.children.add(objExp);
		return result;
	}

	// 逗号表达式
	public static Expression Comma(List<Expression> children) {
		Expression result = new Expression(ExpressionType.Comma);
		for(Expression exp : children) {
			result.children.add(exp);
		}
		return result;
	}

	// 赋值语句
	public static Expression Assign(Expression objExp, Expression exp, String name) {
		Expression result = new Expression(ExpressionType.Assign, name);
		result.children.add(objExp);
		result.children.add(exp);
		return result;
	}

	// 函数调用
	public static Expression Call(Expression objExp, String name,
			Expression[] params) {
		Expression result = new Expression(ExpressionType.Call, name);
		result.children.add(objExp);
		// 把所有参数加入函数调用子中
		for (Expression param : params) {
			result.children.add(param);
		}
		return result;
	}

	// 产生一个条件语句, test:条件，ifTrue:为真时结果，ifFalse:为假时结果
	public static Expression Condition(Expression test, Expression ifTrue,
			Expression ifFalse) {
		Expression result = new Expression(ExpressionType.Condition);
		result.children.add(test);
		result.children.add(ifTrue);
		result.children.add(ifFalse);
		return result;
	}

	// 产生逻辑非语句
	public static Expression Not(Expression exp) {
		Expression result = new Expression(ExpressionType.Not);
		result.children.add(exp);
		return result;
	}

	// 产生逻辑与语句
	public static Expression And(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.And);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生逻辑或语句
	public static Expression Or(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Or);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生>比较运算
	public static Expression GreaterThan(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.GreaterThan);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生>=比较运算
	public static Expression GreaterThanOrEqual(Expression left,
			Expression right) {
		Expression result = new Expression(ExpressionType.GreaterThanOrEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生<比较运算
	public static Expression LessThan(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.LessThan);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生<=比较运算
	public static Expression LessThanOrEqual(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.LessThanOrEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生==比较运算
	public static Expression Equal(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Equal);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生!=比较运算
	public static Expression NotEqual(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.NotEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生+运算
	public static Expression Add(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Add);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生-运算
	public static Expression Subtract(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Subtract);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生*运算
	public static Expression Multiply(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Multiply);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生除法运算
	public static Expression Divide(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Divide);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生求余运算
	public static Expression Modulo(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Modulo);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生字符串连接运算
	public static Expression Concat(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Concat);
		result.children.add(left);
		result.children.add(right);
		return result;
	}
}
