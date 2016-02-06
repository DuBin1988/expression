package com.af.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class Program {
	// Դ����
	public String Source;

	// ��������İ���Ϣ�������ⲿ�����󶨹�ϵ
	public Set<BindInfo> bindings = new HashSet<BindInfo>();

	// ������䴦������У������Ѿ��������İ���Ϣ���Ѿ������İ󶨣������ظ�����
	private Set<BindInfo> binds = new HashSet<BindInfo>();

	// ���ڵ�ı�����
	private Delegate RootDelegate = null;
	
	// ���б���õĸ�ֵ���
	private List<Delegate> delegates = new ArrayList<Delegate>();
 
	// Token���У����ڻ���
	private Queue<Token> _tokens = new LinkedList<Token>();

	// ��ǰ��ȡ�����ַ�λ��
	private int pos;

	// ��ǰ�Ƿ����ַ���������
	private boolean inString;

	// �Ƿ��ڴ���ֵ�����ߣ���ֵ�����߶��󲻽��а󶨲���
	private boolean isLeft;

	// �ַ��������ڶ�ջ��@�����ַ��������ڣ���{}���м䲿�������ַ���������
	private Stack<Boolean> inStrings = new Stack<Boolean>();

	public Program(String source) {
		this.Source = source;
	}

	// ���ý�������
	public Delegate parse() {
		Expression result = CommaExp();
		this.RootDelegate = result.Compile();
		return this.RootDelegate;
	}

	// ��ȡ���в�����
	public Set<String> getParamNames() {
		Set<String> result = new HashSet<String>();
		// ���������Ĳ�������ͳһ�����ⲿ�������������ظ�
		for (Delegate del : this.delegates) {
			result.addAll(del.objectNames.keySet());
		}
		// �Ѹ��ڵ�Ĳ�����Ҳ����ȥ
		result.addAll(this.RootDelegate.objectNames.keySet());
		return result;
	}

	// ��������ֵ
	public void putParam(String name, Object value) {
		// ������delegate�������Ҫ�ò���������丳ֵ
		for (Delegate del : this.delegates) {
			if (del.objectNames.containsKey(name)) {
				del.objectNames.put(name, value);
			}
		}
		if(this.RootDelegate != null) {
			if (this.RootDelegate.objectNames.containsKey(name)) {
				this.RootDelegate.objectNames.put(name, value);
			}
		}
	}

	// ���ű��ʽ=��ֵ���ʽ(,��ֵ���ʽ)*
	private Expression CommaExp() {
		List<Expression> exps = new ArrayList<Expression>();
		
		Expression first = AssignExp();
		exps.add(first);
		
		Token t = GetToken();
		// û�н���
		while (t.Type == TokenType.Oper && t.Value.equals(",")) {
			Expression r = AssignExp();
			exps.add(r);
		}
		this._tokens.offer(t);
		
		Expression result = Expression.Comma(exps);
		return result;
	}

	// ��ֵ���ʽ=��������=һ����ʽ|һ����ʽ
	private Expression AssignExp() {
		// ��������������еİ󶨶���
		this.binds.clear();

		Token t = GetToken();
		
		// �ѵ�һ��Token��λ�ü�¼����������������
		int firstPos = t.StartPos;
		
		if (t.Type != TokenType.Identy) {
			this.pos = firstPos;
			this._tokens.clear();
			this.inString = false;
			return Exp();
		}
		Expression objExp = Expression.Identy((String) t.Value);

		// ��ߣ���������
		isLeft = true;
		objExp = ObjectPath(objExp);
		isLeft = false;

		// �����Ƕ�������
		if (objExp.type != ExpressionType.Property) {
			this.pos = firstPos;
			this._tokens.clear();
			this.inString = false;
			return Exp();
		}
		t = GetToken();
		if (t.Type != TokenType.Oper || !t.Value.equals("=")) {
			this.pos = firstPos;
			this._tokens.clear();
			this.inString = false;
			return Exp();
		}
		Expression exp = Exp();

		// ������Expression�л�ȡ����������
		String name = (String) objExp.value;
		objExp = objExp.children.get(0);
		// ������ֵ���
		Expression assign = Expression.Assign(objExp, exp, name);
		
		// �ѱ�������������
		Delegate del = assign.Compile();
		this.delegates.add(del);

		// �������ݷ����仯ʱ�����ø�ֵ����
		for (BindInfo bind : this.binds) {
			// �������Ըı����
			bind.delegates.add(del);
			// ���µ�bind�������bind��
			this.bindings.add(bind);
		}
		
		return assign;
	}

	// ���ʽ=����:���(,����:���)(,���)?|���
	// ����=�������
	private Expression Exp() {
		Expression v = Logic();
		// ��':'����ʾ����������ֱ�ӷ��ص����
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals(":")) {
			// ��һ��ת��
			Expression result = Logic();
			// ��һ����","��������ȡ��һ��������������������ҽ�ϣ�ֻ�ܲ��õݹ�
			t = GetToken();
			if (t.Type == TokenType.Oper && t.Value.equals(",")) {
				// �ڶ���ת��
				Expression sExp = Exp();
				// ����
				return Expression.Condition(v, result, sExp);
			} else {
				throw new RuntimeException(GetExceptionMessage("������Ĭ��ֵ!"));
			}
		} else {
			_tokens.offer(t);
			return v;
		}
	}

	// ���������=�߼����� (and|or �߼�����)* | !���ʽ
	private Expression Logic() {
		Token t = GetToken();
		// !���ʽ
		if (t.Type == TokenType.Oper && t.Value.equals("!")) {
			Expression exp = Logic();
			exp = Expression.Not(exp);
			return exp;
		}
		_tokens.offer(t);
		Expression v = Compare();
		t = GetToken();
		while (t.Type == TokenType.Identy
				&& (t.Value.equals("and") || t.Value.equals("or"))) {
			// �ڶ���ת��
			Expression exp = Logic();
			// ִ��
			if (t.Value.equals("and")) {
				v = Expression.And(v, exp);
			} else {
				v = Expression.Or(v, exp);
			}
			t = GetToken();
		}
		_tokens.offer(t);
		return v;
	}

	// �߼�����=���ֱ��ʽ (�Ƚ������ ���ֱ��ʽ)?
	private Expression Compare() {
		Expression left = Math();
		Token t = GetToken();
		if (t.Type == TokenType.Oper
				&& (t.Value.equals(">") || t.Value.equals(">=")
						|| t.Value.equals("<") || t.Value.equals("<="))) {
			Expression rExp = Math();

			if (t.Value.equals(">"))
				return Expression.GreaterThan(left, rExp);
			if (t.Value.equals(">="))
				return Expression.GreaterThanOrEqual(left, rExp);
			if (t.Value.equals("<"))
				return Expression.LessThan(left, rExp);
			if (t.Value.equals("<="))
				return Expression.LessThanOrEqual(left, rExp);
		} else if (t.Type == TokenType.Oper
				&& (t.Value.equals("==") || t.Value.equals("!="))) {
			Expression rExp = Math();

			// ��ȱȽ�
			if (t.Value.equals("==")) {
				return Expression.Equal(left, rExp);
			}
			if (t.Value.equals("!=")) {
				return Expression.NotEqual(left, rExp);
			}
		}
		// ���ص������ʽ���
		_tokens.offer(t);
		return left;
	}

	// ���������=�˳��� (+|-) �˳���
	private Expression Math() {
		Expression v = Mul();
		Token t = GetToken();
		while (t.Type == TokenType.Oper
				&& (t.Value.equals("+") || t.Value.equals("-"))) {
			// ת��������2Ϊ����
			Expression r = Mul();

			// ��ʼ����
			if (t.Value.equals("+")) {
				v = Expression.Add(v, r);
			} else {
				v = Expression.Subtract(v, r);
			}
			t = GetToken();
		}
		_tokens.offer(t);
		return v;
	}

	// �˳���=�� (*|/ ��)
	private Expression Mul() {
		Expression v = UnarySub();
		Token t = GetToken();
		while (t.Type == TokenType.Oper
				&& (t.Value.equals("*") || t.Value.equals("/") || t.Value
						.equals("%"))) {
			// ת���ڶ���Ϊ������
			Expression r = UnarySub();

			// ��ʼ����
			if (t.Value.equals("*")) {
				v = Expression.Multiply(v, r);
			} else if (t.Value.equals("/")) {
				v = Expression.Divide(v, r);
			} else {
				v = Expression.Modulo(v, r);
			}
			t = GetToken();
		}
		_tokens.offer(t);
		return v;
	}

	// ��Ŀ�����
	private Expression UnarySub() {
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals("-")) {
			Expression r = Item();
			return Expression.Subtract(Expression.Constant(0), r);
		}
		_tokens.offer(t);
		return Item();
	}

	// ��=����(.����·��)*
	private Expression Item() {
		StringBuilder objName = new StringBuilder();
		// ��ȡ������ʽ
		Expression objExp = ItemHead(objName);
		// ��ȡ����·�����ʽ
		objExp = ObjectPath(objExp);
		return objExp;
	}

	// ����=(���ʽ)|����|��ʶ��|������|�ַ���ƴ�����У�
	// ������������У�����Ǳ�ʶ������Ҫ�����ҵ��Ķ���
	private Expression ItemHead(StringBuilder name) {
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals("(")) {
			Expression result = CommaExp();
			t = GetToken();
			if (t.Type != TokenType.Oper || !t.Value.equals(")")) {
				throw new RuntimeException(GetExceptionMessage("���Ų�ƥ��"));
			}
			return result;
		} else if (t.Type == TokenType.Oper && t.Value.equals("$")) {
			// �ַ���ƴ������
			Expression strExp = StringUnion();
			return strExp;
		} else if (t.Type == TokenType.Int || t.Type == TokenType.Double
				|| t.Type == TokenType.Bool) {
			return Expression.Constant(t.Value);
		} else if (t.Type == TokenType.Identy) {
			String objName = (String) t.Value;
			// �Ѷ���������
			name.append(objName);
			// ����������
			return ObjectName(objName);
		} else if (t.Type == TokenType.Null) {
			return Expression.Constant(null);
		}
		throw new RuntimeException(GetExceptionMessage("�������ʹ���" + t));
	}

	// ���ַ���ƴ�����н��н���
	private Expression StringUnion() {
		// �ַ������ӷ���
		Expression exp = Expression.Constant("");
		Token t = GetToken();
		// �Ƕ�������
		while ((t.Type == TokenType.Oper && t.Value.equals("{"))
				|| t.Type == TokenType.String) {
			// �ַ����������ַ������ӽ��
			if (t.Type == TokenType.String) {
				exp = Expression.Concat(exp, Expression.Constant(t.Value));
			} else {
				// �����ʽ����{}���������
				Expression objExp = Exp();
				t = GetToken();
				if (t.Type != TokenType.Oper || !t.Value.equals("}")) {
					throw new RuntimeException(GetExceptionMessage("ȱ��'}'"));
				}
				// �ַ�������
				exp = Expression.Concat(exp, objExp);
			}
			t = GetToken();
		}
		_tokens.offer(t);
		return exp;
	}

	// �������ƻ�ȡ�����Լ�������ʽ
	private Expression ObjectName(String objName) {
		Expression objExp = Expression.Identy(objName);
		return objExp;
	}

	private Expression ObjectPath(Expression inExp) {
		// ��¼������Ҫ������·���Ŀ�ʼλ�ã�prevPosΪ�����һ������֮ǰ��λ��
		int endPos = -1;
		int startPos = pos;

		// �����������˽�����ת�����������˽��
		Expression objExp = ObjPath(inExp);
		Token t = GetToken();
		while (t.Type == TokenType.Oper && t.Value.equals(".")) {
			// ��ȡ�����Ա
			Token nameToken = GetToken();
			// �������Ƿ񷽷����ã�����Ƿ������ã�ִ�з������ù���
			Token n = GetToken();
			if (n.Type == TokenType.Oper && n.Value.equals("(")) {
				// ���뷽�����ã�����·������λ��Ϊ���˵������������ƵĲ���
				// ����Ѿ����˵����Ͳ������¼���λ��
				if (endPos == -1) {
					endPos = t.StartPos - 1;
				}
				String name = (String) nameToken.Value;
				objExp = MethodCall(name, objExp);
			} else {
				_tokens.offer(n);
				// ȡ����
				String pi = (String) nameToken.Value;
				objExp = Expression.Property(objExp, pi);
				// �����������˽����������������˽��
				objExp = ObjPath(objExp);
			}
			t = GetToken();
		}
		_tokens.offer(t);

		// ����Ǹ�ֵ�����ߣ��������󶨹���
		if (isLeft) {
			return objExp;
		}
		
		// ���û�н���λ�ã�����λ��Ϊ���һ������֮ǰ��λ��
		if (endPos == -1) {
			endPos = t.StartPos - 1;
		}
		// �����ʼλ��Ϊ"."������֮
		if (startPos < Source.length() && Source.charAt(startPos) == '.') {
			startPos++;
		}
		// ֻ�������Ը�ֵʱ���Ž����󶨹��̣������������ԡ�
		if (endPos > startPos && inExp.type == ExpressionType.Identy) {
			String str = Source.substring(startPos, endPos + 1);
			SetBinding((String) inExp.value, str);
		}
		return objExp;
	}

	// ���󵥸�·��=������([����])?
	private Expression ObjPath(Expression objExp) {
		Token n = GetToken();
		// �������ж�
		if (n.Type == TokenType.Oper && n.Value.equals("[")) {
			Expression exp = Exp();
			// ����']'
			n = GetToken();
			if (n.Type != TokenType.Oper || !n.Value.equals("]")) {
				throw new RuntimeException(GetExceptionMessage("ȱ��']'"));
			}
			// �����������˵��ú���
			Expression result = Expression.Call(objExp, "Where",
					new Expression[] { Expression.Constant(exp.Compile()) });
			return result;
		}
		_tokens.offer(n);
		return objExp;
	}

	// ��������
	private Expression MethodCall(String name, Expression obj) {
		Expression[] ps = Params();
		Token t = GetToken();
		if (t.Type != TokenType.Oper || !t.Value.equals(")")) {
			throw new RuntimeException(GetExceptionMessage("�����������Ų�ƥ��"));
		}
		Expression result = Expression.Call(obj, name, ps);

		return result;
	}

	// ���������б�
	private Expression[] Params() {
		List<Expression> ps = new ArrayList<Expression>();
		Expression exp = Exp();
		// ���exp�к���data����˵���Ǽ��ϴ�������ÿһ��ֵ��Ҫ����Delegate����
		procParam(ps, exp);
		Token t = GetToken();
		while (t.Type == TokenType.Oper && t.Value.equals(",")) {
			exp = Exp();
			procParam(ps, exp);
			t = GetToken();
		}
		_tokens.offer(t);
		return (Expression[]) ps.toArray();
	}

	// ������������exp�к���data����˵���Ǽ��ϴ�������ÿһ��ֵ��Ҫ����Delegate����
	private void procParam(List<Expression> ps, Expression exp) {
		Delegate delegate = exp.Compile();
		if (delegate.objectNames.containsKey("data")) {
			ps.add(Expression.Constant(delegate));
		} else {
			ps.add(exp);
		}
	}

	// �����󶨣��Ѱ󶨵��Ķ�����������ڱ��ʽ������ɺ������ټ�����Щ�����ֵ�仯
	private Expression SetBinding(String o, String path) {
		// �������ֲ�����
		if (path != null) {
			int index = path.indexOf("[");
			if (index != -1) {
				path = path.substring(0, index);
			}
		}
		// ����������󶨣����ظ�����
		BindInfo info = new BindInfo(new String(o), path);
		if (!this.binds.contains(info)) {
			// �Ѱ���Ϣ���������Ա���
			binds.add(info);
		}
		Expression result = Expression.Constant(info);
		return result;
	}

	// ��ȡ�쳣��Ϣ���
	private String GetExceptionMessage(String msg) {
		// �Գ�������λ�ý��д���
		String result = Source.substring(0, pos) + " <- "
				+ Source.substring(pos, Source.length());
		return msg + ", " + result;
	}

	// ��ȡ����
	public Token GetToken() {
		// ����������У�ֱ��ȡ�ϴα����
		if (_tokens.size() != 0) {
			Token result = _tokens.poll();
			return result;
		}
		// ��¼���ʵ���ʼλ�ã������ո������
		int sPos = pos;
		// ������ַ�������״̬���ѳ��������ַ��������ַ�ȫ�����ַ���
		if (inString) {
			int startPos = pos;
			// ����������£��ַ���Ҫʹ��$����
			while (pos < Source.length() && Source.charAt(pos) != '{'
					&& Source.charAt(pos) != '$' && Source.charAt(pos) != '}') {
				pos++;
			}
			// �����ַ����������ڣ�����ԭ���ַ�����������״̬
			if (pos < Source.length() && Source.charAt(pos) == '{') {
				inStrings.push(inString);
				inString = false;
			}
			// �����ַ��˳��ַ��������ڣ��ص���һ����
			else {
				inString = inStrings.pop();
			}
			Token t = new Token(TokenType.String, Source.substring(startPos,
					pos), sPos);
			// ����ǲ���$���ַ��������ˣ�Ҫ��$��ȥ
			if (pos < Source.length() && Source.charAt(pos) == '$')
				pos++;
			return t;
		}
		// ��ȥ���пհ�
		while (pos < Source.length() && Source.charAt(pos) == ' ') {
			pos++;
		}
		// ������ˣ����ؽ���
		if (pos == Source.length()) {
			return new Token(TokenType.End, null, sPos);
		}
		// ��������֣�ѭ����ȡֱ��������Ϊֹ
		if (Source.charAt(pos) >= '0' && Source.charAt(pos) <= '9') {
			int oldPos = pos;
			while (pos < Source.length() && Source.charAt(pos) >= '0'
					&& Source.charAt(pos) <= '9') {
				pos++;
			}
			// ���������"."����double���ֶԴ�����������������
			if (pos < Source.length() && Source.charAt(pos) == '.') {
				pos++;
				while (pos < Source.length() && Source.charAt(pos) >= '0'
						&& Source.charAt(pos) <= '9') {
					pos++;
				}
				// λ�û�ԭ���Ա��´ζ�ȡ
				String str = Source.substring(oldPos, pos + 1);
				return new Token(TokenType.Double, Double.parseDouble(str),
						sPos);
			} else {
				String str = Source.substring(oldPos, pos);
				return new Token(TokenType.Int, Integer.parseInt(str), sPos);
			}
		}
		// ������ַ�������ʶ���Դ�
		else if ((Source.charAt(pos) >= 'a' && Source.charAt(pos) <= 'z')
				|| (Source.charAt(pos) >= 'A' && Source.charAt(pos) <= 'Z')
				|| Source.charAt(pos) == '_') {
			int oldPos = pos;
			while (pos < Source.length()
					&& ((Source.charAt(pos) >= 'a' && Source.charAt(pos) <= 'z')
							|| (Source.charAt(pos) >= 'A' && Source.charAt(pos) <= 'Z')
							|| (Source.charAt(pos) >= '0' && Source.charAt(pos) <= '9') || Source
							.charAt(pos) == '_')) {
				pos++;
			}
			String str = Source.substring(oldPos, pos);
			// ��bool����
			if (str == "False" || str == "True") {
				return new Token(TokenType.Bool, Boolean.parseBoolean(str),
						sPos);
			}
			if (str == "null") {
				return new Token(TokenType.Null, null, sPos);
			}
			return new Token(TokenType.Identy, str, sPos);
		}
		// +��-��*��/��>��<��!�Ⱥ�����Դ�=�Ĵ���
		else if (Source.charAt(pos) == '+' || Source.charAt(pos) == '-'
				|| Source.charAt(pos) == '*' || Source.charAt(pos) == '/'
				|| Source.charAt(pos) == '%' || Source.charAt(pos) == '>'
				|| Source.charAt(pos) == '<' || Source.charAt(pos) == '!') {
			// ���������'='������˫�����������򣬷��ص�������
			if (pos < Source.length() && Source.charAt(pos + 1) == '=') {
				String str = Source.substring(pos, pos + 2);
				pos += 2;
				return new Token(TokenType.Oper, str, sPos);
			} else {
				String str = Source.substring(pos, pos + 1);
				pos += 1;
				return new Token(TokenType.Oper, str, sPos);
			}
		}
		// =�ſ�ʼ�����֣�=����==��=>
		else if (Source.charAt(pos) == '=') {
			if (pos < Source.length()
					&& (Source.charAt(pos + 1) == '=' || Source.charAt(pos + 1) == '>')) {
				String str = Source.substring(pos, pos + 2);
				pos += 2;
				return new Token(TokenType.Oper, str, sPos);
			} else {
				String str = Source.substring(pos, pos + 1);
				pos += 1;
				return new Token(TokenType.Oper, str, sPos);
			}
		}
		// ����������
		else if (Source.charAt(pos) == '(' || Source.charAt(pos) == ')'
				|| Source.charAt(pos) == ',' || Source.charAt(pos) == ';'
				|| Source.charAt(pos) == '.' || Source.charAt(pos) == ':'
				|| Source.charAt(pos) == '@' || Source.charAt(pos) == '$'
				|| Source.charAt(pos) == '{' || Source.charAt(pos) == '}'
				|| Source.charAt(pos) == '[' || Source.charAt(pos) == ']') {
			// �����ַ��������ڣ�����ԭ��״̬
			if (Source.charAt(pos) == '$') {
				inStrings.push(inString);
				inString = true;
			}
			// �ٴν���ԭ���Ļ���
			if (Source.charAt(pos) == '}' && inStrings.size() != 0) {
				inString = inStrings.pop();
			}
			String str = Source.substring(pos, pos + 1);
			pos += 1;
			return new Token(TokenType.Oper, str, sPos);
		} else {
			throw new RuntimeException(GetExceptionMessage("��Ч����"));
		}
	}
}
