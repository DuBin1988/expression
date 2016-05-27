# Expression

表达式编译后，产生AST树，一个Expression代表AST树的一个节点。表达式的执行，相当于AST树节点的执行。

## Delegate

所有AST树节点共享同一个delegate，delegate中存放有变量值，参数值。变量作用域为全局作用域。每个节点都有一个delegate属性，指向公共的delegate。delegate执行前，沿Expression树把自己注入到每个Expression节点中。

### 局部编译

sum求和等函数，可以按如下方式书写：

```
list.sum(data.age + 10)
```
这时，sum函数内部的 `data.age + 10` 需要进行局部编译，局部编译的data代表列表中每项数据。  
__注意:__ 局部编译部分目前不支持全局变量。也就是下面的过程无法执行：

```
a = 5,
list.sum(data.age + a)
```

## Expression 属性

- type：节点类型，具体内容参见 `ExpressionType` 源码
- value：节点值，随节点类型不同，具体参考下面描述。
- children：节点子，随节点类型不同，具体参考下面描述。
- delegate：运行时环境，包括参数及局部变量内容。

## Delegate 属性

- objectNames：存放所有参数及局部变量，可以直接访问
- exp：语法树的根节点。

## ast语法节点

- Comma：value = null, children = 各表达式。
- Assign: value = 属性名,
- Json：value = null, children = 属性值对
- Attr: value = 属性名, children[] = 属性值表达式
