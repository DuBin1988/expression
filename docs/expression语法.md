# expression语法

expression是一个纯表达式脚本语言，用来做业务处理逻辑。主要特点有：

1. 只有表达式处理，没有其它内容。

1. 支持赋值表达式和逗号表达式，如下：
```
a = 5,
b = 6,
a = a + b
```

1. 支持条件表达式，如下：

```
a > 5:
  a = 6,
a + 6 > 7:
  a = 8,
a = 10
```
当所有条件都不满足时，必须给一个表达式结果。

1. 支持JSON对象，如下：

```
a = {
  name: 'abc',
  age: 5
},
b = a.age
```

1. 支持java函数调用，如下：

```
a = this.sql($

select * from t_user

$),
b = a.f_name
```

1. 用`()`处理表达式块，主要用于条件表达式处理，如下：

```
(
  a > 5: (
    b = 5,
    c = 6
  ),
  a > 6: (
    b = 7,
    c = 8
  ),
  null
),

// 保存用户对象
this.sql($

update t_user set
  age = {b},
  score = {c}
where name='{a}'

$)

```

## 注释

只支持单行注释，格式为`//`

## 字符串

字符串按模板字符串写法，如下：
```
user = {
  name: 'abc',
  age: 20
},
a = $用户名字为：{user.name}$
```
