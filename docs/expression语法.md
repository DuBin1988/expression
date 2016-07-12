# expression语法

expression是一个纯表达式脚本语言，用来做业务处理逻辑，基本语法与JavaScript一致。主要特点有：

1. 只有表达式处理，没有其它内容。

1. 支持赋值表达式和逗号表达式，如下：
```
a = 5,
b = 6,
a = a + b
```

赋值表达式只支持'='，不支持'+=、-='等内容。

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
  name: $abc$,
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

## 循环

不支持for循环，但是可以调用数组的each函数，对数组里每条数据进行处理，如下：

```
sum = 0,

list.each(
	sum = sum + row.age
)
```
其中'row'代表每条数据。

## 数组访问

可以用[下标]访问数组中的元素，如下：

```
a = array[0]
```

## 注释

只支持单行注释，格式为`//`

## 字符串

字符串按模板字符串写法，如下：
```
user = {
  name: $abc$,
  age: 20
},
a = $用户名字为：{user.name}$
```

## 数据类型

- 数字：整形数按int型处理，浮点数按BigDecimal处理。
- null：空值常数为 `null` 例如：
```
a == null:
  $null$,
a
```
- 逻辑值：常量有'true'和'false'，空值当'false'处理，非空值当'true'处理。
