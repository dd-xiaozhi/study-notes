佛曰: 

写字楼里写字间，写字间里程序员； 

程序人员写程序，又拿程序换酒钱。 

酒醒只在网上坐，酒醉还来网下眠； 

酒醉酒醒日复日，网上网下年复年。 

但愿老死电脑间，不愿鞠躬老板前； 

奔驰宝马贵者趣，公交自行程序员。 

别人笑我忒疯癫，我笑自己命太贱； 

不见满街漂亮妹，哪个归得程序员？ 

噢耶！！！





# 一、基础入门

## 1	注释

```python
# 单行注释

"""
	多行注释 
"""
```



## 2	变量

### ①变量的分类和声明

<img src="E:\学习笔记\图片\20210913155906-16629720934532.png" alt="20210913155906" style="zoom:67%;" />

在python中，只要定义了一个变量，而且它有数据，那么它的类型就已经确定了，不需要咱们开发者主动的 去说明它的类型，**系统会自动辨别。也就是说在使用的时候，变量没有类型，数据才有类型**。

```python
# 变量的使用
a = 20
b = 12.0
```



**使用复数**

虚部后面必须是`j`

```python
c1 = 12 + 0.2j
print(c1)

c2 = 6 - 1.2j
# 复数计算
print(c1 + c2)
```





### ②type()查看变量的数据类型

```python
# 使用type()来检测变量的类型
a = 20
b = 20.0
c = 1 + 5j
d = 'hello world'
e = True
print(type(a))
print(type(b))
print(type(c))
print(type(d))
print(type(e))
```



### ③命名规范

1.   标识符由字母、下划线和数字组成，且数字不能开头。 

2.   严格区分大小写。 

3.   不能使用关键字。

Python的命令规则遵循PEP8标准  -  用下划线“_”来连接所有的单词，比如send_buf. 



### ④关键字

![image-20210913160512312](E:\学习笔记\save-img-upload-img/img/20210913160513.png)



### ⑤类型转换

![image-20210914153105782](E:\学习笔记\save-img-upload-img/img/20210914153107.png)

**常用的几个**

![image-20210913160541915](E:\学习笔记\save-img-upload-img/img/20210913160543.png)

**补充**：chr()：将整数转换为该编码对应的字符串（一个字符）

​				![image-20210913161251302](E:\学习笔记\save-img-upload-img/img/20210913161252.png)

​		   ord()：将字符串（一个字符）转换为对应的编码（整数）

**代码演示**

```python
# 类型转换

a = '2'
b = 30
c = 1
d = ""
l = (29, 230, 23)
f = '[20, 34, 25, 342]'

print(eval(f))      # 将一个对象转换为原来的数据类型，结构就是list类型
print(list(l))      # 将一个系列转化为列表
print(int(a))       # 2
print(float(a))     # 2.0
print(str(b))       # 30
print(bool(c))      # True
print(chr(b))       # RS
print(ord(d))       # 30
```

​	

### ⑥交换变量值

**需求**：有变量 a = 10 和 b = 20 ，交换两个变量的值。

```python
# 方式一
a = 10
b = 20
# 定义中间变量
c = 0
c = a
a = b
b = c
print(a) # 20
print(b) # 10

# 方式二
a, b = 1, 2
a, b = b, a
print(a) # 2
print(b) # 1
```





## 3	运算符

![image-20210913161536160](E:\学习笔记\save-img-upload-img/img/20210913161537.png)

==**python的是逻辑与、逻辑非和逻辑非分别是and、or、not**==



**三目元算符（三元运算符）**

值1 if 表达式 else 值2	-->	如果if里面是true就输出值1，相反输出值2

```python
print('aaa') if 1 == 1 else print('23243')  # 输出aaa
```









# 二、流程控制语句

## python没有switch



## 1	for和range

range 可以生成数字供 for 循环遍历,它可以传递三个参数，**分别表示 起始、结束和步长**

-   起始值默认为0
-   步长为负数是递减，正数是递增
-   步长默认为1
-   不包括结束值，比如 起始0、结束10，那么就是循环9次



**代码演示**

```python
# 用for循环来遍历

# 小于结束值，步长不写默认为1
for x in range(0, 10):
    print(x)
```



**练习**

```python
# 循环练习
"""
    *
    **
    ***
    ****
    *****
"""
# input方法返回值是字符串类型
row = int(input("请输入行数"))
for i in range(row):
    for j in range(i + 1):
        # print函数会自动换行，end=表示结尾是什么，比如end=';'，那么最终输出的就会以 ; 结束
        print('*', end='')
    print()
    
    
'''
    *
   **
  ***
 ****
*****
'''

row = int(input("请输入行数"))
for i in range(row):
    for j in range(row - 1 - i):
        print(" ", end='')
    for j in range(i + 1):
        print("*", end='')
    print()
    
    
"""
    *
   ***
  *****
 *******
*********
"""
row = int(input("请输入行数"))
for i in range(row):
    for _ in range(row - 1 - i):
        print(' ', end='')
    for _ in range(2 * i - 1):
        print('*', end='')
    print()
```



## 2	while

```python
# 玩转while

a = True
b = 1000
c = 1
while(a):
    if c > b:
        print('程序结束')
        break
    print(c)
    c += 1
```



# 三、高级数据类型

## 1	字符串

### ①	字符串的写法

```python
# 字符串的写法
str = "孔某是傻逼"
# 这里要注意一下，我们输入空格的话，它输出的时候会连空格一起输出
str2 = """
孔某是某靓仔智的儿子
孔某本身也是个傻逼
"""
print(str)
print(str2)
```



**字符串中使用`\`（反斜杠）或`r`来表示转义**

**例如**：`\n`不是代表反斜杠和字符n，而是表示换行；而`\t`也不是代表反斜杠和字符t，而是表示制表符。所以如果想在字符串中表示`'`要写成`\'`，同理想表示`\`要写成`\\`。

```python
# 转义字符\
str3 = '\'helloWorld\''  # 输出的结果 'helloWorld'
str4 = '\\helloWorld\\'  # 输出的结果 /helloWorld/
print(str3)
print(str4)
```



**在`\`后面还可以跟一个八进制或者十六进制数来表示字符**

**例如**：`\141`和`\x61`都代表小写字母`a`，前者是八进制的表示法，后者是十六进制的表示法。也可以在`\`后面跟Unicode字符编码来表示字符，例如`\u5c0f\u667a`代表的是中文“小智”。

```python
# 转义字符后面跟八进制和十六进制
str5 = '\141\142\143\x61\x62\x63'
str6 = '\u5c0f\u667a'
print(str5, str6)
```



**在字符串前面加r，`\`就不会转义**

```python
# 让\不转义
str7 = r'\'abcd\''
str8 = r'\n\\'
print(str7, str8,end='')     # 结果显示\'abcd\' \n\\
```



### ②	字符串的运算符

-   `+`运算符来实现字符串的拼接
-   `*`运算符来重复一个字符串的内容，
-   `in`和`not in`来判断一个字符串是否包含另外一个字符串（成员运算），
-   `[]`和`[:]`运算符从字符串取出某个字符或某些字符（切片运算）

**代码演示**

```python
# 字符串运算符的使用
str = 'hello'
str2 = 'world'
# + 字符串拼接
str3 = str + str2
print(str3)

# in 来判断字符串中是否含有对应的字符，not in相反
boo1 = 'he' in str
boo2 = 'si' in str
print(boo1, boo2)   # True False

# 通过[]和[:]取出对应的字符
# [起始:结束] --> 包含起始，不包含结束
str4 = 'asfdsfsd'
print(str4[2])      # f
print(str4[2:4])    # fd
# 也可以这样表示 --> [起始:结束:步长]
print(str4[2::4])   # fs
print(str4[::2])    # afss
print(str4[6::-3])  # sda
# 起始值为负数，从后面数起，不包括起始位置的值
print(str4[-4:-1])  # sfs
```



### ③	格式化输出字符串

```python
# 字符串的格式化
a, b = 5, 10
print('%d * %d = %d' % (a, b, a * b))

# 也可以使用{下标}占位
a, b = 5, 10
print('{0} * {1} = {2}'.format(a, b, a * b))

# 使用f'表达式'来输出
a, b = 5, 10
print(f'{a} * {b} = {a * b}')

# 标明所有参数输出
'''
	0：索引位置
	*：多出的位置需要填充的字符
	>：右对齐，<左对齐，^居中
	10：宽度，字符显示的宽度，多出的会被截断，少了会被字符填充
	,：为数字添加千分符
	.2：小数点后几位
	f：指定数据类型，如果规定了指定类型，那么接收参数的时候就必须是指定类型
	注：上面的参数全部为可选参数
'''
print('{0:*>10,.2f}'.format(12334.2342))
# 报错，int类型不能接收浮点型
print('{0:*>10,d}'.format(12334.2342))
```



### ④	字符串函数的使用

```python
# 字符串函数的使用

str = 'hello，world'
# 计算字符串的长度
print(len(str))

# 得到字符串首字母大写的拷贝
print(str.capitalize())     # Hello，world

# 得到每个单词首字母大写字符串的拷贝
print(str.title())  # Hello，World

# 得到全部大写字符串的拷贝
print(str.upper())

# 得到全部小写字符串的拷贝
print(str.lower())

# 查找字符在字符串的索引值
print(str.find('or'))       # 7
print(str.find('str'))      # 没找到返回-1
# print(str.index('or'))    #
# print(str.index('str'))   # 没找到会报错，慎用

# 返回字符串中字符出现的次数
print(str.count('o'))
print(str.count('o', 1, 5))

# 检查字符串是否以指定的字符开头，是返回true，否者返回false
print(str.startswith('He'))     # False
print(str.startswith('hel'))    # true
print(str.startswith('hel', 0, 3))

# 检查字符串是否以指定的字符结束
print(str.endswith('he'))       # False
print(str.endswith('world'))    # True

# 将字符串以指定的宽度居中并在两侧填充指定的字符
print(str.center(20, '*'))  # *******************hello，world********************

# 将字符串以指定的宽度靠右放置左侧填充指定的字符
print(str.rjust(20, '*'))   # ***************************************hello，world

# 将字符串以指定的宽度靠左放置左侧填充指定的字符
print(str.ljust(20, '*'))

str2 = 'abc123456'
# 检查字符串是否由数字组成
print(str2.isdigit())

# 检查字符串是否由字母组成
print(str2.isalpha())

# 检查字符串是否由字母和数字组成
print(str2.isalnum())

str3 = '   23423   @qq.com   '
# 获取去除字符串前后空格的拷贝,中间的空格是不会被去除的
print(str3.strip())
print(str3.lstrip())    # 去除左边的
print(str3.rstrip())    # 去除右边的

# 字符串替换后的拷贝，参数1是target，参数2是要进行替换的字符，第3个参数是要替换多少个
print(str3.replace('234', '113', 2))

# 字符串的切割
list1 = str3.split('   ')

# 字符串合并
print('-'.join(list1))

# 判断是否全是空白
str4 = '   '
print(str4.isspace())
print(str.isspace())
```



### ⑤ord函数和chr函数

-   ord：将字符串转换为ASCLL码
-   chr：将ASCLL码转换为字符串





## 2	列表

就是数组



### 01	快速入门

```python
# 列表入门

salary = [1000, 20000, 3000, 50000]
length = len(salary)
# 第一种遍历方式 --> 通过下角标获取元素
for index in range(length):
    print(salary[index])

# 第二种遍历方式 --> 直接遍历元素
for elem in salary:
    print(elem)

# 第三种遍历方式 --> 通过enumerate函数可以将下标和元素一起遍历出来
for index, elem in enumerate(salary):
    print(index, elem)
```

**补充**：

```python
list2 = salary * 2
print(list2)    # [1000, 20000, 3000, 50000, 1000, 20000, 3000, 50000]
```



### 02	列表的增删改查

#### ① 添加和插入

```python
# 添加和插入元素
list = [2, 34, 22, 54]
# 直接在末尾处添加一个元素
list.append('理解')
# 插入指定位置，如果插入位置大于或者等于数组长度，那么就会插入到最后位置
list.insert(12, '小智')
for elem in list:
    print(elem)
```



#### ② 合并

```python
# 合并两个列表
list = [1, 34, 35, 32]
list2 = [3, 343, 22, 21]
list.extend(list2)  # list2会追加到list1后
list2 += [34, 23, 34]   # 这种方式也是可以的
print(list)
print(list2)
```



#### ③ 删除

```python
# 删除元素
list = [1, 2, 3, 4, 4, 3]

# remove移除对应的元素，并不是移除对应下角标的元素
if 4 in list:   # 如果4在这个列表中存在，那么就移除
    list.remove(4)
if 1234 in list:    # 不存在的数据就不会移除
    list.remove(1234)
print(list)

# pop是移除指定下角标的元素
list.pop(3)
print(list)

# 清空整个列表
list.clear()
print(len(list))    # 0
```



#### ④ 切片

```python
# 列表切片

list1 = ['xiaozhi', 'heigui', 'goumo','niuniu']
print(list1[1::])       # ['heigui', 'goumo', 'niuniu']
print(list1[::-3])      # ['niuniu', 'xiaozhi']
print(list1[1:4:2])     # ['heigui', 'niuniu']
print(list1[-3:3:1])    # ['heigui', 'goumo']
```



### 03	列表的排序

```python
# 列表的排序
list1 = [23, 34, 22223, 2]
list1.sort()    # 将列表正序排序
list11 = [23, 34, 22223, 2]
list11.sort(reverse=True)
print(list1)
print(list11)

list2 = ['yasdff', 'bcsdf', 'yadf', 'sdfsdfsd']
list3 = sorted(list2)           # 根据字符串的首字母按字母表的顺序排序
list4 = sorted(list2, key=len)  # 有ken=len就是按照字符串长度来进行排序
list5 = sorted(list2, reverse=True)     # reverse=True就是逆序

print(list3)
print(list4)
print(list5)
```



### 04	生成式和生成器

```python
import sys
# 生成式和生成器
# 第一种方式 --> 这个接收的变量名要一致
f1 = [ x for x in range(1, 10)]
print(f1)
# +号在这里是连接的作用,比如 1 + 2 = 12
# 下面的例子是嵌套循环，要注意
f2 = [x + y for x in 'ABCDE' for y in '1234567']
print(f2)

# **的作用就是次方
f3 = [x ** 2 for x in range(1, 1000)]
print(f3)

# 查看占用的字节数，和c的sizeof作用一样
print(sys.getsizeof(f3))
```



### 05	斐波拉切数列

通过`yield`关键字将一个普通函数改造成生成器函数

```python
# 斐波拉切数列
def fib(n):
    a, b = 0, 1
    for _ in range(n):
        a, b = b, a + b
        yield a

def main():
    for val in fib(20):
        print(val)


if __name__ == '__main__':
    main()
```



## 3	元组

元祖的使用方式和列表是类似的，但是不同的是元祖不能修改，列表是可以修改的，元祖使用小括号，列表使用方括号

**代码实现**

```python
# 元组使用
t = ('小智', 18, True, '广东')
print(t)
# 获取元素中的数据
print(t[0])
print(t[3])
# 遍历元组
for elem in t:
    print(elem)

# 将元组转列表
print(type(t))
print(type(list(t)))

f_list = ['service', 'yoy', 'got']
print(type(tuple(f_list)))
```

**元组列表的比较**

-   元组不能修改，所以如果需要固定数据的话可以使用元组

-   元组的占用内存是比列表要少的

    ```python
    # 元组和列表的比较
    import sys
    t = (1, 2, 3)
    l = [1, 2, 3]
    # 列表的内存时比元组占用的要多
    print(sys.getsizeof(t))     # 72
    print(sys.getsizeof(l))     # 88
    ```

-   元组如果修改的话会报错，这个要注意一下



## 4	集合

Python中的集合跟数学上的集合是一致的，**不允许有重复元素**，而且**可以进行交集、并集、差集等运算**。

**集合是无序的，所以不能通过下角标来取值，同时还去重**



### ①入门

```python
# 集合入门
set1 = {1, 3, 32, 2, 3}
print(set1)     # 不会输出重复的数
# 是一个的情况可以不适用占位符
print('Length=', len(set1))

# 创建集合的构造器语法
set2 = set(range(1, 10))
set3 = set((1, 2, 3, 4))
print(set2, set3)

# 创建集合的推导式语法(推导式也可以用于推导集合)
set4 = {num for num in range(1, 100) if num % 3 == 0 or num % 5 == 0}
print(set4)
```



### ②添加和删除

```python
# 集合的添加和删除
set1 = {1, 3, 32, 2, 3}
set1.add('4')
set1.update([11, 12])   # 在最后添加
print(set1)
# 取出元素，从第一个开始取，取出原集合就没有这个元素了
print(set1.pop())
# 移除对应元素
set1.remove(1)      # 如果不存在对应的数据，那么就会报错 
set1.discard(3)     # 移除指定的元素,如果没有对应的数据也不会报错
# 上面的功能和下面的功能一样
if 4 in set1:
    set1.remove(4)
print(set1)
```



### ③交集并集差集运算

```python
# 集合的交集、并集、差集、对称差运算
set1 = {1, 2, 3, 4}
set2 = {3, 4, 5, 6}
set3 = {1, 2}

# 交集的两种写法
print(set1 & set2)
print(set1.intersection(set2))

# 并集的两种做法
print(set1 | set2)
print(set1.union(set2))

# 差集的两种做法
print(set1 - set2)  # 就是set2没有的元素
print(set1.difference(set2))

# 对称差的两种做法
print(set1 ^ set2)  # 双方都不一相同的数
print(set1.symmetric_difference(set2))

# 判断子集和超集   -->    是否包含对方，是返回true，否返回false
print(set1 >= set2)
print(set3 <= set1)
```



## 5	字典

键值对容器，类似java的map集合，键和值用':'隔开

```python
# 字典语法练习
# 普通的创建方式 --> 键是要字符串，不能是其他的
student = {'01': '小黑', '02': '黑仔', '03': '傻逼孔'}

print(student)
# 使用构造器的方式来创建
student2 = dict(one='小智', two='小锋', three='小黑')
print(student2)
# 使用zip来将两个序列
student3 = dict(zip(['a', 'b', 'c'], '123'))
print(student3)

# 获取值 --> 通过键来取值
print(student['01'])        # 对应的键来取值
print(student.get('02'))    # get方法取出

# 遍历，键和值一起输出
for key in student:
    print(f'{key}, {student[key]}')
    print('{0},{1}'.format(key, student[key]))
    print('%s %s' % (key, student[key]))

# 判断是否存在对应的key，存在就取出对应的值
if '01' in student:
    print(student['01'])

# 更新字典中的数据
student['01'] = '黑鬼'
print(student['01'])

# 从字典中取出对应的值，取出之后在字典中就没有了，一次只能取出一个
a = student.pop('01')
print(student, a)   # 取出指定的key对应的值
print(student.popitem())    # 取出第一个

# 清空字典
student2.clear()
print(student2)     # {}
```



## 6	拆包

就是将容器中的值拆成一个单独的变量

```python
# 元组拆包
tuple1 = (100, 200, 300)
num1, num2, num3 = tuple1
print(num1, num2, num3)

# 字典拆包
dict1 = {'name':'小智', 'age':'20'}
d1, d2 = dict1
# 得到key值
print(d1, d2)
# 得到value值
print(dict1[d1], dict1[d2])
```



# 四、函数和模块的使用

## 01 快速入门

函数的作用就是将重复使用的东西封住起来，使用的时候直接调函数

python中用**def关键字定义函数**，括号里面是形参，return关键字返回一个值，函数的命名规则和变量名一样

**代码演示**

```python
# 函数入门
# 定义一个求阶乘的函数
def fac(num):
    result = 1
    for n in range(1, num + 1):
        result *= n
    return result

# 调用函数
print(fac(3))
```



### 函数帮助文档

help(想知道的方法)：得到对应方法的帮助文档



## 02	return返回多个值

```python
# 返回多个值
def test2():
    return 50, 100

print(type(test2()))    # 多个参数是以元组的形式返回
```





## 03	函数的参数

python 不支持重载，但是它有替代重载的方案



**代码演示**

```python
# 函数的参数
from random import randint

def roll_dice(n=2):
    """摇色子"""
    total = 0
    for _ in range(n):
        total += randint(1, 6)
    return total

# 缺省参数 -->	定义参数的时候定义形参的默认值
def add(a=0, b=0, c=0):
    """三个数相加"""
    return a + b + c
def add2(a, b):
    return a + b

# 如果没有指定参数那么使用默认值摇两颗色子
print(roll_dice())      # 我们直接调用的话，传入的参数就是函数形参的默认值 
# 摇三颗色子
print(roll_dice(3))     # 有传入形参，那么就使用我们自己的形参
print(add())
print(add(1))
print(add(1, 2))
print(add(1, 2, 3))
# 键值对的形式传递参数可以不按照设定的顺序进行传递，相反的就需要按照位置来传入对应的值
print(add(c=50, a=100, b=200))
print(add2())	#
```

==**注意**：如果不传入对应的参数就会使用函数默认的参数，前提是定义的参数它是有默认值的，否者就会报错==



### 可变形参

**说明**：*的可变形参是元组类型，**的可变形参是字典类型，传递参数的时候要使用键值对的形式

```python
# 可变形参 --> 可变形参的类型是元组类型 
def add(*args):
    total = 0
    for n in args:
        total += n
    return total

print(add(1, 2, 3, 4, 5))

# 两个*的就是代表是一个字典类型的可变形参
# 在传递参数的时候要使用键值对的形式传递
def user_info(**kwargs):
    print(kwargs)
user_info(age=100, name='李四', money=123)
```



## 04	模块化管理

python是没有重载的，所以如果存在两个重名的函数，后面定义的会覆盖前面定义的，所以如果想要起两个同名的参数，那么我们就需要模块化管理，python中每个文件就代表了一个模块

```python
module1.py
def foo():
    print("我是moduler1中的foo")
    
module2.py
def foo():
    print("我是moduler2中的foo")
    
test.py
# 模块化测试
from module1 import foo
foo()   # 我是moduler1中的foo

from module2 import foo
foo()   # 我是moduler2中的foo

# 注意：后导入的会覆盖前导入的
from module1 import foo
from module2 import foo

foo()   # 我是moduler2中的foo
```

也可以通过起别名的方式来区分

```python
test.py
# 模块化测试
from module1 as m1
from module1 as m2
m1.foo()
m2.foo()
```



## 05	全局变量和局部变量

函数外的就是全局，函数内的就是局部

```python
# 全局变量和局部变量
def foo():
    b = 'hello'

    # Python中可以在函数内部再定义函数
    def bar():
        c = True
        print(a)
        print(b)
        print(c)

    bar()
    # print(c)  # NameError: name 'c' is not defined


if __name__ == '__main__':
    a = 100
    # print(b)  # NameError: name 'b' is not defined
    foo()
```



**局部变量内修改全局变量**

```python
def foo():
    global a	# global声明的变量就是全局变量，如果全局作用域中没有a，那么下面一行的代码就会定义变量a并将其置于全局作用域
    a = 200		# 如果没有global关键字，那么它其实就是一个局部变量，全局变量a不会被修改
    print(a)  # 200


if __name__ == '__main__':
    a = 100
    foo()
    print(a)  # 200
```



## 06	引用

**在python中，值是靠引用来传递来的**

**我们可以用** id() **来判断两个变量是否为同一个值的引用。** 我们可以将id值理解为那块内存的地址标识。相当于是c的&号

```python
# int类型
a = 2
b = a
# a和b的地址值是相同的，因为b引用的就是a的地址值
print(id(a))    # 140721634779568
print(id(b))    # 140721634779568

a = 3
# a的值改变，b没有跟着改变
'''
    说明：a变量引用的是2的地址值，然后a将2的地址值又赋给了b,
         所以b的地址值就是2的，所以当a引用其他的值时，b是不会有改变的
'''
print(id(a))    # 140721634779600
print(id(b))    # 140721634779568

# 列表类型
list1 = [10, 20]
list2 = list1
# 两者的地址是一致的
print(id(list1))    # 1579426664840
print(id(list2))    # 1579426664840

# list1添加一个元素
list1.append(30)

# 两者的地址还是一样的
# 说明：因为修改列表后，a的引用没有发生改变，还是引用开始的列表
print(id(list1))    # 1579426664840
print(id(list2))    # 1579426664840
```



## 07	递归

我调我自己，就是玩~~~，但是不要把自己玩死了，递归要有出口，也就是有结束的语句滴

```python
# 3以内数字累加
def num_add(num):
    if num == 1:
        return 1
    return num + num_add(num - 1)

print(num_add(3))	# 6
```



## 08	 lambda表达式

### ①快速入门

如果一个函数有一个返回值并且只有一句代码，可以使用lambda表达式来简化

**语法**：lambda 参数列表 : 表达式

```python
def f1():
    return 200
print(f1())

# lambda表达式
# 不用写return关键字，直接写要返回的值即可
f2 = lambda : 100
print(f2())

# 实例：计算a+b
sum_num = lambda a, b : a + b
print(sum_num(1, 2))
```



### ②lambda的参数形式

```python
# 无参
f1 = lambda : 100
print(f1())     # 100

# 有一个参数
f2 = lambda a : a
print(f2(2))    # 2

# 一个或者一个以上参数的要显示的写出来，不能为空
f3 = lambda a, b : a + b
print(f3(2, 4)) # 6

# 默认参数
f4 = lambda a, b=3 : a + b
print(f4(1))    # 4

# 可变形参 *args和**kwargs
f5 = lambda *args : args
f6 = lambda **kwargs : kwargs
print(f5(1, 2, 3))  # (1, 2, 3)
print(f6(name='小智', age=18))
```



### ③lambda表达式的应用

```python
# 1 带判断的lambda表达式
f1 = lambda a, b : a if a > b else b
print(f1(10, 20))

# 2 列表数据按字典key的值排序
students = [
    {'name': 'Tom', 'age': 20},
    {'name': 'Rose', 'age': 29},
    {'name': 'Jack', 'age':30}
]
# 根据名字首字母进行排序
students.sort(key = lambda x : x['name'])
print(students)

# 名字降序
students.sort(key = lambda x : x['name'], reverse=True)
print(students)

# 按年龄进行正序排序
students.sort(key = lambda x : x['age'])
print(students)
```





## 09	高阶函数

```python
# abs()对数值做绝对值处理
print(abs(-1))  # 1
# round()对数值做四舍五入处理
print(round(1.5))   # 2

# 方法一
def f1(a=0, b=0):
    return abs(a) + abs(b)

print(f1(-1, -2))   # 3

# 方法2 --> 传入一个函数作为参数
def f2(a, b, f):
    return f(a) + f(b)

print(f2(-2, -5, f1))   # 7
```



**内置高阶函数**

```python
import functools

list1 = [1, 2, 3, 4, 5]
def f1(a):
    return a ** 2

# map函数 --> 它会遍历列表中的每一个元素，经过方法处理后又放回去
# map的返回值是Object类型
result = map(f1, list1)
print(list(result))

# reduce函数 -->
# reduce函数的返回值不需要转换，可以直接输出
def f2(a, b):
    return a + b
# 这个就相当于是 result = ((((1+2)+3)+4)+5)
result2 = functools.reduce(f2, list1)
print(result2)

def f3(a):
    return a % 2 == 0

# 过滤掉带入方法中返回false的值
result3 = filter(f3, list1)
print(list(result3))
```

![image-20210926113220573](E:\学习笔记\save-img-upload-img/img/20210926113222.png)





# 五、文件

## 01	文件的基本操作

### ①打开文件

在python，使用open函数，可以打开一个已经存在的文件，或者创建一个新文件，语法如下：

```python
# name 要打开目标文件名的字符串（包含文件所在的具体路径）
# mode 打开文件的模式（访问模式），默认值是r(只读)
open(name, mode)
```



**打开文件的模式**

![image-20210926143211861](E:\学习笔记\图片\20210926143213.png)



### ②读写等操作

1.  **写操作**

    ```python
    # 1 打开文件
    f = open("test.txt", 'w')
    
    # 2 操作文件
    f.write('hello world')  # 写入内容
    
    # 3 释放资源
    f.close()
    ```

2.  **读操作**

    -   readline()：读取一行，第一次调用读取第一行，第二次调用读取第二行

    -   readlines()：读取整个文件，每一行都是一个元素，以列表返回

        ```python
        # 1 打开文件
        f = open("test.txt", 'r')
        
        # 2 操作文件
        # content = f.read(12)    # 读取指定长度的内容
        content = f.readlines()   # 读取整个文件，每一行是一个元素
        # content = f.readline()    # 读取一行
        # content2 = f.readline()    # 第二次读取就是第二行
        # 依次类推
        print(content)
        
        # 3 释放资源
        f.close()
        ```

3.  seek

    ```python
    f = open('test.txt', 'r')
    # 重新指定文件指针的位置，也就是定义从哪里开始读取呗
    f.seek(4, 0)
    content = f.readline()
    print(content)
    ```

    

### ③关闭文件

文件对象.close()关闭

**注意**：一定要关闭资源



## 02	文件的备份

```python
# 输入文件名
old_name = input('请输入你要备份的文件名')
index = old_name.rfind('.')
if index <= 0:
    print('您的输入有误，请重新输入')
else:
    new_name = old_name[:index] + '[备份]' + old_name[index:]
    old_f = open(old_name, 'rb')
    new_f = open(new_name, 'wb')
    while True:
        con = old_f.read(1024)
        if len(con) == 0:
            break
        new_f.write(con)
```



## 03	操作文件

在Python中文件和文件夹的操作要借助os模块里面的相关功能

```python
# 导入os模块
import os

# rename --> 修改文件名字
os.rename('E:\\test\\test.txt', 'E:\\test\\rename.txt')
# 如果被修改的文件目标地址不一致，那么最终它是会被移动到目标位置
# 下面这个最终这个文件会被改名并且会移动到当前目录下
os.rename('E:\\test\\test.txt', 'rename.txt')

# remove --> 删除文件
os.remove('E:\\test\\rename.txt')

# mkdir --> 创建文件夹
os.mkdir('E:\\test2')

# rmdir --> 删除文件夹
os.rmdir('E:\\test2')

# getcwd --> 获取当期目录
print(os.getcwd())  # E:\Work\schcoolWork\basicStudy\05 文件

# chdir --> 改变当前工作目录到指定目录
os.chdir('E:\\test')
print(os.getcwd())      # E:\test

# listdir --> 获取目录列表，默认值是当前目录下
print(os.listdir('E:\\test'))
```



## 04	应用案例

**需求**：批量修改文件名，既可添加指定字符串，又能删除指定字符串。

**步骤**：

1.   设置添加删除字符串的的标识
2.   获取指定目录的所有文件
3.   将原有文件名添加/删除指定字符串，构造新名字
4.   os.rename()重命名

```python
import os

dir_name = 'E:\\test\\'
dir_list = os.listdir(dir_name)
print(f'修改前 {dir_list}')
for old_name in dir_list:
    new_name = 'test' + old_name
    os.rename(dir_name + old_name, dir_name + new_name)
dir_list = os.listdir(dir_name)
print(f'修改后 {dir_list}')
```





# 六、面向对象

## 01	面向对象基础

### ①面向对象是什么

将一系列事物抽象成对象的方式来进行编程就是面向对象编程

**类**：相当于是设计图，里面是对象具体的实现，好比如洗衣机的设计图

**对象**：是根据类来创建出来的实例，通过设计图来制作出洗衣机



### ②对象的创建和使用

和ajva的类似，不过python的是不需要new关键字的

```python
# 定义类 --> 使用class关键字定义
class Dog():
    # self值的是调用该函数的对象，类似java的this
    def call(self):
        print('汪汪汪')

# 创建对象 --> 对象名 = 类名()
dog = Dog()
# 调用方法 --> 对象名.方法名()
dog.call()
```



### ③定义和获取对象属性

属性名要以下划线开头，这是python的规范

```python
# 定义 --> 对象.属性名 = 值
class Dog():
	def __init__(self):
        self._name = ''
    	self._aeg = 0
    def writerTo(self):
        # 类里面通过self来获取属性值
        print(f'名字是{self._name}, 年龄{self._age}')

dog = Dog()
# 初始化类属性
# 类外面获取对象属性 --> 对象名.属性名
dog._name = '小黑'
dog._age = 19
# 调用方法输出我们初始化的属性值
dog.writerTo()
```



### ④魔法方法

在python中，__ xx __()的函数叫做魔法方法，值的是具有特殊功能的函数



#### 1	__ init __()

**说明**：

-   这个函数的功能就给对象初始化，**在python中函数时不能重载的**，所以无参init方法和有参的只能有一个存在
-   python是可以不用定义属性的，直接就是使用__ init __ 方法进行初始化（离谱~~~）
-   **这个类似于java的构造器，有参和无参的构造器**



**代码实现**

```python
class Cat():

    # 无参的
    # def __init__(self):
    #     self.name = '小灰灰'
    #     self.age = 1
    # # 有参构造
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def print_info(self):
        print(f'猫的名字是{self.name} 年龄是{self.age}')

# 使用无参构造器创建对象
# cat = Cat()
# cat.print_info()

# 使用有参构造器创建对象
cat2 = Cat('小黑', 3)
cat2.print_info()
```



#### 2	__ str __()

这个方法类似于java的toStirng方法，在没有定义这个方法之前，直接输出对象是输出的地址值，有了它之后，输出的就是return的数据了

**代码演示**

```python
class Animal():
    def __init__(self, name, age):
        self.name = name
        self.age = age

    def __str__(self):
        return f'动物的名字是{self.name} 年龄是{self.age}'

animal = Animal('海龟', 1000)
print(animal)   # 动物的名字是海龟 年龄是1000
```



#### 3	__ del __()

当删除对象时，python解释器也会默认调用__ del __() 函数

**代码演示**

```python
class Dog():
    def __init__(self):
        self.name = '小蓝'
        self.age = 20
    # 在对象销毁之后会调用这个方法来进行一些处理
    def __del__(self):
        print(f'{self}对象已经被删除了')

dog = Dog()
```



### ⑤应用案例

#### 烤地瓜

**需求主线**：

1.   被烤的时间和对应的地瓜状态：

     0-3分钟：生的

     3-5分钟：半生不熟

     5-8分钟：熟的

     超过8分钟：烤糊了

2.   添加的调料：

     用户可以按自己的意愿添加调料

```python
# 烤地瓜
class SweetPotato():
    def __init__(self):
        # 被烤的时间
        self.cook_time = 0
        # 地瓜的状态
        self.cook_static = '生的'
        # 调料列表
        self.condiments = []

    def cook(self, time):
        """
        烤地瓜的方法
        :param time: 烤的时间
        description:    0-3分钟：生的
                        3-5分钟：半生不熟
                        5-8分钟：熟的
                        超过8分钟：烤糊了
        """
        self.cook_time += time
        if 0 <= self.cook_time < 3:
            self.cook_static = '生的'
        elif 3 <= self.cook_time < 5:
            self.cook_static = '半生不熟'
        elif 5 <= self.cook_time < 8:
            self.cook_static = '熟的'
        elif self.cook_time >= 8:
            self.cook_static = '烤糊了'

    ''' 输出信息 '''
    def __str__(self):
        return f'这个地瓜烤了{self.cook_time}分钟, 状态是{self.cook_static}, ' \
               f'添加的调 料有{self.condiments}'

    def add_condiments(self, condiments):
        """
        添加调料方法
        """
        self.condiments.append(condiments)

# 创建对象
sweetPotato = SweetPotato()
sweetPotato.cook_time = 5
sweetPotato.add_condiments(['酱油', '香蕉', '八角'])
print(sweetPotato)
```



#### 搬家具

将小于房子剩余面积的家具摆放到房子中

```python
"""
    @description    # 搬家具  --> 将小于房子剩余面积的家具摆放到房子中
    @author 小智
    @create 2021-09-2021/9/27 9:05
"""
# 家具类
class Furniture():
    def __init__(self, name, area):
        # 家具名字
        self.name = name
        # 家具占地面积
        self.area = area

# 房子类
class Home():
    def __init__(self, address, area):
        # 地理位置
        self.address = address
        # 房屋面积
        self.area = area
        # 剩余面积
        self.free_area = area
        # 家具列表
        self.furniture = []

    def __str__(self):
        return f'房子坐落于{self.address}, ' \
               f'占地面积{self.area},' \
               f' 剩余面积 {self.free_area}, ' \
               f'家具有{self.furniture}'

    def add_furniture(self, item):
        """容纳家具"""
        if self.free_area >= item.area:
            self.furniture.append(item.name)
            # 家具搬入后，房屋剩余面积 = 之前剩余面积 - 家具面积
            self.free_area -= item.area
        else:
            print('家具太大，剩余内存不足，无法容纳')

# 床架对象
bed = Furniture('双人床', 6)
jia1 = Home('北京', 1000)
jia1.add_furniture(bed)
print(jia1)

sofa = Furniture('沙发', 10) 
jia1.add_furniture(sofa) 
print(jia1) 

ball = Furniture('篮球场', 1500) 
jia1.add_furniture(ball) 
print(jia1)
```



## 02	继承

### ①继承的概念

python中是支持多继承的，也就是可以继承多个类**（好家伙，几个爸，离谱！！！）**

子类继承父类的所有属性和方法

==在Python中，所有类默认继承object类，object类是顶级类或基类；其他子类叫做派生类==

**语法**：子类(父类类名1，类名2，...，类名n)



**拓展**：

1.  **经典类或旧式类**

    不由任意内置类型派生出的类，称之为经典类。

    ```python
    class 类名: 
    	代码
    	......
    ```

2.  **新式类**

    ```python
    class 类名(object): 
    	代码
    ```



**代码演示**

```python
# 父类
class SuperClass():
    def __init__(self):
        self.name = '小智'
        self.age = 20

    def print_info(self):
        print(f'名字是{self.name} 年龄是{self.age}')
# 子类
class SonClass(SuperClass):
    pass

son = SonClass()
son.print_info()
```



### ②单继承、多继承和多重继承

**说明**：

-   单继承就是只有一个父类
-   多继承就是有多个父类
-   多重继承就是子类继承父类，父类继承爷类，也类继承太爷类，后面依次类推



**代码演示**

**多继承**

```python
# 单继承 --> 继承一个类
class A():
    def print_A(self):
        print('我是A中的方法')

class B():
    def print_B(self):
        print('我是B中的方法')

# 多继承 --> 继承多个类
class Son(A, B):
    pass

son = Son()
# 调用继承的两个类的方法
son.print_A()
son.print_B()
```



**多层继承**

```python
# 多层继承 --> 继承多个类 --> 也就相当于是多继承了
class A():
    def print_A(self):
        print('我是A中的方法')

class B(A):
    def print_B(self):
        print('我是B中的方法')

class Son(B):
    pass

son = Son()
son.print_A()
son.print_B()
```



### ③子类重写父类同名方法和属性

```python
# 子类调用父类的同名方法和属性
class A():
    count = 0
    def __init__(self):
        self.name = '小黑'

    def print_info(self):
        print(f'A中的值为{self.name}')

class B():
    def __init__(self):
        self.name = '小鬼'
    def print_info(self):
        print(f'B中的值为{self.name}')

class C(A, B):
    def __init__(self):
        self.name = '小绿'

    def print_info(self):
        self.__init__()
        print(f'C中的值为{self.name}')

    def print_A_info(self):
        # 初始化值
        A.__init__(self)
        A.print_info(self)

    def print_B_info(self):
        B.__init__(self)
        B.print_info(self)

c = C()
c.print_info()
c.print_A_info()
c.print_B_info()
```



### ④super()调用父类方法

super()可以来区分同名的方法和属性，也可以直接调用父类的方法和属性

```python
# super()的使用
class A():
    count = 0
    def __init__(self):
        self.name = '小黑'

    def print_info(self):
        print(f'A中的值为{self.name}')

class B():
    def __init__(self):
        self.name = '小鬼'
    def print_info(self):
        print(f'B中的值为{self.name}')

class C(A):
    def __init__(self):
        self.name = '小孔狗'
    def print_info(self):
        print(f'C中的值为{self.name}')
        super().__init__()
        # 调用父类的方法
        # 方式一 当前类名改变了，还要手动修改类名，费劲，所以不推荐
        # A.print_info()
        # 方式二
        # super(B, self).print_info()
        # 方式三
        super().print_info()


class D(A, B):
    def __init__(self):
        self.name = 'dddd'

    def print_info(self):
        print(f'D中的值为{self.name}')
        # 多个继承默认调用第一个类中的同名方法
        super().__init__()  # 使用父类中的同名属性
        super().print_info()

# 单继承的
c = C()
c.print_info()      # B中的值为小鬼 \n A中的值为小鬼

# 多继承的
d = D()
d.print_info()      # D中的值为dddd \n A中的值为小黑
```





## 03	私有属性

python中只有两种访问权限，公有和私有

在实例属性和方法之前加两个下划线就表示是私有的，例如：self.__name 这个就是私有属性

子类调用父类的私有方法和属性，通过父类公共的set和get方法来获取和修改对应的属性值

**两种get和set方法的写法**

1.  定义set和get方法设置和返回值
2.  使用装饰器的写法



**代码演示**

```python
# 私有属性
class Me():
    def __init__(self):
        # 私房钱
        self.__privateMoney = 1000
    # 使用私房钱的方法
    def __usePrivateMoney(self):
        print(f'me拿出了{self.__privateMoney}私房钱')

    # 定义get和set方法
    def set_privateMoney(self, privateMoney):
        self.__privateMoney = privateMoney

    def get_privateMoney(self):
        return self.__privateMoney

    # 第二种写法 --> 使用装饰器
    @property
    def privateMoney(self):
        return self.__privateMoney
    @privateMoney.setter
    def privateMoney(self, privateMoney):
        self.__privateMoney = privateMoney

class girlFriend(Me):
    pass

g = girlFriend()
# 调用me的私有属性
# print(g.__privateMoney)   # 会报错
# g.__usePrivateMoney()     # 报错
# 修改me的属性值
g.__privateMoney = 2    # 是可以的，但是它是重新生成了一个属性并赋值，不是修改me中的属性值
print(g.__privateMoney) # 所以这个时候调用属性就不会报错


# 通过get和set方法获取和修改值
g.set_privateMoney(2000)
print(g.get_privateMoney())

# 第二种写法的调用
g.privateMoney = 3000
print(g.privateMoney)
```





## 04	多态

**说明**：

-   多态就是一类事物的多种形态，比如动物可以是狗、猫、人
-   多态的前提就是继承，没有继承就没有多态
-   **好处**：灵活多变，传递的对象不同，实现的效果可能也是不同的

**代码实现**

```python
# 动物类
class Animal():
    def call(self):
        print('.....')

class Dog(Animal):
    def call(self):
        print('汪汪汪')

class Cat(Animal):
    def call(self):
        print('喵喵喵')

class Person(object):
    def make_animal(self, animal):
        animal.call()

person = Person()
animal = Animal()
dog = Dog()
cat = Cat()
person.make_animal(animal)
person.make_animal(dog)
person.make_animal(cat)
```



## 05	类属性和实例属性

**类属性**：定义在方法内部，init函数外部的变量就是类属性

**实例属性**：定义在实例init函数内的属性就是实例属性

**注意**：

-   **实例对象和类**可以修改和访问**实例属性**，但是**类属性只能被类访问**，也就是说实例对象是不能访问类属性的
-   每创建对象一次，实例属性都会被初始化一次，同时开辟一块内存
-   类属性从对象创建的时候就只占一块内存，不管你后面创建了几次对象，它是共用的



**代码演示**

```python
class A():
    count = 1
    def __init__(self):
        self.num = 100

# 只能用类来访问类属性
A.count += 1
a = A()
'''
    使用实例对象访问类属性
        它并不会对类属性进行操作，
        而是生成一个和类属性名一样的实例属性，
        所以最终输出的是实例属性
'''
a.count += 1
a2 = A()
a2.count += 1
print(a.count)
print(a2.count)

# 测试实例属性
a3 = A()
a4 = A()    # 每次创建都会初始化
a3.num += 1
a4.num += 1
print(a3.num)
print(a4.num)
# print(A.num)    # 报错
```



## 06	类方法和实例方法

### 类方法

-   使用装饰器**@classmethod**来标识类方法，**第一个参数必须是对象**，一般**cls**作为第一个参数

**代码演示**

```python
class Test(object):
    __count = 1

    @classmethod
    def getCount(cls):
        return cls.__count

test = Test()
print(test.getCount())
```



### 静态方法

**说明**：

-   需要通过装饰器 @staticmethod 来进行修饰，**静态方法既不需要传递类对象也不需要传递实例对象（形参没有self/cls)。**
-   静态方法 也能够通过 **实例对象** 和 **类对象** 去访问。

**调用**：类名.方法名()

**好处**：不需要传参，不用创建对象，减少了内存的消耗

==**注意**：不需要传参不代表不能传参，静态方法也是可以传入参数的==



**代码演示**

```python
0class Test():
    @staticmethod
    def print_info():
        print('静态方法的调用')

    # 传入参数的静态方法
    @staticmethod
    def print_info2(name):
        print(name)
        
Test.print_info()
# 实例的方式调用也可以
test = Test()
test.print_info()
Test.print_info2('小智')
```



# 七、异常处理

## 1	异常使用

**语法**：

```python
try:
    可能出现异常的代码
except 异常类(也可以不填，也可以指定要捕获的异常，可以是多个，多个要用放入元组中):
    捕获异常之后要执行的代码
else:
    没有异常才执行的代码
finally:
    无论如何都会执行的代码
```



### ①try-except

```python
try:
    open('test.txt','r')
except:
    print('有异常')
```



### ②捕获指定异常

也就是说，如果报的异常不是指定的异常类，那么就捕获不了

```python
# 一个指定捕获的异常
try:
    open('test.txt', 'r')
except FileNotFoundError:   # 指定的异常
    print('有异常')
 
# 多个指定捕获的异常
try:
    open('test.txt', 'r')
    # 多个异常要放入到元组中
except (FileNotFoundError, NameError):   # 指定的异常
    print('有异常')
```



### ③捕获异常信息

```python
try:
    open('test.txt', 'r')
    # 通过as关键字，将异常信息给error
except FileNotFoundError as error:
    print(error)
```



### ④捕获所有异常

Exception是所以异常类的父类，所以，它是可以捕获所有异常的

```python
try:
    open('test.txt', 'r')
except Exception:
    print('有异常')
```



### ⑤else和finally

```python
try:
    f = open('test2.txt', 'r')
except Exception:
    f = open('test2.txt', 'w')
    print('有异常')
else:
    # 没有异常就会执行else中的代码,有异常程序直接结束
    print('没有异常')
finally:
    f.close()
```



### ⑥异常的传递

在程序运行的过程中，如果用户打断了程序执行，也是会抛异常的

```python
def b():
    # 使用ctrl + c结束程序
    return int(input("请输入一个整数："))

def a():
    return b()

try:
    print(a())
except ValueError:
    print("请输入正确的整数")
except Exception as result:
    print("未知错误 %s" % result)
```



## 2	自定义异常

自己定义一个自己想要的异常类，自定义异常要继承Exception类

通过**raise**关键字来**抛出异常**，类似于java的throw关键字，功能都是将异常往上抛出异常

```python
# 自定义异常
class MyError(Exception):           
    def __init__(self, length, min_len):
        self.length = length
        self.min_len = min_len

    # 抛出的异常信息
    def __str__(self):
        return f'你输入的长度是{self.length}, 不能少于{self.min_len}个字符'

def main():
    try:
        con = input('请输入密码')
        if len(con) < 3:
            raise MyError(len(con), 3)
    except Exception as result:
        print(result)
    else:
        print('密码输入完成')

main()
```





# 八、模块

Python 模块(Module)，是一个 Python 文件，以 .py 结尾，包含了 Python 对象定义和Python语句。

模块能定义函数，类和变量，模块里也能包含可执行的代码。



## 1	导入模块

### ①import 

**语法**：

1.  导入模块 --> import 模块名1 模块名2 ...
2.  调用功能 --> 模块名.功能名

**代码演示**

```python
# 1 导入模块
import math
# 2 调用模块中的方法
print(math.sqrt(9))
```



### ②from...import ...

**代码演示**

```python
# 2、from...import...方式导入
# 语法：from 模块名 import 功能1 功能2 功能3 ...
from math import sqrt
print(sqrt(9))
```



### ③from...import *

```python
# 3、from...import * 和 import 模块名是一样的功能
# 语法：from 模块名 import *
from math import *
print(sqrt(4))
```



## 2	制作模块

在Python中，每个Python文件都可以作为一个模块，模块的名字就是文件的名字。**自定义模块名必须要符合标识符命名规则。**



### ①定义和测试模块

-   新创建一个python文件，然后定义一个方法
-   在开发中，当创建好一个模块的时候，需要测试它是否能够正常运行
-   测试的调用要放到是当前文件调用的判断语句内

```python
def test(a, b):
    print(a + b)

# 测试模块
# 使用下面方式测试，当其他文件调用的时候就会执行，我们需要它不执行，调用的时候才执行
# test(1, 3)

# 解决方法
# 放入到main方法中，只有是当前文件调用才能够执行
if __name__ == '__main__':
    test(1, 3)
```



### ②调用模块

和之前导入的一样，通过import导入

```python
from makeModular import test
test(1, 3)      # 4
```



### ③as定义别名

有两个同名的函数名，这个时候我们就要进行区分到底导入的是哪一个模块中的功能了，所以可以用as来定义别名，这样我们就可以知道调用的是谁的函数了

**代码演示**

```python
# 重新创建一个文件定义了一个函数
def test(a, b):
    print(a + b)

if __name__ == '__main__':
    test(1, 3)
    
# 在测试文件中测试
from makeModular import test as t1
from makeModular2 import test as t2

t1(1, 3)    # 4
t2(3, 5)    # 8
```

==**注意**：如果导入不同模块中使用同名的函数的话，它使用的是最后面导入的那个函数，所以要用as来区分一下==





## 3	模块定位顺序

当导入一个模块，Python解析器对模块位置的搜索顺序是：

1.   当前目录

2.   如果不在当前目录，Python则搜索在shell变量PYTHONPATH下的每个目录。

3.   如果都找不到，Python会察看默认路径。UNIX下，默认路径一般为/usr/local/lib/python/模块搜索路径存储在system模块的sys.path变量中。变量里包含当前目录，PYTHONPATH和由安装过程决定的默认目录。

**注意**：

-   自己的文件名不要和已有模块名重复，否则导致模块功能无法使用
-   使用from 模块名 import 功能 的时候，如果功能名字重复，调用到的是最后定义或导入的功能。





## 4	__ all __

**__ all __ 列表** 指定导入模块的时候只能导入那些函数，就是说没有在__ all __ 列表中的功能你就不能调用

**代码演示**

```python
__all__ = ['test']

def test(a, b):
    print(a + b)

def test2(a, b):
    print(a, b)
```

**调用包中的函数**

```python
from makeModular2 import *
test(1, 2)
# test2(1, 3)     # 引用不在__all__列表中的函数会报错
```





## 5	包

### ①制作包

**1、创建一个包**

![image-20210929234633634](E:\学习笔记\save-img-upload-img/img/20210929234635.png)

**注意**：新建包后，包内部会自动创建 __ init __ .py 文件，这个文件定义包的导入行为。也就是这个包能做什么是由__ init__.py文件来定义的

![image-20210929234710801](E:\学习笔记\save-img-upload-img/img/20210929234712.png)

**2、在包内创建两个模块来进行测试**

```python
# module1.py
print(1)

def print_info():
    print('module1')
    
# module2.py
print(2)

def print_info():s
    print('module2')
```



### ②导入包

-   **方式一**：导入包中指定的模块
-   **方式二**：导入包下所有的模块

**代码实现**

```python
# 方法一 --> import 包名.模块名
import test.module
# 包名.模块名.目标
test.module.print_info()

# 方法二 --> 导入包名的所有模块
from test import *
module.print_info()
module2.print_info()
```

==**注意**：使用导入包所有模块的方式一定要在 __ init__ 文件中添加 __ all __[]，用来控制允许导入的模块列表==





# 九、多任务编程

## 1	多任务和进程的介绍

### ①多任务的概述

我们现在的电脑几乎都是多核的，所以可以同一时间来执行多个任务，比如运行多个软件



### ②多任务的执行方式

**并发**

-   单核cpu在一段时间内交替的去执行任务，由于cpu的执行速度太快，所以我们可以看成是同时执行的



**并行**

-   多个cpu在一段时间内同时去执行任务，就是说有多个软件同时执行



### ③进程

**进程的概念**

一个正在运行的程序或者软件就是一个进程，**它是操作系统进行资源分配的基本单位**，也就是说每启动一个进程，操作系统都会给其分配一定的运行资源(内存资源)保证进程的运行。

**注意**：

**一个程序运行后至少有一个进程，一个进程默认有一个线程**，进程里面可以创建多个线程，**线程是依附在进程里面的，没有进程就没有线程**。



**进程的作用**

-   单进程

    单进程就是只有一个进程去执行任务，效率不高

-   多进程

    多个进程去执行一个任务，分工合作，效率高





## 2	多进程的使用

### ①导入进程包

```python
import multiprocessing
```



### ②Process进程类的说明

**Process([group [, target [, name [, args [, kwargs]]]]])**

-   group：指定进程组，目前只能使用None
-   target：执行的目标任务名
-   name：进程名字
-   args：以元组方式给执行任务传参
-   kwargs：以字典方式给执行任务传参



**Process创建的实例对象的常用方法**

-   start()：启动子进程实例（创建子进程）
-   join()：等待子进程执行结束
-   terminate()：不管任务是否完成，立即终止子进程



**Process创建的实例对象的常用属性**

​	name：当前进程的别名，默认为Process-N，N为从1开始递增的整数



==**注意**：Process的'P'是大写的，小写的不是！！！==



### ③Process进程类使用

```python
# 导入对任务包
import multiprocessing
import time

# 跳舞任务
def dance():
    for i in range(5):
        print('跳舞中')
        time.sleep(0.2)

# 唱歌任务
def sing():
    for i in range(5):
        print('唱歌中')
        time.sleep(0.2)

if __name__ == '__main__':
    '''
        group：表示进程组，目前只能使用None
        target：表示要执行的任务（函数名）
        name：进程名称，默认是process-1
    '''
    dance_process = multiprocessing.Process(target=dance, name='myProcess1')
    sing_process = multiprocessing.Process(target=sing, name='myProcess2')

    # 启动子进程执行对应的任务
    dance_process.start()
    sing_process.start()
```

![image-20211004165347764](E:\学习笔记\save-img-upload-img/img/20211004165349.png)





### ④获取进程号

-    **multiprocessing.current_process()**     获取当前线程
-   **os.getpid()**      表示获取当前进程编号
-   **os.getppid()**    表示获取当前父线程编号



**代码演示**

```python
# 导入对任务包
import multiprocessing
import os
import time

# 跳舞任务
def dance():
    # 获取当前进程的编号
    print('dance:', os.getpid())
    # 获取当前线程
    print('dance:', multiprocessing.current_process())
    # 获取当前线程的父线程编号
    print('dance的父线程编号:', os.getppid())
    for i in range(5):
        print('跳舞中...')
        time.sleep(0.2)

# 唱歌任务
def sing():
    # 获取当前进程的编号
    print('sing:', os.getpid())
    # 获取当前线程
    print('sing:', multiprocessing.current_process())
    # 获取当前线程的父线程编号
    print('dance的父线程编号:', os.getppid())
    for i in range(5):
        time.sleep(0.2)
        print('唱歌中...')

if __name__ == '__main__':
    # 获取当前进程的编号
    print('main:', os.getpid())
    # 获取当前线程
    print('main:', multiprocessing.current_process())
    '''
        group：表示进程组，目前只能使用None
        target：表示要执行的任务（函数名）
        name：进程名称，默认是process-1
    '''
    dance_process = multiprocessing.Process(target=dance, name='myProcess1')
    sing_process = multiprocessing.Process(target=sing, name='myProcess2')

    # 启动子进程执行对应的任务
    dance_process.start()
    sing_process.start()
```



### ⑤进程带执行有参数的任务

-   **args**：以元组的方式给任务传入参数
-   **kwargs**：以字典的方式给任务传入参数

**代码实现**

```python
import multiprocessing
import time

def task(count):
    print(count)
    for i in range(count):
        print('任务执行中...')
        time.sleep(0.2)
    else:
        print('任务执行完成')

if __name__ == '__main__':
    # args：以元组的方式给任务传入参数
    sub_process = multiprocessing.Process(target=task(5), args=(5, ))
    sub_process.start()

    print('----------------------------------------')

    # kwargs：以字典的方式给任务传入参数
    sub_process2 = multiprocessing.Process(target=task, kwargs={'count': 3})
    sub_process2.start()
```



## 3	进程的注意点

### 3.1 进程之间不共享全局变量

```python
import multiprocessing
import time

# 定义全局变量
g_list = list()

# 添加数据的函数
def add_data():
    for i in range(5):
        g_list.append(i)
        print("add:", i)
        time.sleep(0.2)

    print("add_data:", g_list)

# 读取数据
def read_data():
    print("read_data:", g_list)

# 测试
if __name__ == '__main__':
    # 创建进程
    add_data_process = multiprocessing.Process(target=add_data)
    read_data_process = multiprocessing.Process(target=read_data)

    # 启动进程
    add_data_process.start()
    # 主进程等待添加数据的子线程完成以后继续往下执行，读取数据
    add_data_process.join()
    read_data_process.start()

    print("main:", g_list)
```

![image-20211008213507248](E:\学习笔记\save-img-upload-img/img/20211008213509.png)

**解释图**

![image-20211008213548644](E:\学习笔记\save-img-upload-img/img/20211008213549.png)





### 3.2 主进程会等待所有的子进程执行结束再结束

假如我们现在创建一个子进程，这个子进程执行完大概需要2秒钟，现在让主进程执行0.5秒钟就退出程序，查看一下执行结果，示例代码如下: 

```python
import multiprocessing
import time

def task():
    for i in range(10):
        print("任务执行中...")
        time.sleep(0.2)

if __name__ == '__main__':
    # 创建子进程
    sub_process = multiprocessing.Process(target=task)
    sub_process.start()

    # 主进程延时0.5秒
    time.sleep(0.5)
    print("程序over")
    exit()
```

**执行结果**

![image-20211009151726921](E:\学习笔记\save-img-upload-img/img/20211009151728.png)

**说明**：主进程会等待子进程结束



### 3.3	主进程不等待子进程结束

-   设置守护进程

    ​	当设置了守护进程，那么当主进程结束的时候，子线程也会跟着结束，主线程不会等待子线程结束再结束

-   子进程自我销毁

    ​	子线程销毁了，也就意味着子线程结束了，那么主线程就不会等待子线程结束

**代码演示**

```python
import multiprocessing
import time

def task():
    for i in range(10):
        print("任务执行中...")
        time.sleep(0.2)

if __name__ == '__main__':
    # 创建子进程
    sub_process = multiprocessing.Process(target=task)
    # 第一种方式 --> 设置守护进程
    # sub_process.daemon = True
    sub_process.start()

    # 主进程延时0.5秒
    time.sleep(0.5)
    print("程序over")
    # 第二种方式 --> 子线程自我销毁
    sub_process.terminate()
    exit()
```



## 4	线程的概念

一个进程中会有多个线程协同工作，线程之间相互协作完成任务



**线程的作用**

![image-20211009163233464](E:\学习笔记\save-img-upload-img/img/20211009163234.png)

**说明**：程序启动默认会开启一个主程序，程序员自己创建的线程就是子线程，多线程可以完成多任务



## 5	多线程的使用

### ①导入线程模块

```python
#导入线程模块 
import threading
```



### ②线程类Thread的参数说明

Thread([group [, target [, name [, args [, kwargs]]]]])

-   group: 线程组，目前只能使用None
-   target: 执行的目标任务名
-   args: 以元组的方式给执行任务传参
-   kwargs: 以字典方式给执行任务传参
-   name: 线程名，一般不用设置



### ③应用案例

```python
# 导入对任务包
import threading
import time

# 跳舞任务
def dance():
    for i in range(5):
        print('跳舞中...')
        time.sleep(0.2)

# 唱歌任务
def sing():
    for i in range(5):
        print('唱歌中...')
        time.sleep(0.2)

if __name__ == '__main__':
    '''
        group：表示进程组，目前只能使用None
        target：表示要执行的任务（函数名）
        name：进程名称，默认是process-1
    '''
    dance_process = threading.Thread(target=dance)
    sing_process = threading.Thread(target=sing)

    # 启动子进程执行对应的任务
    dance_process.start()
    sing_process.start()
```



==**说明**：带参数任务的实现方式和多进程的实现方式一样==



## 6	线程的注意点

### ①线程之间执行是无序的

它是由cpu调度的，随机的，所以线程是没有秩序的



### ②主线程会等待所有的子线程执行结束再结束

这个跟多进程是一样的，主进程会等待所有子线程结束了再结束

**让主进程不等待就结束**  -->  线程是没有销毁方法的，所以它只有一种方式 --> 设置守护线程

**守护线程的两种方式**：

-   threading.Thread(daemon=True)
-   线程对象.setDaemon(True)



**代码演示**

```python
import threading
import time

def test():
    for i in range(5):
        print("test:", i)
        time.sleep(0.5)

if __name__ == '__main__':
    # 设置守护线程方式一
    # sub_thread = threading.Thread(target=test, daemon=True)
    # 方式二 --> 线程对象.setDaemon
    sub_thread = threading.Thread(target=test)
    sub_thread.setDaemon(True)
    sub_thread.start()

    time.sleep(1)
    print("over")
```



### ③线程之间共享全局变量

这里会出现一个问题，多个线程共享一个变量时会导致数据错误的问题，你抢我又抢



### ④线程之间共享全局变量数据出现错误问题

**两种解决方式**

1.  **线程等待（join）**

    ​	一个线程执行完之后在执行下一个线程

    

    **代码实现**

    ```python
    import threading
    
    count = 0
    
    def sum_1():
        global count
        for i in range(1000000):
            count += 1
        print("sum_1:", count)
    
    def sum_2():
        global count
        for i in range(1000000):
            count += 1
        print("sum_2:", count)
    
    if __name__ == '__main__':
        sum_1_process = threading.Thread(target=sum_1)
        sum_2_process = threading.Thread(target=sum_2)
    
        sum_1_process.start()
        # 解决数据错误方式一 --> 调用等待方法
        sum_1_process.join()
        sum_2_process.start()
    ```

    

2.  **互斥锁**

    ​	一个线程再执行的时候给数据上锁，其他的线程就不能访问了，当线程执行完之后进行解锁，然后给其他线程来使	用，这样就能确保数据的一致性

    

    **互斥锁操作**

    1.  创建锁对象 --> threading.Lock()
    2.  上锁 --> 锁对象.acquire() 
    3.  解锁 --> 锁对象.release

    

    **代码实现**

    ```python
    import threading
    
    count = 0
    # 定义全局互斥锁
    lock = threading.Lock()
    
    def sum_1():
        # 上锁
        lock.acquire()
        global count
        for i in range(1000000):
            count += 1
        print("sum_1:", count)
        # 解锁
        lock.release()
    
    def sum_2():
        # 上锁
        lock.acquire()
        global count
        for i in range(1000000):
            count += 1
        print("sum_2:", count)
        # 解锁
        lock.release()
    
    if __name__ == '__main__':
        sum_1_process = threading.Thread(target=sum_1)
        sum_2_process = threading.Thread(target=sum_2)
        
        # 方式二 --> 互斥锁
        sum_1_process.start()
        sum_2_process.start()
    ```





## 7	死锁

**死锁**: 一直等待对方释放锁的情景就是死锁

**死锁的结果**

-   会造成应用程序的停止响应，不能再处理其它任务了。

**代码演示**

```python
import threading

# 创建互斥锁
import time

lock = threading.Lock()

def get_value(index):
    # 上锁
    lock.acquire()
    print(threading.current_thread())
    my_list = [3, 34, 45, 32]
    # 判断下角标是否越界
    if index >= len(my_list):
        print("下角标越界:", index)
        # 如果不释放锁就会造成堵塞，后面的线程不能执行
        # 所以要在合适的地方释放锁
        lock.release()
        return
    value = my_list[index]
    print(value)
    time.sleep(0.2)
    # 释放锁
    lock.release()

if __name__ == '__main__':
        # 模拟取值操作
    for i in range(30):
        sub_thread = threading.Thread(target=get_value, args=(i, ))
        sub_thread.start()
```

**没上锁的结果**

![image-20211009210237844](E:\学习笔记\save-img-upload-img/img/20211009210239.png)



## 8	进程和线程对比

### ①关系对比

1.   线程是依附在进程里面的，没有进程就没有线程。
2.   一个进程默认提供一条线程，进程可以创建多个线程。



### ②区别对比

1.   进程之间不共享全局变量
2.   线程之间共享全局变量，但是要注意资源竞争的问题，解决办法: 互斥锁或者线程同步
3.   创建进程的资源开销要比创建线程的资源开销要大
4.   进程是操作系统资源分配的基本单位，线程是CPU调度的基本单位
5.   线程不能够独立执行，必须依存在进程中
6.   多进程开发比单进程多线程开发稳定性要强



### ③优缺点对比

**进程优缺点**:

-   优点：可以用多核
-   缺点：资源开销大

**线程优缺点**:

-   优点：资源开销小
-   缺点：不能使用多核





# 十、网络编程

## 1	socket概述

socket (简称 套接字) 是**进程之间通信一个工具**，好比现实生活中的**插座**，所有的家用电器要想工作都是基于插座进行，**进程之间想要进行网络通信需要基于这个** **socket**。



## 2	TCP客户端开发

### ①开发步骤

1.   创建客户端套接字对象
2.   和服务端套接字建立连接
3.   发送数据
4.   接收数据
5.   关闭客户端套接字



### ② socket 类的介绍

```python
# 导入socket模块
import socket
```

创建客户端 socket 对象 **socket.socket(AddressFamily, Type)**

**参数说明**：

-   AddressFamily 表示IP地址类型, 分为TPv4和IPv6
-   Type 表示传输协议类型

**方法说明**：

-   connect((host, port)) 表示和服务端套接字建立连接, host是服务器ip地址，port是应用程序的端口号
-   send(data) 表示发送数据，data是二进制数据
-   recv(buffersize) 表示接收数据, buffersize是每次接收数据的长度

**编码和解码**

1.   str.encode(编码格式) 表示把字符串编码成为二进制
2.   data.decode(编码格式) 表示把二进制解码成为字符串



### ③应用实例

```python
import socket

if __name__ == '__main__':
    # 1 创建tcp客户端套接字对象
    tcp_client_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # 2 建立连接
    tcp_client_socket.connect(("172.18.83.226", 8080))
    # 3 要发送的数据
    send_data = "蔡玉敏是狗".encode("gbk")
    # 4 发送数据
    tcp_client_socket.send(send_data)
    # 5 接收服务端发送来的数据
    # recv中的参数是设置最大接收值
    recv_data = tcp_client_socket.recv(1024)
    print(recv_data)
    # 对数据进行解码
    recv_content = recv_data.decode("gbk")
    print("接收客户端的数据为:", recv_content)
    # 6 关闭连接
    tcp_client_socket.close()
```



## 3	TCP服务端开发

### ①开发流程

1.   创建服务端端套接字对象
2.   绑定端口号
3.   设置监听
4.   等待接受客户端的连接请求
5.    接收数据
6.   发送数据
7.   关闭套接字



### ②方法介绍

服务端还是使用socker对象

**socket.socket(AddressFamily, Type)**

**参数说明****:**

-   AddressFamily    表示IP地址类型, 分为TPv4和IPv6
-   Type                    表示传输协议类型

**方法说明****:**

-   bind((host, port)) 表示绑定端口号, host 是 ip 地址，port 是端口号，ip 地址一般不指定，表示本机的任何一个ip地址都可以。
-   **listen (backlog)**    表示设置监听，backlog参数表示最大等待建立连接的个数。
-   **accept()**                表示等待接受客户端的连接请求
-   send(data)             表示发送数据，data 是二进制数据
-   recv(buffersize)     表示接收数据, buffersize 是每次接收数据的长度



### ③应用实例

```python
import socket

if __name__ == '__main__':
    # 1 创建服务端端套接字对象
    tcp_server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # 设置端口号复用，让程序退出端口号立即释放
    tcp_server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, True)
    # 2 绑定端口号
    # 参数一 --> 指定可以访问的ip，如果为空，那么就是所有ip都可以访问
    tcp_server_socket.bind(("", 8080))
    # 3 设置监听
    # listen中的参数就是最大的连接数
    tcp_server_socket.listen(128)
    # 4 等待接受客户端的连接请求
    # 2.专门和客户端通信的套接字： service_client_socket
    # 2.客户端的ip地址和端口号： ip_port
    tcp_client_socket, ip_port = tcp_server_socket.accept()
    print("客户端的ip为", ip_port)
    # 5 接收数据
    recv_data = tcp_client_socket.recv(1024)
    # 获取数据的长度
    recv_data_len = len(recv_data)
    print(recv_data_len)
    # 对数据进行解码
    recv_content = recv_data.decode("gbk")
    print("接收客户端的数据为:", recv_content)
    # 6 发送数据
    send_data = "ok,正在解决问题...".encode("gbk")
    # 发送数据给客户端
    tcp_client_socket.send(send_data)
    # 7 关闭套接字
    tcp_server_socket.close()
    tcp_client_socket.close()
```



## 4	TCP网络应用程序注意点

1.   当 TCP 客户端程序想要和 TCP 服务端程序进行通信的时候必须要先**建立连接**
2.   TCP 客户端程序一般不需要绑定端口号，因为客户端是主动发起建立连接的。
3.   **TCP** **服务端程序必须绑定端口号**，否则客户端找不到这个 TCP 服务端程序。
4.   listen 后的套接字是被动套接字，**只负责接收新的客户端的连接请求，不能收发消息。**
5.    当 TCP 客户端程序和 TCP 服务端程序连接成功后， TCP 服务器端程序会产生一个**新的套接字**，收发客户端消息使用该套接字。
6.   **关闭** **accept** **返回的套接字意味着和这个客户端已经通信完毕**。 
7.   **关闭** **listen** **后的套接字意味着服务端的套接字关闭了，会导致新的客户端不能连接服务端，但是之前已经接成功的客户端还能正常通信。**
8.   *当客户端的套接字调用** **close** **后，服务器端的** **recv** **会解阻塞，返回的数据长度为****0**，服务端可以通过返回数据的长度来判断客户端是否已经下线，反之**服务端关闭套接字，客户端的** **recv** **也会解阻塞，返回的数据长度也为0**



## 5	多任务版TCP服务端程序开发

### ①具体实现步骤

1.   编写一个TCP服务端程序，循环等待接受客户端的连接请求
2.   当客户端和服务端建立连接成功，创建子线程，使用子线程专门处理客户端的请求，防止主线程阻塞
3.   把创建的子线程设置成为守护主线程，防止主线程无法退出。



### ②代码实现

```python
import socket
import threading


def handle_client_request(service_client_socket, ip_port):
    while True:
        # 接收客户端的数据
        recv_data = service_client_socket.recv(1024)
        # 接收到的消息是二进制的
        if recv_data:
            print("recv_data=", recv_data.decode("gbk"),"ip_port=" , ip_port)
            service_client_socket.send("接收了消息，正在处理中...".encode("gbk"))
        else:
            print("客户端下线了...", ip_port)
            break

    # 关闭套接字对象
    service_client_socket.close()


if __name__ == '__main__':
    # 1 创建套接字对象
    tcp_server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    # 设置端口号复用，让程序退出端口号立即释放
    tcp_server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, True)
    # 2 绑定端口号
    tcp_server_socket.bind(("", 8080))
    # 3 设置监听
    tcp_server_socket.listen(128)
    # 4 循环等待客户端请求
    while True:
        # 等待客户端任务请求
        service_client_socket, ip_port = tcp_server_socket.accept()
        print("客户端连接成功:", ip_port)

        # 接收成功开启一个子线程，不同线程负责接收不同客户端的数据
        sub_thread = threading.Thread(target=handle_client_request,
                                      args=(service_client_socket, ip_port))
        # 设置守护线程
        sub_thread.setDaemon(True)
        # 启动子线程
        sub_thread.start()
```

![image-20211011092916079](E:\学习笔记\save-img-upload-img/img/20211011092918.png)

![image-20211011092932360](E:\学习笔记\save-img-upload-img/img/20211011092933.png)



## 6	socket之send和recv原理剖析

### ①接收区和缓冲区

当创建一个TCP socket对象的时候会有一个**发送缓冲区**和一个**接收缓冲区**，**这个发送和接收缓冲区指的是内存中的一片空间。**



### ②send和recv的原理剖析图

**send原理剖析**

send是不是直接把数据发给服务端?

不是，要想发数据，必须得**通过网卡发送数据**，应用程序是无法直接通过网卡发送数据的，它需要调用操作系统接口，也就是说，应用程序把发送的数据先写入到**发送缓冲区**(内存中的一片空间)，再**由操作系统控制网卡把发送缓冲区的数据发送给服务端网卡** 。



**recv原理剖析**

recv是不是直接从客户端接收数据?

不是，**应用软件是无法直接通过网卡接收数据的**，它需要调用操作系统接口，**由操作系统通过网卡接收数据**，把接收的数据**写入到接收缓冲区**(内存中的一片空间），应用程序**再从接收缓存区获取客户端发送的数据**。



**send和recv的原理剖析图**

![image-20211011095257696](E:\学习笔记\save-img-upload-img/img/20211011095259.png)





# 十一、闭包和装饰器

## 闭包

### 1	闭包是什么

内部函数对外部函数的变量进行过一些列的操作，会导致外部函数的变量发生变化，所以如果我们需要函数外部变量不发生变化就需要用到闭包，闭包的作用就是不让外部函数的变量被修改，**只在内部函数内部进行修改**，外部函数的变量是不会被修改的

**闭包的构成条件（全部都要满足）**

1.  必须是有函数嵌套，也就是在函数内定义函数
2.  内部函数使用了外部函数的变量（还包括外部函数的参数）
3.  **外部函数返回了内部函数**

**闭包的优点和缺点**

-   **优点**：闭包可以保存外部函数内的变量，不会随着内部函数调用完而销毁
-   **缺点**：由于闭包引用了外部函数的变量，则外部函数的变量没有及时释放，消耗内存



### 2	闭包入门

```python
# 定义一个外部函数
def func_out(num1):
    # 定义一个内部函数
    def func_inner(num2):
        # 内部函数使用了外部函数的变量
        result = num1 + num2
        print("结果是：", result)

    # 外部函数返回了内部函数，这里返回的内部函数就是闭包
    return func_inner

# 创建闭包实例
f = func_out(1)
f(2)
f(3)
```



### 3	闭包案例演示

```python
# 外部函数
def config_name(name):
    # 内部函数
    def say_info(info):
        print(name + ":" + info)

    return say_info

tom = config_name("Tom")

tom("您好！")
tom("你好，在吗？")

jerry = config_name("Jerry")
jerry("不在，不和玩！")
```



### 4	修改闭包内使用的外部变量

就是说在闭包的情况下可以修改外部的变量

**说明**：修改闭包内使用的外部函数变量**使用nonlocal关键字**来完成



**代码演示**

```python
def func_out(num1):
    def func_inner(num2):
        # 告诉解释器，此处使用的是 外部变量
        nonlocal num1
        # 修改外部变量值
        num1 = 10
        result = num1 + num2
        print("结果是：", result)

    print(num1)
    func_inner(1)
    print(num1)
    # 闭包
    return func_inner

if __name__ == '__main__':
    f = func_out(1)
    f(2)
```

**执行结果**

![image-20211013204939399](E:\学习笔记\save-img-upload-img/img/20211013204941.png)

 **解释**：

-   **结果一**：第一次调用的时候是外部的函数，因为外部函数调用了一下内部的方法，所以修改了外部的变量值，最终输出的就是 1 + 10 = 11
-   **结果二**：第二个结果就是闭包实例调用的内部函数，所以直接是2 + 10 = 12

==**总结**：nonlocal关键字的用法和global的用法类似，内部修改外部，局部修改全局==





## 装饰器

### 1	装饰器的定义

就是**给已有函数增加额外功能的函数，它本质上就是一个闭包函数**。

==**自我理解**：就是代理模式的实现，给一个方法添加额外的功能==

**装饰器的功能特点**:

1.   不修改已有函数的源代码
2.   不修改已有函数的调用方式
3.   给已有函数增加额外的功能



### 2	装饰器入门

```python
# 添加一个登陆验证的功能
def check(fn):
    def inner():
        print("请先登陆...")
        fn()
    return inner

def comment():
    print("发表评论")

# 使用装饰器来装饰函数
comment = check(comment)
comment()
```

**说明**：

-   闭包函数有且只有一个参数，而且必须是函数类型的，只有这样定义的函数才是装饰器
-   要遵循开发封闭原则，它规定已经实现的功能代码不允许被修改，但可以被扩展



### 3	装饰器的语法糖写法

之前的装饰器写法有点麻烦，python提供了一个简单的装饰函数的写法，就是语法糖

**格式**：@装饰器名字

**简化上面的代码**

```python
# 添加一个登陆验证的功能
def check(fn):
    def inner():
        print("请先登陆...")
        fn()
    return inner

# 使用语法糖的形式
@check
def comment():
    print("发表评论")

if __name__ == '__main__':
    comment()
```

**说明**：

-   @check等价于 comment = check(comment)
-   装饰器的执行时间是加载模块时立即执行



### 4	装饰器的使用

#### ①装饰器的使用场景

1.  函数执行时间的统计
2.  输出日志信息



#### ②装饰器实现已有函数执事件的统计

```python
import time

# 统计执行事件的装饰器函数
def get_time(fun):
    def inner():
        begin = time.time()
        fun()
        end = time.time()
        print(f"函数执行花费{end - begin}")
    return inner

@get_time
def fun_test():
    for i in range(100000):
        print(i)

if __name__ == '__main__':
    fun_test()
```

**说明**：这尼玛就是代理模式的实现



### 5	通用装饰器的使用

#### ①装饰带有参数的函数

```python
# --1.装饰带有参数的函数--
# 添加输出日志的功能
def logging(fun):
    def inner(num1, num2):
        print("--正在努力计算")
        fun(num1, num2)
    return inner

# 使用装饰器装饰函数
@logging
def sum_num(a, b):
    result = a + b
    print(result)

sum_num(1, 2)
```



#### ②装饰带有返回值的函数

```python
# --2.装饰带有返回值的参数--
# 添加输入日志的功能
def logging(fun):
    def inner(num1, num2):
        print("--正在努力计算")
        result = fun(num1, num2)
        return result
    return inner

# 使用装饰器装饰函数
@logging
def sum_num(a, b):
    result = a + b
    return result

result = sum_num(1, 2)
print(result)
```



#### ③装饰带有不定长参数的函数

```python
# --3.装饰带有不定参数的函数--
# 添加输入日志的功能
def logging(fun):
    def inner(*args, **kwargs):
        print("--正在努力计算")
        fun(*args, **kwargs)
    return inner

# 使用装饰器装饰函数
@logging
def sum_num(*args, **kwargs):
    result = 0
    for value in args:
        result += value
    for value in kwargs.values():
        result += value
    print(result)

sum_num(1, 2, a = 10)
```



#### ④通用装饰器

```python
# --4.通用装饰器--
# 添加输入日志的功能
def logging(fun):
    def inner(*args, **kwargs):
        print("--正在努力计算")
        result = fun(*args, **kwargs)
        return result

    return inner

# 使用装饰器装饰函数
@logging
def sum_num(*args, **kwargs):
    result = 0
    for value in args:
        result += value
    for value in kwargs.values():
        result += value
    return result

@logging
def subtraction(a, b):
    result = a - b
    print(result)

result = sum_num(1, 2, a = 10)
print(result)

subtraction(4, 2)
```



### 6	多个装饰器的使用

```python
def make_div(fun):
    """对被装饰的函数的返回值 div标签"""
    def inner(*args, **kwargs):
        return "<div>" + fun() + "</div>"
    return inner

def make_p(fun):
    """对被装饰的函数的返回值 p标签"""
    def inner(*args, **kwargs):
        return "<p>" + fun() + "</p>"
    return inner

@make_div	# 这个后执行
@make_p		# 这个装饰器先执行
def content():
    return "人生苦短"

result = content()
print(result)
```

**结果显示**

**`<div><p>人生苦短</p></div>`**

**说明**：

-   使用多个装饰器，**离函数最近的装饰器先装饰**，然后外面的装饰器再装饰，由内到外的装饰过程





### 7	带有参数的装饰器

带有参数的装饰器就是使用装饰器装饰函数的时候可以传入指定参数

**类似于java注释传入不同的值可以执行不同的操作**

**语法格式**：@装饰器(参数,...)



#### 错误写法

```python
def decorator(fun, flag):
    def inner(num1, num2):
        if flag == "+":
            print("正在努力加法计算...")
        elif flag == "-":
            print("正在努力减法计算")
        result = fun(num1, num2)
        return result
    return inner

@decorator("+")
def add(a, b):
    result = a + b
    return result

result = add(1, 3)
print(result)
```

**执行结果**

![image-20211015004637912](E:\学习笔记\save-img-upload-img/img/20211015004639.png)

**说明**：装饰器只能接受一个参数，并且还是函数类型的



#### 正确写法

在装饰器外面再包裹一个函数，让最外面的函数接收参数，返回的是装饰器，@符号和面必须是装饰器实例

```python
def logging(flag):
    def decorator(fun):
        def inner(num1, num2):
            if flag == "+":
                print("正在努力加法计算...")
            elif flag == "-":
                print("正在努力减法计算")
            result = fun(num1, num2)
            return result

        return inner

    # 返回装饰器
    return decorator

@logging("+")
def add(a, b):
    result = a + b
    return result

@logging("-")
def sub(a, b):
    result = a - b
    return result

result = add(1, 3)
print(result)

result = sub(1, 2)
print(result)
```





### 8	类装饰器的使用

装饰器还有一种特殊的用法就是类装饰器，就是通过定义一个类来装饰函数。



**示例代码**

```python
class Check(object):
    def __init__(self, fun):
        self.__fun = fun

    # 实现__call__函数，表示对象是一个可调用对象，可以像调用函数一样进行调用
    def __call__(self, *args, **kwargs):
        # 添加装饰功能
        print("请先登陆")
        self.__fun()

@Check
def comment():
    print("发表评论")

comment()
```

**执行结果**

```
请先登陆
发表评论
```

**说明**：

-   @Check等价于comment = Check(comment)，所以要提供一个init方法，并添加一个fun参数
-   要想类的实例对象可以向函数一样调用，需要在类中使用cell函数，把类的实例变成可调用对象(callable)，就可以像调用函数一样进行调用
-   在call方法里进行对fun函数的装饰，来添加额外的功能





# 十二、with语句和上下文管理器

## 1	为什么需要with语句

**首先看一段代码**

![image-20211018090721322](E:\学习笔记\save-img-upload-img/img/20211018090723.png)

**说明**：没有文件读取的话就会报错，所以要防止这个异常出现，我们可以使用try-except-finaly来防止这个异常，但是使用这个的话代码过于冗长，不是很简洁，所以**python提供了with语句这种写法，简单又安全**



## 2	with语句实例语句

==with语句执行完成以后自动调用关闭文件操作，即使出现异常也会自动调用文件关闭操作==

```python
# 使用with来改写
# 不用调用close函数来关闭资源，with会自动帮我们调用
with open("test.txt", "w") as f:
    f.write("hello world")
```





## 3	上下文管理器

**说明**：所谓的上下文，就是语言环境，就像英语，一个词的意思要结合一句话中的上下文来进行判断

**使用**：类实现 `__enter__`和`__exit__`这两个函数，通过这个类创建的对象就是上下文管理器

==**with语句的实现是由上下文管理器来做支撑的，它会在执行之后调用 __ exit __函数**==



### ①第一种实现方式

**代码演示**

```python
"""自定义上下文管理器，模拟文件操作"""
class File(object):

    # 初始化方法
    def __init__(self, file_name, file_model):
        # 定义变量保存文件名和打开模式
        self.file_name = file_name
        self.file_model = file_model

    # 上文函数
    def __enter__(self):
        print("进入上文方法")
        # 返回资源文件
        self.file = open(self.file_name, self.file_model)
        return self.file

    # 下文函数
    def __exit__(self, exc_type, exc_val, exc_tb):
        print("进入下文方法")
        self.file.close()

if __name__ == '__main__':
    # 使用with管理文件
    with File("test.txt", "r") as f:
        f_read = f.read()
        print(f_read)
```

==**总结**：这tm的不就是spring中的Aop环切咩~~~，代理模式的实现==



### ②第二种实现方式

假如想要让一个函数成为上下文管理器，**Python 还提供了一个 @contextmanager 的装饰器**，更进一步简化了上下文管理器的实现方式。通过 yield 将函数分割成两部分，yield 上面的语句在 `__enter__`方法中执行，yield 下面的语句在 `__exit__`方法中执行，**紧跟在 yield 后面的参数是函数的返回值**。

**代码演示**

```python
# 导入装饰器
from contextlib import contextmanager

# 装饰器装饰函数，让它成为一个上下文管理器对象
@contextmanager
def my_open(path, model):
    try:
        # 1 打开文件
        file = open(path, model)
        yield file
    except Exception as e:
        print(e)
    finally:
        print("over")
        file.close()

if __name__ == '__main__':
    # 使用with语句
    with my_open("test.txt", "w") as f:
        f.write("The World is beautiful")
```

**疑问**：为什么不是用return返回file对象

**解答**：因为如果是return的话就直接结束了这个方法，就不会继续往下执行，也就是不会调用close函数了，资源就关闭不了，yield关键字的作用就是暂停，操作完之后再往下执行





# 十三、生成器和深浅拷贝

## 生成器

### 1	生成器介绍

根据程序员制定的规则循环生成数据，当条件不成立时则生成数据结束。数据不是一次性全部生成处理，而是使用一个，再生成一个，可以**节约大量的内存**。



### 2	创建生成器

1.  生成器推导式
2.  yield关键字



#### ①生成器推导式

-   与列表推导式类似，不过生成器推导式使用的是小括号

**说明**：

-   next函数获取生成器下一个值，**拿出了就没有了**，相当于是吐出一个值
-   for循环遍历生成器的每一个值

```python
# 创建生成器
my_generator = (i * 2 for i in range(5))
print(my_generator)

# next获取生成器下一个值
# value = next(my_generator)
# print(value)

# 遍历生成器
for value in my_generator:
    print(value)
```

**执行结果**

![image-20211018112934931](E:\学习笔记\save-img-upload-img/img/20211018112936.png)



#### ②yield关键字

只要在def函数里面看到有 yield 关键字那么就是生成器

**代码演示**

```python
def my_generator(n):
    for i in range(n):
        print("开始生成")
        yield i
        print("完成一次")


if __name__ == '__main__':
    g = my_generator(2)

    # 获取生成器中下一个值
    # result = next(g)
    # print(result)
    #
    # while True:
    #     try:
    #         result = next(g)
    #         print(result)
    #     except StopIteration as e:
    #         break

    # for遍历生成器，for循环内部自动处理了停止迭代异常(推荐使用)
    for value in g:
        print(value)
```

**执行结果**

![image-20211018113820732](E:\学习笔记\save-img-upload-img/img/20211018113821.png)

**代码说明**：

-   代码执行到 yield 会暂停，然后把结果返回出去，下次启动生成器会在暂停的位置继续往下执行
-   生成器如果把数据生成完成，再次获取生成器中的下一个数据会抛出一个StopIteration 异常，表示停止迭代异常
-   while 循环内部没有处理异常操作，需要手动添加处理异常操作
-   for 循环内部自动处理了停止迭代异常，使用起来更加方便（推荐使用）。



### 3	生成器的使用场景

数学中有个著名的斐波拉契数列（Fibonacci），数列中第一个数为0，第二个数为1，其后的每一个数都

可由前两个数相加得到：

0, 1, 1, 2, 3, 5, 8, 13, 21, 34, ...

现在我们使用生成器来实现这个斐波那契数列，每次取值都通过算法来生成下一个数据, **生成器每次调用只生成一个数据，可以节省大量的内存。**

>   **代码实现**

```python
def fibonacci(num):
    a = 0
    b = 1

    # 记录生成fibonacci数字的下标
    current_index = 0
    while current_index < num:
        result = a
        a, b = b, a + b
        current_index += 1
        yield result

if __name__ == '__main__':
    fib = fibonacci(5)
    # 遍历生成的数据
    for value in fib:
        print(value)
```

​			



## 深浅拷贝

### 1	浅拷贝

copy函数是浅拷贝，只对可变类型的第一层对象进行拷贝，对拷贝的对象开辟新的内存空间进行存储，不会拷贝对象内部的子对象。

**代码演示**

>   **不可变类型的浅拷贝**

```python
# 导入copy模块
import copy

# 不可变类型有: 数字、字符串、元组
a1 = 1234312
b1 = copy.copy(a1)
# 查看内存地址
print("a1:", id(a1))
print("a2:", id(b1))

print("-" * 10)

# 字符串类型拷贝
a3 = "我是靓仔智"
a4 = copy.copy(a3)
print("a3:", id(a3))
print("a4:", id(a4))

print("-" * 10)

# 元组拷贝
a5 = (1, 2, ["Hello", "world"])
a6 = copy.copy(a3)
# 查看内存地址
print("a5:", id(a5))
print("a6:", id(a6))
```

**执行结果**

```
a1: 2321203000880
a2: 2321203000880
----------
a3: 2321158299536
a4: 2321158299536
----------
a5: 2321160530344
a6: 2321158299536
```

**总结**：不可变类型的浅拷贝只是**拷贝了对数据的引用地址**，并没有开辟新的空间



>   **可变数据类型的浅拷贝**

```python
import copy

"""可变类型有：列表、字典、集合"""
# 列表
a1 = [1, 34, 3]
a2 = copy.copy(a1)
print("a1:", id(a1))
print("a2:", id(a2))

print("-" * 10)

b1 = {"name": "张三", "age": 20}
b2 = copy.copy(b1)
print("b1:", id(b1))
print("b2:", id(b2))

print("-" * 10)

c1 = {1, 2, "小智"}
c2 = copy.copy(c1)
print("c1:", id(c1))
print("c2:", id(c2))

print("-" * 10)
# 浅拷贝只会拷贝父对象，不会对子对象进行拷贝
d1 = [1, 2, [4, 5]]
d2 = copy.copy(d1)
print("d1:", id(d1))
print("d2:", id(d2))

print("-" * 10)
# 查看内存地址
print("d1子元素:", id(d1[2]))
print("d2子元素:", id(d2[2]))

# 修改数据
d1[2][0] = 6
# 子对象会受到影响
print(d1)
print(d2)
```

**执行结果**

```
a1: 2651749437832
a2: 2651749438344
----------
b1: 2651755265864
b2: 2651755263624
----------
c1: 2651754593864
c2: 2651798625640
----------
d1: 2651754314312
d2: 2651798412872
----------
2651798541576
2651798541576
[1, 2, [6, 5]]
[1, 2, [6, 5]]
```

**说明**：通过上面的执行结果可以得知，可变类型进行浅拷贝只对可变类型的第一层对象进行拷贝，对拷贝的对象会开辟新的内存空间进行存储，子对象不进行拷贝。



### 2	深拷贝

deepcopy函数是深拷贝, **只要发现对象有可变类型就会对该对象到最后一个可变类型的每一层对象就行拷贝**, 对每一层拷贝的对象都会开辟新的内存空间进行存储。

**代码演示**

>   **不可变数据类型的深拷贝**

```python
import copy

# 数字类型
a1 = 1
a2 = copy.deepcopy(a1)
print("a1:", id(a1))
print("a2:", id(a2))

print("-" * 10)
# 字符串类型
b1 = "Hello World"
b2 = copy.deepcopy(b1)
print("b1:", id(b1))
print("b2:", id(b2))

print("-" * 10)
# 元组类型
c1 = (1, 2)
c2 = copy.deepcopy(c1)
print("c1:", id(c1))
print("c2:", id(c2))

print("-" * 10)
# 元组内有列表
d1 = (1, ["李四", "王五"])
d2 = copy.deepcopy(d1)
print("d1:", id(d1))
print("d2:", id(d2))
# 元组内部的成员也会被拷贝
print("d1内子元素:", id(d1[1]))
print("d2内子元素:", id(d2[1]))
```

**执行结果**

```
a1: 140722802696592
a2: 140722802696592
----------
b1: 2571050979440
b2: 2571050979440
----------
c1: 2571046531400
c2: 2571046531400
----------
d1: 2571095572872
d2: 2571095572552
d1内子元素: 2571045982600
d2内子元素: 2571095022792
```

**总结**：不可变类型进行深拷贝**如果子对象没有可变类型则不会进行拷贝，而只是拷贝了这个对象的引用**，否则会对该对象到最后一个可变类型的每一层对象进行拷贝,对每一层拷贝的对象都会开辟新的内存空间进行存储



>   **可变数据类型的深拷贝**

```python
import copy

"""可变类型有：列表、字典、集合"""
# 列表
a1 = [1, 2]
a2 = copy.deepcopy(a1)
print("a1:", id(a1))
print("a2:", id(a2))

print("-" * 10)
# 字典
b1 = {"name": "小智", "age": 18}
b2 = copy.deepcopy(b1)
print("b1:", id(b1))
print("b2:", id(b2))

print("-" * 10)
# 集合
c1 = {1, 2, 4}
c2 = copy.deepcopy(c1)
print("c1:", id(c1))
print("c2:", id(c2))

print("-" * 10)
# 可变类型中存放可变类型
d1 = [1, 2, ["李四", "王五"]]
d2 = copy.deepcopy(d1)
print("d1:", id(d1))
print("d2:", id(d2))

# 子元素的内存地址
print("d1:", id(d1[2]))
print("d2:", id(d2[2]))
# 修改子元素的值
d1[2][0] = "张三"

# 输出 --> 深拷贝重新开辟了一个空间，所以两个之间是独立的
print(d1)
print(d2)

print("-" * 10)
# 可变类型中存放不可变类型
e1 = [1, 2, ("小智", "花花")]
e2 = copy.deepcopy(e1)
print("e1:", id(e1))
print("e2:", id(e2))
print("e1的子元素:", id(e1[2]))
print("e2的子元素:", id(e2[2]))
```

**执行结果**

```
a1: 1855686398280
a2: 1855686398792
----------
b1: 1855689670472
b2: 1855689669672
----------
c1: 1855687949896
c2: 1855732178280
----------
d1: 1855731965768
d2: 1855687670344
d1: 1855732423624
d2: 1855732094600
[1, 2, ['张三', '王五']]
[1, 2, ['李四', '王五']]
----------
e1: 1855732095880
e2: 1855732096328
e1的子元素: 1855687668040
e2的子元素: 1855687668040
```

**总结**：只有是可变类型深拷贝，就会开辟出一个新的空间来存放



### 3	浅拷贝和深拷贝的区别

**浅拷贝最多拷贝对象的一层**

-   就是说子对象是不进行拷贝的

**深拷贝可能拷贝对象的多层**

-   可变类型里存放不可变类型，可变类型开辟新的空间存放，不可变类型只是引用
-   可变类型中存放可变类型，都开辟新的空间存放





# 十四、正则表达式

## 1	正则表达式概述

在实际开发过程中经常会有查找符合某些复杂规则的字符串的需要，

**比如**:邮箱、图片地址、手机号码等，这时候想匹配或者查找符合某些规则的字符串就可以使用正则表达式了。



正则表达式就是记录文本规则的代码



**正则表达式的特点**

-   正则表达式的语法很令人头疼，可读性差
-   正则表达式通用行很强，能够适用于很多编程语言







## 2	re模块介绍

```python
# 导入re模块
import re

# 使用match方法进行匹配操作
result = re.match(正则表达式,要匹配的字符串)
# 如果匹配到数据的话，可以使用group函数来提取数据
result.group()
```

re模块的使用

```python
import re

result = re.match("baidu", "baidu.com")
print(result.group())
```



## 3	匹配单个字符



![image-20211025190600276](E:\学习笔记\save-img-upload-img/img/20211025190602.png)



### ①`.`代码演示

匹配任意一个字符（除了\n）

```python
# .演示
import re
result = re.match(".", "M")
print(result.group())
result = re.match("t.o", "two")
print(result.group())
result = re.match("t.o", "too")
print(result.group())
```



### ②`[]`代码演示

匹配`[]`中列举的字符

```python
# []演示
ret = re.match("h", "hello Python")
print(ret.group())
# hello的首字母为大写
ret = re.match("H", "Hello Python")
print(ret.group())

# 大小写都可以的情况
ret = re.match("[Hh]", "hello python")
print(ret.group())
ret = re.match("[Hh]ello python", "hello python")
print(ret.group())

# 匹配0-9第一种写法
ret = re.match("[0123456789]Hello python", "7Hello python")
print(ret.group())

# 匹配0-9第二种写法
ret = re.match("[1-9]Hello python", "7Hello python")
print(ret.group())

# 下面这个正则不能匹配到数字4
ret = re.match("[0-35-9]Hello", "7Hello")
print(ret.group())
```



### ③`\d`代码演示

匹配数字，既0-9

```python
#  \d演示
ret = re.match("嫦娥1号", "嫦娥1号发射成功")
print(ret.group())

# 使用\d进行匹配
ret = re.match("嫦娥\d号", "嫦娥2号发射成功")
print(ret.group())
```



### ④`\D`代码演示

匹配非数字

```python
# \D演示
ret = re.match("\D", 'f')
print(ret.group())
```



### ⑤`\s`代码演示

匹配空白，既空格，tab

```python
# \s演示
ret = re.match("hello\sworld", "hello world")
print(ret.group())

ret = re.match("hello\sworld", "hello\tworld")
print(ret.group())
```



### ⑥`\S`代码演示

匹配非空格

```python
# /S演示
ret = re.match("hello\Sworld", "hello&world")
print(ret.group())
```



### ⑦`\w`代码演示

匹配非特殊字符，即a-z、A-Z、0-9、_、汉字

```python
# /w演示
ret = re.match("\w", "A")
print(ret.group())
```



### ⑧`\W`代码演示

匹配特殊字符，即非字母、非数字、非汉字

```python
# /W演示
ret = re.match("\W", "&")
print(ret.group())
```





## 4	匹配多个字符

![image-20211025193351180](E:\学习笔记\save-img-upload-img/img/20211025193353.png)



### ① `*`代码演示

匹配前一个字符出现0次或者无限次，即可有可无

```python
ret = re.match("[A-Z][a-z]*", "M")
print(ret.group())
ret = re.match("[A-Z][a-z]*", "Major")
print(ret.group())
ret = re.match("[A-Z][a-z]*", "Mooo")
print(ret.group())
```



### ②`+`代码演示

匹配前一个字符出现1次或者无限次，即至少有1次

```python
# +演示
# 需求：匹配一个字符串，第一个字符是t,最后一个字符串是o,中间至少有一个字符
ret = re.match("t.+o", "two")
print(ret.group())
ret = re.match("t.+o", "ttwwo")
print(ret.group())
```



### ③ `?`代码演示

尽可能少的匹配

匹配前一个字符出现1次或者0次，即要么有1次，要么没有

```python
# ?演示
# 需求：匹配出这样的数据，但是https 这个s可能有，也可能是http 这个s没有
ret = re.match("https?", "http")
print(ret.group())
```



### ④`{m}`代码演示

匹配前一个字符出现m次

```python
ret = re.match("[a-zA-Z0-9_]{6}", "12a3g45678")
if ret:
    print(ret.group())
else:
    print("没有匹配结果")
```



### ⑤ `{m,n}`代码演示

匹配前一个字符出现从m到n次

```python
# 需求：匹配出，8到20位的密码，可以是大小写英文字母、数字、下划线
# {m,n}mn和逗号是不能有空格的
ret = re.match("[a-zA-Z0-9_]{8,20}", "33j8gj383")
print(ret.group())
```



## 5	匹配开头和结尾

![image-20211025195055758](E:\学习笔记\save-img-upload-img/img/20211025195057.png)

### ①开头字符代码演示

```python
ret = re.match("^\dhello", "3hello")
print(ret.group())
```



### ②`$`代码演示

```python
ret = re.match(".*\d$", "hello5")
print(ret.group())
```



### ③组合使用

```python
ret = re.match("^\d.*\d$", "5hello4")
print(ret.group())
```



### ④除了指定字符其他的都匹配

```python
ret = re.match("[^asdf]", "h")
print(ret.group())
```





## 6	匹配分组

![image-20211025200012736](E:\学习笔记\save-img-upload-img/img/20211025200014.png)



### ①`|`演示

匹配左右任意一个表达式

```python
import re

# | 演示
# 需求：在列表中["apple", "banana", "orange", "pear"]，匹配apple和pear
fruit_list = ["apple", "banana", "orange", "pear"]
# 遍历数据
for value in fruit_list:
    # 匹配左右任意一个表达式
    ret = re.match("apple|pear", value)
    if ret:
        print(ret.group())
    else:
        print("不是我要的", value)
```



### ②`(ab)`演示

将括号中字符作为一个分组	

```python
# 需求：匹配出163、126、qq等邮箱
ret = re.match("[a-zA-Z0-9_]{4,20}@(163|126|qq|sina|yahoo)\.com", "hello@163.com")
if ret:
    print(ret.group())
    # 获取分组数据
    print(ret.group(1))
else:
    print("匹配失败")
```

**需求**: 匹配qq:10567这样的数据，提取出来qq文字和qq号码

```python
ret = re.match("(qq):([1-9]\d{4,10})", "qq:13452")
if ret:
    # 获取第一个分组的数据
    print(ret.group(1))
    # 获取第二个分组的数据
    print(ret.group(2))
else:
    print("匹配失败")
```



### ③`\num`演示

```python
# 需求：匹配出 <html>hh</html>
ret = re.match("<[a-zA-Z1-6]+>.*</[a-zA-Z1-6]+>", "<html>hh</html>")
if ret:
    print(ret.group())
else:
    print("匹配失败")
```

引用分组num匹配到的字符串

```python
# 使用\num
ret = re.match("<([a-zA-Z1-6]+)>.*</\\1>", "<html>hh</html>")
print(ret.group())
```

**需求**：匹配出 `<html><h1>www.baidu.com</h1></html>`

```python
# 需求：匹配出 <html><h1>www.baidu.com</h1></html>
ret = re.match("<([a-zA-Z1-6]+)><([a-zA-Z1-6]+)>.*</\\2></\\1>"
               , "<html><h1>www.baidu.com</h1></html>")
if ret:
    print(ret.group())
else:
    print("匹配失败")
```



### ④`(?P<name>)`和`(?P=name)`演示

**需求**：匹配出 `<html><h1>www.baidu.com</h1></html>`

```python
# (?P<name>)和(?P=name)演示
ret = re.match("<(?P<name1>[a-zA-Z1-6]+)><(?P<name2>[a-zA-Z1-6]+)>.*</(?P=name2)></(?P=name1)>"
               , "<html><h1>www.baidu.com</h1></html>")
if ret:
    print(ret.group())
else:
    print("匹配失败")
```

==**注意**：`（?P<name>）`是在分组括号里面的，不是独立一对括号的==





## 7	贪婪匹配和惰性匹配

-   `.*`		贪婪匹配
-   `.*?`      惰性匹配

我们写爬虫用的最多的就是这个惰性匹配

**案例**

```python
str --> 玩儿吃鸡游戏，晚上一起玩游戏，干嘛呢？打游戏啊
reg --> 玩儿.*?游戏
```

 ![image-20211109103637275](E:\学习笔记\save-img-upload-img/img/20211109103639.png)

==**说明**：？是尽可能少的去匹配，可以理解成是匹配第一个的，这个就是惰性匹配，一个字懒~~~==



## 8	正则表达式的方法

**正则表达式前面的r**：正则表达式前的 r 表示原生字符串（rawstring），该字符串声明了引号中的内容表示该内容的原始含义，避免了多次转义造成的反斜杠困扰。



### ①常用方法

-   `match`：只能从字符串的开头进行匹配，如果开头不能匹配，那么返回的就是None

    ```python
    ret = re.match(r"b", "abc")
    print(ret.group())
    ```

    **说明**：这一段代码会报错，因为返回的是None

    

-   `search`：会进行匹配，但是如果匹配到了第一个结果，就会返回这个结果，不再往下匹配，如果匹配不到返回的就是None

    ```python
    # search
    ret = re.search("\d", "我真的还想再活500年，这样我就可以赚到10000000000000元")
    print(ret.group())      # 5
    ```

    

-   `findall`：查找所有匹配的数据，返回list列表

    ```python
    ret = re.findall("\d+", "我真的还想再活500年，这样我就可以赚到10000000000000元")
    print(ret)      # ['500', '10000000000000']
    ```

    

-   `finditer`：和findall的用法一样，不过它返回的是一个迭代器，效率高

    ```python
    ret = re.finditer("\d+", "我真的还想再活500年，这样我就可以赚到10000000000000元")
    print(ret)      # <callable_iterator object at 0x000002DFE8E16B88>
    for it in ret:
        print(it.group())   # 500       10000000000000
    ```



### ②预加载

**预先把正则表达式加载好，需要多次使用的时候就可以不用重新加载**

-   `compile`方法：创建一个预加载正则表达式的对象，这个对象可以调用我们之前讲到的方法
-   `re.S`：让`.`能匹配换行符

```python
import re

obj = re.compile(r"\d+")
ret = obj.finditer("我真的还想再活500年，这样我就可以赚到100000000万了")
for it in ret:
    print(it.group())

# 小案例
str = '''
<div class="jj"><span id="1">歌手</span></div>
<div class="wujing"><span id="2">演员</span></div>
<div class="yuanlongping"><span id="3">国宝</span></div>
<div class="xudizhi"><span id="4">诺贝尔奖得主</span></div>
'''
obj = re.compile('<div class="(?P<name>.*?)"><span id="(?P<id>\d+)">(?P<zhiye>.*?)</span></div>', re.S)
ret = obj.finditer(str)
for it in ret:
    print(it.group("name"))
    print(it.group("id"))
    print(it.group("zhiye"))
```



### ③案例

#### 1.豆瓣Top250爬取

```python
import requests
import re
import csv

url = 'https://movie.douban.com/top250'
headers = {
    'user-agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/95.0.4638.69 Safari/537.36'
}
resp = requests.get(url, headers=headers)

obj = re.compile(r'<li>.*?<span class="title">(?P<name>.*?)</span>'
                 r'.*?<p class="">.*?<br>(?P<year>.*?)&nbsp;/&nbsp;'
                 r'.*?<span class="rating_num" property="v:average">'
                 r'(?P<score>.*?)</span>.*?<span>(?P<num>.*?)人评价</span>', re.S)
result = obj.finditer(resp.text)
f = open('test.csv', mode='w')
csvWriter = csv.writer(f)
for it in result:
    dict = it.groupdict()
    dict['year'] = dict['year'].strip()
    csvWriter.writerow(dict.values())
f.close()
print('over')
```



#### 2.电影天堂

```python
import re
import requests

domain = 'https://www.dydytt.net'

obj1 = re.compile(r'2021新片精品.*?<table width="100%" border="0" cellspacing="0" cellpadding="0">'
                  r'(?P<tbody>.*?)</table>', re.S)
obj2 = re.compile(r"<tr>.*?最新电影下载</a>]<a href='(?P<href>.*?)'>.*?</a><br/>", re.S)
obj3 = re.compile(r"<br />◎译　　名　(?P<yiming>.*?)<br />.*?"
                  r"◎产　　地　(?P<candi>.*?)<br />", re.S)
#  请求对应的网址
resp = requests.get(domain + "/index2.htm")
# 设置编码
resp.encoding = 'gb2312'
result1 = obj1.search(resp.text)
tr = result1.group('tbody')
content = obj2.finditer(tr)
child_url_list = []
# 遍历得到每个子页面的url，保存到列表中
for it in content:
    child_url_list.append(it.group('href'))

child_dict = {}
# 访问每个列表，将东西提取出来
for url in child_url_list:
    child_url = domain + url
    # 发送请求
    child_resp = requests.get(child_url)
    child_resp.encoding = 'gb2312'
    # 获取片名和产地
    child_result = obj3.finditer(child_resp.text)
    for result2 in child_result:
        list = []
        yiming = result2.group('yiming')
        candi = result2.group('candi')
        list.append(yiming)
        list.append(candi)
        child_dict[yiming] = list
print(child_dict)
```







# 十五、python操作mysql

## 1	pymysql介绍

是python用来连接mysql数据库的类库

pymysql为第三方库，需要使用`pip install pymysql`来进行安装





## 2	pymysql连接数据库

创建connect对象

```python
import pymysql

# 创建连接对象，最基本的使用
conn = pymysql.connect(host="localhost", port=3306, user="root"
                , password="root", database="db1", charset="utf8")
```



## 3	增删改查操作

### ①创建connect对象

```python
# 创建连接对象
conn = pymysql.connect(host="localhost", port=3306, user="root"
                , password="root", database="db1", charset="utf8")

# 获取游标对象，它相当于是一个结果集，查询的结果会保存在对象中
cursor = conn.cursor()
```



### ②查询

-   **execute函数**：执行sql语句，返回的是int类型，执行sql语句的影响行数
-   **fetchall函数**：返回查询到的所有结果，遍历得到每一个元组类型的结果
-   **fetchone函数**：返回查询的单个结果，返回就没有了，==如果返回了所有的结果，那么再调用fetchall函数就会返回空的元组，要注意这个问题==
-   **close函数**：关闭资源，游标和连接都要关闭

```python
# 获取游标对象，它相当于是一个结果集，查询的结果会保存在对象中
cursor = conn.cursor()

sql = "select * from t_user"
# execute返回的是int类型，受影响的行数
row_count = cursor.execute(sql)
# print(row_count)
# # 返回查询结果的第一行
# one = cursor.fetchone()
# # 返回查询结果的第二行
# one2 = cursor.fetchone()
# print(one, one2)

# 返回查询结果的所有结果
all = cursor.fetchall()
print(type(all))
# 进行遍历，里面的每个元素都是元组类型
for elem in all:
    print(type(elem))

# 关闭游标
cursor.close()
# 关闭连接
conn.close()
```



### ③增删改

```python
import pymysql

# 创建连接对象
conn = pymysql.connect(host="localhost", port=3306, user="root"
                , password="root", database="db1", charset="utf8")
# 获取游标对象
cursor = conn.cursor()
try:
    # sql = "insert into t_user(name, age) values ('小智',18);"
    # sql = "update t_user set age=20 where name='小智'"
    sql = "delete from t_user where name = '小智'"
    row_count = cursor.execute(sql)
    print(row_count)
    # 一定要提交到数据库，不然数据是不会变化的
    conn.commit()
except:
    # 发生异常就回滚
    conn.rollback()
finally:
    # 无论是否发生错误都要关闭连接
    cursor.close()
    conn.close()
```











# 十六、GUI

这里使用的时python自带的类库`tkinter`



## 1、基础入门

```python
import tkinter

# 创建一个容器
base = tkinter.Tk()
# GUI框上的标题
base.wm_title('hello word')
```





## 2、组件

### 2.1 容器

创建一个主窗口

```python
window = tkinter.Tk()
# 窗口标题
window.wm_title('hello word')
```



### 2.2 标签

```python
'''
tkinter.Label(主容器，text)
    创建一个标签，放在主容器上，它显示的内容是text
    接收类型是Label类型

Label.pack()    -   布局方式 
'''
w1 = tkinter.Label(base, text='我的标签', background='green')
w1.pack()
```



### 2.3 按钮

声明一个`Button`组件，将按钮的回调函数用`command`进行绑定，点击按钮就会触发`command`绑定的函数

```python
def printSB():
    global base
    l = tkinter.Label(base, text='黑仔时傻逼', background='red')
    l.pack()

but = tkinter.Button(base, text='输出傻子', command=printSB)
but.pack()

# 进行时间循环 - 不关闭
base.mainloop()
```



### 2.4 文本框

使用`tkinter`中的Entry创建一个文本框

-   show属性：文本展现的方式
-   get()：获取文本的值

```python
from tkinter import *


root = Tk()
# 设置宽高
root.wm_minsize(500, 500)
root.wm_title('基础入门')

uLabel = Label(root, text='用户名：')
# row表示行，column表示列，sticky表示粘性
uLabel.grid(row=0, column=0, sticky=W)

u = Entry(root)
u.grid(row=0, column=1, sticky=E)

# 密码的label
pLabel = Label(root, text='密码：')
pLabel.grid(row=1, column=0, sticky=E)

# 输入框内容展现的形式，''表示原来形式
p = Entry(root, show='*')
p.grid(row=1, column=1, sticky=E)

info = Label(root,text='')
info.grid(row=3)

def login():
    username = u.get()
    password = p.get()
    if username == '1' and password == '1':
        # 修改text属性的值
        info['text'] = '登录成功'

b = Button(root,text='登录',command=login)
b.grid(row=2,column=0,sticky=E)

root.mainloop()
```







## 3、布局

### 3.1 pack()布局

纵向排列，在前面的依次排列下来，类似html中的div布局

```python
from tkinter import *

baseFrame = Tk()
baseFrame.wm_minsize(500, 500)
'''
    side：组件往那边浮动
    expand：是否扩大，YES和NO
    fill：往那个方向填充
        None：不填充
        X：水平方向填充
        Y：垂直方向填充
        BOTH：水平方向和垂直方向填充
    anchor：浮动的方向
        N、S、E、W：北、南、东、西，通俗的理解为上下左右
            两者可以组合，比如：NE -> 左上角
            注意：不能是两个相反方向使用，比如NS、WE会报错
        CENTER：居中
'''
btn1 = Button(baseFrame,text='A',background='red').pack(side=LEFT,expand=YES,fill=Y)
btn2 = Button(baseFrame,text='B',background='blue').pack(side=TOP,expand=YES,fill=BOTH)
btn3 = Button(baseFrame,text='C',background='orange').pack(side=RIGHT,expand=YES,fill=None,anchor=NE)
btn4 = Button(baseFrame,text='D',background='yellow').pack(side=LEFT,expand=NO,fill=Y)
btn5 = Button(baseFrame,text='E',background='green').pack(side=TOP,expand=NO,fill=BOTH)
btn6 = Button(baseFrame,text='F',background='purple').pack(side=BOTTOM,expand=YES)
btn7 = Button(baseFrame,text='G',background='red').pack(anchor=NW)

baseFrame.mainloop()
```





### 3.2 Grid布局

网格布局，通过row(行)，column(列)控制组件的位置

```python
from tkinter import *

root = Tk()
# 设置宽高
root.wm_minsize(500, 500)
root.wm_title('基础入门')

uLabel = Label(root, text='用户名：')
# row表示行，column表示列，sticky表示粘性
uLabel.grid(row=0, column=0, sticky=W)

u = Entry(root)
u.grid(row=0, column=1, sticky=E)

# 密码的label
pLabel = Label(root, text='密码：')
pLabel.grid(row=1, column=0, sticky=E)

# 输入框内容展现的形式，''表示原来形式
p = Entry(root, show='*')
p.grid(row=1, column=1, sticky=E)

info = Label(root,text='')
info.grid(row=3)

def login():
    username = u.get()
    password = p.get()
    if username == '1' and password == '1':
        # 修改text属性的值
        info['text'] = '登录成功'

b = Button(root,text='登录',command=login)
b.grid(row=2,column=0,sticky=E)

root.mainloop()
```



### 3.3 place布局

定位，绝对定位和相对定位，绝对定位使用 x 和 y 参数，相对定位使用 relx、rely，relheight 和 relwidth 参数





## 4、事件

```python
from tkinter import *

root = Tk()
root.wm_minsize(500, 500)

'''
    bind(sequence, func, add)：事件绑定
        sequence：事件类型
            <Button-1>：1表示鼠标左键、2表示鼠标滚轮、3表示鼠标右键
            <KeyPress-A>：表示A键按下触发事件
            <Control-V>：ctrl+V按下触发事件
            <F1>：按下F1触发事件
        func：函数回调，事件执行的函数
        add：可选项， '' 或 '+' 。
            传入空字符串表示本次绑定将替换与此事件关联的其他所有绑定。
            传递 '+' 则意味着加入此事件类型已绑定函数的列表中。
    bind_all：全局绑定
    bind_class：绑定指定的组件，在组件内触发事件
    注意：回调函数中要有event对象，这个对象记录事件信息
'''


def printSB(event):
    global root
    info = Label(root, text='黑仔是傻逼', background='red')
    info.pack()

l = Label(root, text='输出傻逼')
l.bind('<Button-1>', printSB)
l.pack()

root.mainloop()
```









