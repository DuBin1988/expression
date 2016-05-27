package com.af.expression;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class Program {
	// 源程序
	public String Source;

	// Token队列，用于回退
	private Queue<Token> _tokens = new LinkedList<Token>();

	// 当前获取到的字符位置
	private int pos;

	// 当前是否在字符串处理环节
	private boolean inString;

	// 字符串处理环节堆栈，@进入字符串处理环节，“{}”中间部分脱离字符串处理环节
	private Stack<Boolean> inStrings = new Stack<Boolean>();

	public Program(String source) {
		this.Source = source;
	}

	// 调用解析过程
	public Delegate parse() {
		Expression result = CommaExp();
		Delegate delegate = result.Compile();
		return delegate;
	}

	// 逗号表达式=赋值表达式(,赋值表达式)*
	private Expression CommaExp() {
		List<Expression> exps = new ArrayList<Expression>();

		Expression first = AssignExp();
		exps.add(first);

		Token t = GetToken();
		// 没有结束
		while (t.Type == TokenType.Oper && t.Value.equals(",")) {
			Expression r = AssignExp();
			exps.add(r);
			t = GetToken();
		}
		this._tokens.offer(t);

		Expression result = Expression.Comma(exps);
		return result;
	}

	// 赋值表达式=对象属性=一般表达式|一般表达式
	private Expression AssignExp() {
		Token t = GetToken();

		// 把第一个Token的位置记录下来，方便后面回退
		int firstPos = t.StartPos;

		if (t.Type != TokenType.Identy) {
			this.pos = firstPos;
			this._tokens.clear();
			this.inString = false;
			return Exp();
		}
		Expression objExp = Expression.Identy((String) t.Value);
		objExp = ObjectPath(objExp);

		// 只能给对象属性或者变量赋值
		if (objExp.type != ExpressionType.Property && objExp.type != ExpressionType.Identy) {
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

		// 如果是属性赋值
		if (objExp.type == ExpressionType.Property) {
			// 从属性Expression中获取对象及属性名
			String name = (String) objExp.value;
			objExp = objExp.children.get(0);
			Expression assign = Expression.Assign(objExp, exp, name);
			return assign;
		} else if (objExp.type == ExpressionType.Identy) {
			// 变量赋值
			Expression assign = Expression.Assign(null, exp, objExp.value.toString());
			return assign;
		}
		throw new RuntimeException(GetExceptionMessage("只能给属性或者变量赋值!"));
	}

	// 表达式=条件:结果(,条件:结果)(,结果)?|结果
	// 条件=单个结果
	private Expression Exp() {
		Expression v = Logic();
		// 是':'，表示条件，否则直接返回单结果
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals(":")) {
			// 第一项转换
			Expression result = Logic();
			// 下一个是","，继续读取下一个条件结果串，由于是右结合，只能采用递归
			t = GetToken();
			if (t.Type == TokenType.Oper && t.Value.equals(",")) {
				// 第二项转换
				Expression sExp = Exp();
				// 返回
				return Expression.Condition(v, result, sExp);
			} else {
				throw new RuntimeException(GetExceptionMessage("必须有默认值!"));
			}
		} else {
			_tokens.offer(t);
			return v;
		}
	}

	// 单个结果项=逻辑运算 (and|or 逻辑运算)* | !表达式
	private Expression Logic() {
		Token t = GetToken();
		// !表达式
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
			// 第二项转换
			Expression exp = Logic();
			// 执行
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

	// 逻辑运算=数字表达式 (比较运算符 数字表达式)?
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

			// 相等比较
			if (t.Value.equals("==")) {
				return Expression.Equal(left, rExp);
			}
			if (t.Value.equals("!=")) {
				return Expression.NotEqual(left, rExp);
			}
		}
		// 返回当个表达式结果
		_tokens.offer(t);
		return left;
	}

	// 单个结果项=乘除项 (+|-) 乘除项
	private Expression Math() {
		Expression v = Mul();
		Token t = GetToken();
		while (t.Type == TokenType.Oper
				&& (t.Value.equals("+") || t.Value.equals("-"))) {
			// 转换操作数2为数字
			Expression r = Mul();

			// 开始运算
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

	// 乘除项=项 (*|/ 项)
	private Expression Mul() {
		Expression v = UnarySub();
		Token t = GetToken();
		while (t.Type == TokenType.Oper
				&& (t.Value.equals("*") || t.Value.equals("/") || t.Value
						.equals("%"))) {
			// 转换第二个为数字型
			Expression r = UnarySub();

			// 开始运算
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

	// 单目运算符
	private Expression UnarySub() {
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals("-")) {
			Expression r = Item();
			return Expression.Subtract(Expression.Constant(0), r);
		}
		_tokens.offer(t);
		return Item();
	}

	// 项=对象(.对象路径)*
	private Expression Item() {
		StringBuilder objName = new StringBuilder();
		// 获取对象表达式
		Expression objExp = ItemHead(objName);
		// 获取对象路径表达式
		objExp = ObjectPath(objExp);
		return objExp;
	}

	// 对象=(表达式)|常数|标识符|绑定序列|字符串拼接序列，
	// 对象解析过程中，如果是标识符，则要返回找到的对象
	private Expression ItemHead(StringBuilder name) {
		Token t = GetToken();
		if (t.Type == TokenType.Oper && t.Value.equals("(")) {
			Expression result = CommaExp();
			t = GetToken();
			if (t.Type != TokenType.Oper || !t.Value.equals(")")) {
				throw new RuntimeException(GetExceptionMessage("括号不匹配"));
			}
			return result;
		} else if (t.Type == TokenType.Oper && t.Value.equals("{")) {
			// json对象
			return Json();
		} else if (t.Type == TokenType.Oper && t.Value.equals("$")) {
			// 字符串拼接序列
			Expression strExp = StringUnion();
			return strExp;
		} else if (t.Type == TokenType.Int || t.Type == TokenType.Double
				|| t.Type == TokenType.Bool) {
			return Expression.Constant(t.Value);
		} else if (t.Type == TokenType.Identy) {
			String objName = (String) t.Value;
			// 把对象名返回
			name.append(objName);
			// 对象名处理
			return ObjectName(objName);
		} else if (t.Type == TokenType.Null) {
			return Expression.Constant(null);
		}
		throw new RuntimeException(GetExceptionMessage("单词类型错误，" + t));
	}
	
	// JSON对象::={}|{属性名:属性值,属性名:属性值}
	private Expression Json() {
		List<Expression> children = new LinkedList<Expression>();
		
		Token t = GetToken();
		// 空对象，直接返回
		if (t.Type == TokenType.Oper && t.Value.equals("}")) {
			return Expression.Json(children);
		}
		
		_tokens.offer(t);
		children.add(Attr());
		t = GetToken();
		// 如果是"," 继续看下一个属性
		while (t.Type == TokenType.Oper && t.Value.equals(",")) {
			children.add(Attr());
			t = GetToken();
		}
		if (t.Type != TokenType.Oper && t.Value.equals("}")) {
			throw new RuntimeException(GetExceptionMessage("必须是'}'"));
		}
		
		return Expression.Json(children);
	}
	
	// 属性值对::=属性名: 属性值
	private Expression Attr() {
		Token name = GetToken();
		if (name.Type != TokenType.Identy) {
			throw new RuntimeException(GetExceptionMessage("JSON对象必须是属性名"));
		}
		Token t = GetToken();
		if (t.Type != TokenType.Oper || !t.Value.equals(":")) {
			throw new RuntimeException(GetExceptionMessage("必须是':'"));
		}
		Expression exp = Exp();
		return Expression.Attr(name.Value.toString(), exp);
	}
	
	// 对字符串拼接序列进行解析
	private Expression StringUnion() {
		// 字符串连接方法
		Expression exp = Expression.Constant("");
		Token t = GetToken();
		// 是对象序列
		while ((t.Type == TokenType.Oper && t.Value.equals("{"))
				|| t.Type == TokenType.String) {
			// 字符串，返回字符串连接结果
			if (t.Type == TokenType.String) {
				exp = Expression.Concat(exp, Expression.Constant(t.Value));
			} else {
				// 按表达式调用{}里面的内容
				Expression objExp = Exp();
				t = GetToken();
				if (t.Type != TokenType.Oper || !t.Value.equals("}")) {
					throw new RuntimeException(GetExceptionMessage("缺少'}'"));
				}
				// 字符串连接
				exp = Expression.Concat(exp, objExp);
			}
			t = GetToken();
		}
		_tokens.offer(t);
		return exp;
	}

	// 根据名称获取对象以及对象表达式
	private Expression ObjectName(String objName) {
		Expression objExp = Expression.Identy(objName);
		return objExp;
	}

	private Expression ObjectPath(Expression inExp) {
		// 调用条件过滤解析，转换出条件过滤结果
		Expression objExp = ObjPath(inExp);
		Token t = GetToken();
		while (t.Type == TokenType.Oper && t.Value.equals(".")) {
			// 获取对象成员
			Token nameToken = GetToken();
			// 继续看是否方法调用，如果是方法调用，执行方法调用过程
			Token n = GetToken();
			if (n.Type == TokenType.Oper && n.Value.equals("(")) {
				String name = (String) nameToken.Value;
				objExp = MethodCall(name, objExp);
			} else {
				_tokens.offer(n);
				// 取属性
				String pi = (String) nameToken.Value;
				objExp = Expression.Property(objExp, pi);
				// 调用条件过滤解析，产生条件过滤结果
				objExp = ObjPath(objExp);
			}
			t = GetToken();
		}
		_tokens.offer(t);

		return objExp;
	}

	// 对象单个路径=属性名([条件])?
	private Expression ObjPath(Expression objExp) {
		Token n = GetToken();
		// 是条件判断
		if (n.Type == TokenType.Oper && n.Value.equals("[")) {
			Expression exp = Exp();
			// 读掉']'
			n = GetToken();
			if (n.Type != TokenType.Oper || !n.Value.equals("]")) {
				throw new RuntimeException(GetExceptionMessage("缺少']'"));
			}
			// 产生条件过滤调用函数
			List<Expression> params = new LinkedList<Expression>();
			params.add(Expression.Constant(exp.Compile()));
			Expression result = Expression.Call(objExp, "Where", params);
			return result;
		}
		_tokens.offer(n);
		return objExp;
	}

	// 函数调用
	private Expression MethodCall(String name, Expression obj) {
		List<Expression> ps = Params();
		Token t = GetToken();
		if (t.Type != TokenType.Oper || !t.Value.equals(")")) {
			throw new RuntimeException(GetExceptionMessage("函数调用括号不匹配"));
		}
		Expression result = Expression.Call(obj, name, ps);

		return result;
	}

	// 函数参数列表
	private List<Expression> Params() {
		List<Expression> ps = new ArrayList<Expression>();
		Expression exp = Exp();
		// 如果exp中含有data对象，说明是集合处理函数中每一项值，要当做Delegate处理。
		procParam(ps, exp);
		Token t = GetToken();
		while (t.Type == TokenType.Oper && t.Value.equals(",")) {
			exp = Exp();
			procParam(ps, exp);
			t = GetToken();
		}
		_tokens.offer(t);
		return ps;
	}

	// 处理参数，如果exp中含有data对象，说明是集合处理函数中每一项值，要当做Delegate处理。
	private void procParam(List<Expression> ps, Expression exp) {
		Delegate delegate = exp.Compile();
		if (delegate.objectNames.containsKey("data")) {
			ps.add(Expression.Constant(delegate));
		} else {
			ps.add(exp);
		}
	}

	// 获取异常信息输出
	private String GetExceptionMessage(String msg) {
		// 对出错的语句位置进行处理
		String result = Source.substring(0, pos) + " <- "
				+ Source.substring(pos, Source.length());
		return msg + ", " + result;
	}

	// 获取单词
	public Token GetToken() {
		// 如果队列里有，直接取上次保存的
		if (_tokens.size() != 0) {
			Token result = _tokens.poll();
			return result;
		}
		// 记录单词的起始位置，包括空格等内容
		int sPos = pos;
		// 如果是字符串处理状态，把除过特殊字符的所有字符全部给字符串
		if (inString) {
			int startPos = pos;
			// 在特殊情况下，字符串要使用$结束
			while (pos < Source.length() && Source.charAt(pos) != '{'
					&& Source.charAt(pos) != '$' && Source.charAt(pos) != '}') {
				pos++;
			}
			// 脱离字符串操作环节，保存原来字符串操作环节状态
			if (pos < Source.length() && Source.charAt(pos) == '{') {
				inStrings.push(inString);
				inString = false;
			}
			// 其他字符退出字符串处理环节，回到上一环节
			else {
				inString = inStrings.pop();
			}
			Token t = new Token(TokenType.String, Source.substring(startPos,
					pos), sPos);
			// 如果是采用$让字符串结束了，要把$读去
			if (pos < Source.length() && Source.charAt(pos) == '$')
				pos++;
			return t;
		}
		// 读去所有空白
		while (pos < Source.length() && (Source.charAt(pos) == ' ' || Source.charAt(pos) == '\n')) {
			pos++;
		}
		// 如果是 "//" 读去所有注释，到行尾都是注释
		if (pos < Source.length() - 2 && Source.charAt(pos) == '/' && Source.charAt(pos + 1) == '/') {
			pos += 2;
			while (pos < Source.length() && Source.charAt(pos) != '\n') {
				pos++;
			}
			// 读掉行尾
			pos++;
		}
				
		// 如果完了，返回结束
		if (pos == Source.length()) {
			return new Token(TokenType.End, null, sPos);
		}
		// 如果是数字，循环获取直到非数字为止
		if (Source.charAt(pos) >= '0' && Source.charAt(pos) <= '9') {
			int oldPos = pos;
			while (pos < Source.length() && Source.charAt(pos) >= '0'
					&& Source.charAt(pos) <= '9') {
				pos++;
			}
			// 如果后面是"."，按double数字对待，否则按整形数返回
			if (pos < Source.length() && Source.charAt(pos) == '.') {
				pos++;
				while (pos < Source.length() && Source.charAt(pos) >= '0'
						&& Source.charAt(pos) <= '9') {
					pos++;
				}
				// 位置还原，以便下次读取
				pos--;
				String str = Source.substring(oldPos, pos + 1);
				return new Token(TokenType.Double, Double.parseDouble(str),
						sPos);
			} else {
				String str = Source.substring(oldPos, pos);
				return new Token(TokenType.Int, Integer.parseInt(str), sPos);
			}
		}
		// 如果是字符，按标识符对待
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
			// 是bool常量
			if (str == "False" || str == "True") {
				return new Token(TokenType.Bool, Boolean.parseBoolean(str),
						sPos);
			}
			if (str == "null") {
				return new Token(TokenType.Null, null, sPos);
			}
			return new Token(TokenType.Identy, str, sPos);
		}
		// +、-、*、/、>、<、!等后面可以带=的处理
		else if (Source.charAt(pos) == '+' || Source.charAt(pos) == '-'
				|| Source.charAt(pos) == '*' || Source.charAt(pos) == '/'
				|| Source.charAt(pos) == '%' || Source.charAt(pos) == '>'
				|| Source.charAt(pos) == '<' || Source.charAt(pos) == '!') {
			// 后面继续是'='，返回双操作符，否则，返回单操作符
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
		// =号开始有三种，=本身，==，=>
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
		// 单个操作符
		else if (Source.charAt(pos) == '(' || Source.charAt(pos) == ')'
				|| Source.charAt(pos) == ',' || Source.charAt(pos) == ';'
				|| Source.charAt(pos) == '.' || Source.charAt(pos) == ':'
				|| Source.charAt(pos) == '@' || Source.charAt(pos) == '$'
				|| Source.charAt(pos) == '{' || Source.charAt(pos) == '}'
				|| Source.charAt(pos) == '[' || Source.charAt(pos) == ']') {
			// 进入字符串处理环节，保留原来状态
			if (Source.charAt(pos) == '$') {
				inStrings.push(inString);
				inString = true;
			}
			// 再次进入原来的环节
			if (Source.charAt(pos) == '}' && inStrings.size() != 0) {
				inString = inStrings.pop();
			}
			String str = Source.substring(pos, pos + 1);
			pos += 1;
			return new Token(TokenType.Oper, str, sPos);
		} else {
			throw new RuntimeException(GetExceptionMessage("无效单词：" + Source.charAt(pos)));
		}
	}
}
