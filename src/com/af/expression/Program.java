package com.af.expression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.Stack;

public class Program {
	// 源程序
	public String Source;

	// 整个程序的绑定信息，用于外部建立绑定关系
	public Set<BindInfo> bindings = new HashSet<BindInfo>();

	// 单个语句处理过程中，保存已经建立过的绑定信息，已经建立的绑定，不再重复建立
	private Set<BindInfo> binds = new HashSet<BindInfo>();

	// 根节点的编译结果
	private Delegate RootDelegate = null;
	
	// 所有编译好的赋值语句
	private List<Delegate> delegates = new ArrayList<Delegate>();
 
	// Token队列，用于回退
	private Queue<Token> _tokens = new LinkedList<Token>();

	// 当前获取到的字符位置
	private int pos;

	// 当前是否在字符串处理环节
	private boolean inString;

	// 是否在处理赋值语句左边，赋值语句左边对象不进行绑定操作
	private boolean isLeft;

	// 字符串处理环节堆栈，@进入字符串处理环节，“{}”中间部分脱离字符串处理环节
	private Stack<Boolean> inStrings = new Stack<Boolean>();

	public Program(String source) {
		this.Source = source;
	}

	// 调用解析过程
	public Delegate parse() {
		Expression result = CommaExp();
		this.RootDelegate = result.Compile();
		return this.RootDelegate;
	}

	// 获取所有参数名
	public Set<String> getParamNames() {
		Set<String> result = new HashSet<String>();
		// 把所有语句的参数名，统一交给外部，参数名不能重复
		for (Delegate del : this.delegates) {
			result.addAll(del.objectNames.keySet());
		}
		// 把根节点的参数名也返回去
		result.addAll(this.RootDelegate.objectNames.keySet());
		return result;
	}

	// 给参数赋值
	public void putParam(String name, Object value) {
		// 对所有delegate，如果需要该参数，则给其赋值
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
		}
		this._tokens.offer(t);
		
		Expression result = Expression.Comma(exps);
		return result;
	}

	// 赋值表达式=对象属性=一般表达式|一般表达式
	private Expression AssignExp() {
		// 清除语句解析过程中的绑定对象
		this.binds.clear();

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

		// 左边，不建立绑定
		isLeft = true;
		objExp = ObjectPath(objExp);
		isLeft = false;

		// 必须是对象属性
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

		// 从属性Expression中获取对象及属性名
		String name = (String) objExp.value;
		objExp = objExp.children.get(0);
		// 产生赋值语句
		Expression assign = Expression.Assign(objExp, exp, name);
		
		// 把编译结果保存下来
		Delegate del = assign.Compile();
		this.delegates.add(del);

		// 当绑定内容发生变化时，调用赋值过程
		for (BindInfo bind : this.binds) {
			// 增加属性改变监听
			bind.delegates.add(del);
			// 把新的bind加入对外bind中
			this.bindings.add(bind);
		}
		
		return assign;
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
		// 记录绑定所需要的属性路径的开始位置，prevPos为读最后一个单词之前的位置
		int endPos = -1;
		int startPos = pos;

		// 调用条件过滤解析，转换出条件过滤结果
		Expression objExp = ObjPath(inExp);
		Token t = GetToken();
		while (t.Type == TokenType.Oper && t.Value.equals(".")) {
			// 获取对象成员
			Token nameToken = GetToken();
			// 继续看是否方法调用，如果是方法调用，执行方法调用过程
			Token n = GetToken();
			if (n.Type == TokenType.Oper && n.Value.equals("(")) {
				// 进入方法调用，属性路径结束位置为过滤掉方法调用名称的部分
				// 如果已经过滤掉，就不再重新计算位置
				if (endPos == -1) {
					endPos = t.StartPos - 1;
				}
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

		// 如果是赋值语句左边，不建立绑定过程
		if (isLeft) {
			return objExp;
		}
		
		// 如果没有结束位置，结束位置为最后一个单词之前的位置
		if (endPos == -1) {
			endPos = t.StartPos - 1;
		}
		// 如果开始位置为"."，读掉之
		if (startPos < Source.length() && Source.charAt(startPos) == '.') {
			startPos++;
		}
		// 只有在属性赋值时，才建立绑定过程，包括依赖属性。
		if (endPos > startPos && inExp.type == ExpressionType.Identy) {
			String str = Source.substring(startPos, endPos + 1);
			SetBinding((String) inExp.value, str);
		}
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
			Expression result = Expression.Call(objExp, "Where",
					new Expression[] { Expression.Constant(exp.Compile()) });
			return result;
		}
		_tokens.offer(n);
		return objExp;
	}

	// 函数调用
	private Expression MethodCall(String name, Expression obj) {
		Expression[] ps = Params();
		Token t = GetToken();
		if (t.Type != TokenType.Oper || !t.Value.equals(")")) {
			throw new RuntimeException(GetExceptionMessage("函数调用括号不匹配"));
		}
		Expression result = Expression.Call(obj, name, ps);

		return result;
	}

	// 函数参数列表
	private Expression[] Params() {
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
		return (Expression[]) ps.toArray();
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

	// 建立绑定，把绑定到的对象存起来，在表达式解析完成后，外面再监听这些对象的值变化
	private Expression SetBinding(String o, String path) {
		// 条件部分不包括
		if (path != null) {
			int index = path.indexOf("[");
			if (index != -1) {
				path = path.substring(0, index);
			}
		}
		// 如果建立过绑定，不重复建立
		BindInfo info = new BindInfo(new String(o), path);
		if (!this.binds.contains(info)) {
			// 把绑定信息存起来，以便检查
			binds.add(info);
		}
		Expression result = Expression.Constant(info);
		return result;
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
		while (pos < Source.length() && Source.charAt(pos) == ' ') {
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
			throw new RuntimeException(GetExceptionMessage("无效单词"));
		}
	}
}
