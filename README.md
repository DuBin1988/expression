# expression

提供表达式处理。

## get start

语法处理过程代码如下：
```
// 产生语言编译器
Program prog = new Program(expression);
// 调用编译过程
Delegate d = prog.parse();
// 执行程序
Object actual = d.invoke();
```

## 参数处理
程序执行时，可带参数，参数为Map<String, Object>键值对，如下：
```
Map<String, Object> params = new HashMap<String, Object>();
params.put("a", 5);

d.invoke(params);
```

## 关于回车换行
程序只认`\n`，在windows操作系统上，必须把`\r\n`替换成`\n`。

## 版本发布

发布前，修改build.gradle中版本号，用下列命令发布版本：
```
gradle release
```
