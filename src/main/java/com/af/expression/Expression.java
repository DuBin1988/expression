package com.af.expression;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import com.af.util.MehodSignatureMatcher;

public class Expression {
	// 节点类型
	public ExpressionType type;

	// 节点值
	public Object value;

	// 子节点
	public List<Expression> children = new ArrayList<Expression>();

	// 运行时对应的Delegate，Delegate中保存有实参
	public Delegate delegate;
	
	// 节点对应的源程序及位置
	private String source;
	private int pos;

	private Expression(ExpressionType type, String source, int pos) {
		this.type = type;
		this.source = source;
		this.pos = pos;
	}

	private Expression(ExpressionType type, Object value, String source, int pos) {
		this.type = type;
		this.value = value;
		this.source = source;
		this.pos = pos;
	}
	
	// 覆盖toString方法，显示树状的节点信息，方便调试
	@Override
	public String toString() {
		// 显示自己的类型及名称
		String result = "type: " + this.type + ", value: " + 
				(this.value != null ? this.value.toString() : "null") + "[\n";
		// 递归显示子
		for (Expression child : this.children) {
			result += (child != null ? child.toString() : "null\n");
		}
		result += "]";
		return result;
	}
	
	// 编译，产生可执行单元Delegate
	public Delegate Compile() {
		Delegate result = new Delegate(this);
		return result;
	}

	// 执行
	public Object invoke() {
		try {
			switch (type) {
			case Or: // 逻辑运算
			case And: {
				Expression left = this.children.get(0);
				Expression right = this.children.get(1);
				boolean l = getBoolean(left.invoke());
				boolean r = getBoolean(right.invoke());
				switch (type) {
				case Or:
					return l || r;
				case And:
					return l && r;
				}
			}
			case Not: {
				Expression left = this.children.get(0);
				boolean l = getBoolean(left.invoke());
				return !l;
			}
			case Add: // 算数运算
			case Subtract:
			case Multiply:
			case Divide:
			case Modulo: {
				Expression left = this.children.get(0);
				Expression right = this.children.get(1);
				BigDecimal l = new BigDecimal(left.invoke().toString());
				BigDecimal r = new BigDecimal(right.invoke().toString());
				switch (type) {
				case Add:
					return l.add(r);
				case Subtract:
					return l.subtract(r);
				case Multiply:
					return l.multiply(r);
				case Divide:
					return l.divide(r);
				case Modulo:
					return new BigDecimal(l.intValue() % r.intValue());
				}
			}
			case Concat: { // 字符串连接
				Expression left = this.children.get(0);
				Expression right = this.children.get(1);
				Object l = left.invoke();
				Object r = right.invoke();
				return l.toString() + r.toString();
			}
			case Json: {	// 返回Json对象
				return Json();
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
					BigDecimal dl = new BigDecimal(l.toString());
					BigDecimal dr = new BigDecimal(r.toString());
					int cr = dl.compareTo(dr);
					switch (type) {
					case GreaterThan:
						return cr > 0;
					case GreaterThanOrEqual:
						return cr >= 0;
					case LessThan:
						return cr < 0;
					case LessThanOrEqual:
						return cr <= 0;
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
					return l.toString().equals(r.toString());
				case NotEqual:
					return !l.toString().equals(r.toString());
				}
			}
			case Constant: { // 常数
				// 数字
				if (this.value instanceof Double || this.value instanceof Integer) {
					return new BigDecimal(this.value.toString());
				}
				// 其它常数，直接返回值
				return this.value;
			}
			case Identy: { // 标识符，获取实参对象
				return this.delegate.objectNames.get((String) this.value);
			}
			case Condition: { // 条件语句
				return condition();
			}
			case Property: { // 获取属性值
				return property();
			}
			case ArrayIndex: { // 获取数组元素
				return arrayIndex();
			}
			case Call: { // 函数调用
				return call();
			}
			case For: { // for循环
				return loop();
			}
			case Assign: { // 属性赋值
				return assign();
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
		} catch (ExpressionException ex) {
			throw ex;
		} catch (Exception ex) {
			throw new ExpressionException(source, pos, ex);
		}
	}

	// 执行条件处理
	private Object condition() {
		// 条件
		Expression condExp = this.children.get(0);
		// 为真时表达式
		Expression isTrue = this.children.get(1);
		// 为假时表达式
		Expression isFalse = this.children.get(2);
		// 如果条件返回的不是bool值，则非空值为真，空值为假
		Object obj = condExp.invoke();
		boolean cond = getBoolean(obj); 
		// 为真，返回为真的表达式，否则，返回为假的表达式
		if (cond) {
			return isTrue.invoke();
		} else {
			return isFalse.invoke();
		}
	}
	
	// 执行for循环
	private Object loop() throws Exception {
		Expression objExp = this.children.get(0);
		// 获取对象，for循环只针对JSONArray
		JSONArray array = (JSONArray)objExp.invoke();
		// 获取循环体，循环体中row代表每一项对象, 把对象传递给循环体执行
		Expression body = this.children.get(1);
		for (int i = 0; i < array.length(); i++) {
			body.delegate.objectNames.put("row", array.get(i));
			body.invoke();
		}
		return null;
	}
	
	// 执行函数调用
	private Object call() {
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
		Object[] types = new Object[params.size()];
		for (int i = 0; i < params.size(); i++) {
			types[i] = params.get(i).getClass();
		}
		try {
			// 利用反射机制获得函数
			Method method = MehodSignatureMatcher.getMatchingMethod(obj.getClass(), name, types);
			return method.invoke(obj, params.toArray());
		} catch (Exception e) {
			throw new RuntimeException("函数调用错误：" + name, e);
		}
	}
	
	// 执行赋值过程
	private Object assign() {
		// 属性值
		Expression right = this.children.get(1);
		Object value = right.invoke();
		// 获取属性
		String name = (String) this.value;
		// 要赋值的对象，空代表给变量赋值
		Expression left = this.children.get(0);
		if (left == null) {
			this.delegate.objectNames.put(name, value);
		} else {
			Object obj = left.invoke();
			try {
				Field field = obj.getClass().getField(name);
				// 给属性设置值
				field.set(obj, value);
				return value;
			} catch (Exception e) {
				throw new RuntimeException("属性赋值错误：" + name);
			}
		}
		return value;
	}
	
	// 获取对象属性
	private Object property() throws Exception {
		Expression objExp = this.children.get(0);
		// 获取对象
		Object obj = objExp.invoke();
		// 属性名
		String name = (String) this.value;
		// 是Map
		if (obj.getClass() == HashMap.class) {
			Map<String, Object> map = (Map<String, Object>)obj;
			return map.get(name);
		} 
		// 是JSONObject
		else if (obj instanceof JSONObject) {
			JSONObject json = (JSONObject)obj;
			return json.get(name);
		}
		else {
			// 利用反射机制获得属性值
			return obj.getClass().getField(name).get(obj);
		}
	}
	
	// 获取数组元素
	private Object arrayIndex() throws Exception {
		Expression objExp = this.children.get(0);
		Expression indexExp = this.children.get(1);
		// 获取对象
		Object obj = objExp.invoke();
		// 获取下标值
		int index = Integer.parseInt(indexExp.invoke().toString());
		// 如果对象为JSONArray，调用JSONArray的方法
		if (obj instanceof JSONArray) {
			JSONArray array = (JSONArray)obj;
			return array.get(index);
		}
		throw new ExpressionException(this.source, this.pos);
	}
	
	// 返回Json对象的结果，返回一个Map
	private Object Json() {
		Map<String, Object> result = new HashMap<String, Object>();
		for (Expression child : this.children) {
			String name = child.value.toString();
			Object value = child.children.get(0).invoke();
			result.put(name, value);
		}
		return result;
	}
	
	// 根据值返回boolean结果
	private boolean getBoolean(Object obj) {
		// 如果条件返回的不是bool值，则非空值为真，空值为假
		boolean cond = false;
		if (obj instanceof Boolean) {
			cond = (Boolean)obj;
		} else {
			cond = (obj != null);
		}
		return cond;
	}
	
	// 产生常数
	public static Expression Constant(Object value, String source, int pos) {
		Expression result = new Expression(ExpressionType.Constant, value, source, pos);
		return result;
	}

	// 产生标识符
	public static Expression Identy(Object value, String source, int pos) {
		Expression result = new Expression(ExpressionType.Identy, value, source, pos);
		return result;
	}

	// 获取对象属性
	public static Expression Property(Expression objExp, String name, String source, int pos) {
		Expression result = new Expression(ExpressionType.Property, name, source, pos);
		result.children.add(objExp);
		return result;
	}

	// 产生Json对象，value=null, children=属性值对
	public static Expression Json(List<Expression> attrs, String source, int pos) {
		Expression result = new Expression(ExpressionType.Json, source, pos);
		for(Expression exp : attrs) {
			result.children.add(exp);
		}
		return result;
	}
	
	// 产生属性表达式，value=属性名，children[0]=属性值
	public static Expression Attr(String name, Expression value, String source, int pos) {
		Expression result = new Expression(ExpressionType.Attr, source, pos);
		result.value = name;
		result.children.add(value);
		return result;
	}
	
	// 逗号表达式, value=null, children=各表达式
	public static Expression Comma(List<Expression> children, String source, int pos) {
		Expression result = new Expression(ExpressionType.Comma, source, pos);
		for(Expression exp : children) {
			result.children.add(exp);
		}
		return result;
	}

	// 赋值语句，value=属性名/变量名，child[0]=赋值对象，child[1]=赋值内容
	public static Expression Assign(Expression objExp, Expression exp, String name, String source, int pos) {
		Expression result = new Expression(ExpressionType.Assign, name, source, pos);
		result.children.add(objExp);
		result.children.add(exp);
		return result;
	}

	// 函数调用
	public static Expression Call(Expression objExp, String name,
			List<Expression> params, String source, int pos) {
		Expression result = new Expression(ExpressionType.Call, name, source, pos);
		result.children.add(objExp);
		// 把所有参数加入函数调用子中
		for (Expression param : params) {
			result.children.add(param);
		}
		return result;
	}

	// 产生一个条件语句, test:条件，ifTrue:为真时结果，ifFalse:为假时结果
	public static Expression Condition(Expression test, Expression ifTrue,
			Expression ifFalse, String source, int pos) {
		Expression result = new Expression(ExpressionType.Condition, source, pos);
		result.children.add(test);
		result.children.add(ifTrue);
		result.children.add(ifFalse);
		return result;
	}

	// 产生逻辑非语句
	public static Expression Not(Expression exp, String source, int pos) {
		Expression result = new Expression(ExpressionType.Not, source, pos);
		result.children.add(exp);
		return result;
	}

	// 产生逻辑与语句
	public static Expression And(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.And, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生逻辑或语句
	public static Expression Or(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Or, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生>比较运算
	public static Expression GreaterThan(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.GreaterThan, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生>=比较运算
	public static Expression GreaterThanOrEqual(Expression left,
			Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.GreaterThanOrEqual, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生<比较运算
	public static Expression LessThan(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.LessThan, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生<=比较运算
	public static Expression LessThanOrEqual(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.LessThanOrEqual, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生==比较运算
	public static Expression Equal(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Equal, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生!=比较运算
	public static Expression NotEqual(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.NotEqual, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生+运算
	public static Expression Add(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Add, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生-运算
	public static Expression Subtract(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Subtract, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生*运算
	public static Expression Multiply(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Multiply, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生除法运算
	public static Expression Divide(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Divide, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生求余运算
	public static Expression Modulo(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Modulo, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// 产生字符串连接运算
	public static Expression Concat(Expression left, Expression right, String source, int pos) {
		Expression result = new Expression(ExpressionType.Concat, source, pos);
		result.children.add(left);
		result.children.add(right);
		return result;
	}
	
	// 产生数组下标
	public static Expression ArrayIndex(Expression objExp, Expression indexExp, String source, int pos) {
		Expression result = new Expression(ExpressionType.ArrayIndex, source, pos);
		result.children.add(objExp);
		result.children.add(indexExp);
		return result;
	}
	
	// 产生for循环
	public static Expression For(Expression objExp, Expression forExp, String source, int pos) {
		Expression result = new Expression(ExpressionType.For, source, pos);
		result.children.add(objExp);
		result.children.add(forExp);
		return result;
	}
}
