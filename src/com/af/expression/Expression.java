package com.af.expression;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

public class Expression {
	// �ڵ�����
	public ExpressionType type;

	// �ڵ�ֵ
	public Object value;

	// �ӽڵ�
	public List<Expression> children = new ArrayList<Expression>();

	// ����ʱ��Ӧ��Delegate��Delegate�б�����ʵ��
	public Delegate delegate;

	private Expression(ExpressionType type) {
		this.type = type;
	}

	private Expression(ExpressionType type, Object value) {
		this.type = type;
		this.value = value;
	}

	// ���룬������ִ�е�ԪDelegate������ʱ������ȡ�������
	public Delegate Compile() {
		Delegate result = new Delegate(this);
		// �������б������ҵ����ж�������������浽result��
		Expression exp = this;
		List<String> names = this.getParams(result);
		for (String name : names) {
			result.objectNames.put(name, null);
		}
		return result;
	}

	// �ݹ��ȡ�����
	private List<String> getParams(Delegate del) {
		List<String> result = new ArrayList<String>();
		// ��ʶ�����������
		if (this.type == ExpressionType.Identy) {
			result.add((String) this.value);
			// ����Σ����Delegate, ��Delegate�л�ȡʵ��
			this.delegate = del;
		}
		// �ݹ��ȡ�ӽڵ�
		for (Expression exp : this.children) {
			result.addAll(exp.getParams(del));
		}
		return result;
	}

	// ִ��
	public Object invoke() {
		switch (type) {
		case Or: // �߼�����
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
		case Add: // ��������
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
		case Concat: { // �ַ�������
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			Object l = left.invoke();
			Object r = right.invoke();
			return l.toString() + r.toString();
		}
		case GreaterThan: // �Ƚ�����
		case GreaterThanOrEqual:
		case LessThan:
		case LessThanOrEqual: {
			Expression left = this.children.get(0);
			Expression right = this.children.get(1);
			Object l = left.invoke();
			Object r = right.invoke();
			// �����ַ����Ƚ�
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
			// �ַ��������ֱȽϣ����ַ���ת��������
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
		case Equal: // ��ȱȽ�
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
		case Constant: { // ����
			// ����
			if (this.value instanceof Double || this.value instanceof Integer) {
				return Double.parseDouble(this.value.toString());
			}
			// ����������ֱ�ӷ���ֵ
			return this.value;
		}
		case Identy: { // ��ʶ������ȡʵ�ζ���
			return this.delegate.objectNames.get((String) this.value);
		}
		case Condition: { // �������
			// ����
			Expression condExp = this.children.get(0);
			// Ϊ��ʱ���ʽ
			Expression isTrue = this.children.get(1);
			// Ϊ��ʱ���ʽ
			Expression isFalse = this.children.get(2);
			boolean cond = (Boolean) condExp.invoke();
			// Ϊ�棬����Ϊ��ı��ʽ�����򣬷���Ϊ�ٵı��ʽ
			if (cond) {
				return isTrue.invoke();
			} else {
				return isFalse.invoke();
			}
		}
		case Property: { // ��ȡ����ֵ
			Expression objExp = this.children.get(0);
			// ��ȡ����
			Object obj = objExp.invoke();
			// ������
			String name = (String) this.value;
			// ���÷�����ƻ������ֵ
			try {
				return obj.getClass().getField(name).get(obj);
			} catch (Exception e) {
				throw new RuntimeException("����ֵ��ȡ�쳣��" + name);
			}
		}
		case Call: { // ��������
			Expression objExp = this.children.get(0);
			// ��ȡ����
			Object obj = objExp.invoke();
			// ������
			String name = (String) this.value;
			// ��ò���������
			List<Object> params = new ArrayList<Object>();
			for (int i = 1; i < this.children.size(); i++) {
				Expression paramExp = this.children.get(i);
				params.add(paramExp.invoke());
			}
			// ת����������
			Class[] types = new Class[params.size()];
			for (int i = 0; i < params.size(); i++) {
				types[i] = params.get(i).getClass();
			}
			try {
				// ���÷�����ƻ�ú���
				return obj.getClass().getMethod(name, types).invoke(obj,
						params.toArray());
			} catch (Exception e) {
				throw new RuntimeException("�������ô���name");
			}
		}
		case Assign: { // ���Ը�ֵ
			// Ҫ��ֵ�Ķ���
			Expression left = this.children.get(0);
			Object obj = left.invoke();
			// ����ֵ
			Expression right = this.children.get(1);
			Object value = right.invoke();
			// ��ȡ����
			String name = (String) this.value;
			try {
				Field field = obj.getClass().getField(name);
				// ����������ֵ
				field.set(obj, value);
				return value;
			} catch (Exception e) {
				throw new RuntimeException("���Ը�ֵ����" + name);
			}
		}
		case Comma: { // ���ű��ʽ
			Object value = 0;
			for(Expression child : this.children) {
				value = child.invoke();
			}
			return value;
		}
		}
		throw new RuntimeException("��Ч����");
	}

	// ��������
	public static Expression Constant(Object value) {
		Expression result = new Expression(ExpressionType.Constant, value);
		return result;
	}

	// ������ʶ��
	public static Expression Identy(Object value) {
		Expression result = new Expression(ExpressionType.Identy, value);
		return result;
	}

	// ��ȡ��������
	public static Expression Property(Expression objExp, String name) {
		Expression result = new Expression(ExpressionType.Property, name);
		result.children.add(objExp);
		return result;
	}

	// ���ű��ʽ
	public static Expression Comma(List<Expression> children) {
		Expression result = new Expression(ExpressionType.Comma);
		for(Expression exp : children) {
			result.children.add(exp);
		}
		return result;
	}

	// ��ֵ���
	public static Expression Assign(Expression objExp, Expression exp, String name) {
		Expression result = new Expression(ExpressionType.Assign, name);
		result.children.add(objExp);
		result.children.add(exp);
		return result;
	}

	// ��������
	public static Expression Call(Expression objExp, String name,
			Expression[] params) {
		Expression result = new Expression(ExpressionType.Call, name);
		result.children.add(objExp);
		// �����в������뺯����������
		for (Expression param : params) {
			result.children.add(param);
		}
		return result;
	}

	// ����һ���������, test:������ifTrue:Ϊ��ʱ�����ifFalse:Ϊ��ʱ���
	public static Expression Condition(Expression test, Expression ifTrue,
			Expression ifFalse) {
		Expression result = new Expression(ExpressionType.Condition);
		result.children.add(test);
		result.children.add(ifTrue);
		result.children.add(ifFalse);
		return result;
	}

	// �����߼������
	public static Expression Not(Expression exp) {
		Expression result = new Expression(ExpressionType.Not);
		result.children.add(exp);
		return result;
	}

	// �����߼������
	public static Expression And(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.And);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// �����߼������
	public static Expression Or(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Or);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����>�Ƚ�����
	public static Expression GreaterThan(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.GreaterThan);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����>=�Ƚ�����
	public static Expression GreaterThanOrEqual(Expression left,
			Expression right) {
		Expression result = new Expression(ExpressionType.GreaterThanOrEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����<�Ƚ�����
	public static Expression LessThan(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.LessThan);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����<=�Ƚ�����
	public static Expression LessThanOrEqual(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.LessThanOrEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����==�Ƚ�����
	public static Expression Equal(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Equal);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����!=�Ƚ�����
	public static Expression NotEqual(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.NotEqual);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����+����
	public static Expression Add(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Add);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����-����
	public static Expression Subtract(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Subtract);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ����*����
	public static Expression Multiply(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Multiply);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ������������
	public static Expression Divide(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Divide);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// ������������
	public static Expression Modulo(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Modulo);
		result.children.add(left);
		result.children.add(right);
		return result;
	}

	// �����ַ�����������
	public static Expression Concat(Expression left, Expression right) {
		Expression result = new Expression(ExpressionType.Concat);
		result.children.add(left);
		result.children.add(right);
		return result;
	}
}
