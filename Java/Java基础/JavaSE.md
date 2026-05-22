# 一、java基础知识和开发环境

**一  基础常识**
1.1 计算机和用户的交互方式： 图形化界面 vs 命令行方式

**1.2   Dos命令：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828214031389.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)

1.3 Java语言版本迭代概述	
	  jdk1.5开始更名为5.0。 1.5 -> 5.0   1.6 -> 6.0 ......	
   JavaSE(J2SE) : 桌面级应用开发（废弃） + 核心类库（Java基础学习的部分）
	  JavaEE(J2EE) ： 企业级开发 （后台-web）
	  JavaME(J2ME) ： 手机或小智能设备（废弃了） 已经被Android所替代
	 

**二 Java语言应用的领域**

2.1 Java语言的特点
面向对象：面向对象的三大特性：封装性 、继承性、多态性 
健壮性  ： ①去掉了C或C++中的指针  ②增加垃圾回收机制（GC）
跨平台性：一次编译到处运行（依赖于JVM-不同平台有不同的JVM）

**三 开发环境的搭建**
3.1JDK、JRE、JVM的关系	
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828214211839.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)

**四 环境变量的配置**
4.1为什么要配置环境变量 ：
为了能在任何目录下都能访问Java开发工具集

4.2如何配置？
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828214257873.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)





# 二、基础语法

## 2.1 关键字与标识符

### java关键字的使用

定义：被java赋予了特殊含义的字符串(单词)

特点：所有字母都是小写



### 保留字

java现版本暂未使用，但以后的版本可能作为关键字来使用



### 标识符的使用

标识符：凡是需要自己命名的地方都叫标识符



### 标识符的规则与规范

**定义合法标识符规则：**

1.  由26个英文字母大小写，0-9 ，_或 $ 组成
2.  数字不可以开头。
3.  不可以使用关键字和保留字，但能包含关键字和保留字。
4.  Java中严格区分大小写，长度无限制。
5.  标识符不能包含空格。

**Java中的名称命名规范**

1.  包名：多单词组成时所有字母都小写：xxxyyyzzz
2.  类名、接口名：多单词组成时，所有单词的首字母大写：XxxYyyZzz
3.  变量名、方法名：多单词组成时，第一个单词首字母小写，第二个单词开始每个单词首字母大写：xxxYyyZzz
4.  常量名：所有字母都大写。多单词时每个单词用下划线连接：XXX_YYY_ZZZ
5.  注意 ：起名字时一定要做到见名知意





## 2.2 变量的使用

**一 变量的分类**
**1.按照数据类型来分** ：
	基本数据类型：
		整型 ：byte(1字节 = 8bit  表数范围-128 ~ 127) ，short(2字节) ， int(4字节)，long(8字节)
		浮点型 ： float(4字节)，double（8字节）
		字符型 ：char(2)字节
		布尔类型 ：boolean（只有两个值true和false一般忽略大小）
 	    引用数据类型：类（String），接口，数组
       2.安照位置分(面向对象的时候再说)			
**二 定义变量的格式：**
格式： 变量的类型  变量名 =  变量值
		1.声明和赋值同时进行 ：
			int number = 10;
		2.先声明后赋值 : 
			int number;
			number = 10;
		3.同时声明多个变量
			int a,b,c;
			a = b = c = 10;
		4.同时声明多个变量和赋值
			int a = 10,b = 20,c = 30;
**三 变量使用的注意点：**
		1.变量先声明后使用
		2.在同一作用域内的变量名不能相同。
		3.作用域 ：声明那个变量所在的那对大括号内
		4.同一个变量可以被多次赋值，后一次赋值覆盖前一次的值
**四 java常量的默认类型**
	1.java的整型常量默认为 : int
	2.java 的浮点型常量默认为 : double
**五 变量类型的说明**
1.long类型数值后面要加 : l或L
2.float类型的数值后面要加 : f或F
3.double类型后面可以加(一般不加因为默认的常量就是double类型) ：d或D





## 运算符

**算术运算符：**
**一  图示：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828231318856.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)

**二 代码：**
		

		double d = 10 / 4;
		d = 10 * 1.0 / 4;
		d = (10 + 0.0) / 4;
		d = (double)10 / 4;
		d = 10 / 4 * 4; //8.0
		d = 10 * 4 / 4; //10.0  注意 ：乘除的顺序可以调换的情况下，先写乘再写除。
**总结：**
1.要注意看类型给的是谁，double d = 10 / 4这个可以看出来，double是给了d ，运算结果是double类型，先是两个int的值先除等于2
，然后再赋值给d。
2.注意乘除的先后顺序，最好是加个括号。
3.运算时注意值得类型是什么类型。
**三 取模（取余）**
		//作用 ：常常用来判断某个数是否可以被某个数整除
		

		System.out.println(3 % 3); //0
		System.out.println(4 % 3); //1
		System.out.println(5 % 3); //2
		System.out.println(6 % 3); //0
		System.out.println(7 % 3); //1
		System.out.println(8 % 3); //2

//思考 ：结果的正负和谁有关 (和被模数的正负有关)

		System.out.println(-6 % 4);
		System.out.println(6 % -4);
		System.out.println(-6 % -4);
**总结**：结果的正负与被模数（左边的数）有关，被模数是正数，那么结果就是正数，否则相反。
**四： 前++ 后++ 前-- 后--的区别是什么？**
	++a : 先自身加1，再赋值(运算)
  a++ : 先赋值（运算），再自身加1
  --a : 先自身减1，再赋值（运算）
  a--:  先赋值（运算），再自身减1

**赋值运算符：**
**一 赋值运算符 ： =**

*****二 扩展赋值运算符 ：+= -= *= /= %=*****

  int a = 10;
  a += 2;  //可以理解成 a = a + 2;

**三 面试题**

```java
byte b = 10;
b += 2; //编译可以通过，不会改变原来的数据类型
b = b + 2; //编译会出错，因为byte做运算时会先提升为
			//int所以应该使用int类型的变量来接收数据
```


总结：上面的代码中的 （   b += 2; //编译可以通过，不会改变原来的数据类型 ）  可以理解为是不管右边是什么值，都不会改变左边的值。


**比较运算符：**
![在这里插入图片描述](https://img-blog.csdnimg.cn/2020082823122455.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)**特点** ：****结果为布尔类型



**逻辑运算符：**

**一 运算符**

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828231354510.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)



**二 说明：**
	1.&和&& ：两边都为true结果为true (两边有false结果为false)
		2.|和|| ：两边有true结果为true.(两边都为false结果为false)
		3.! : 取反
		4.^ : 两边相同为false,不同为true
		
**三 特点 ：**
			1.对布尔类型的数据进行运算
		2.逻辑运算符的结果为布尔类型
	
**四 [面试题]& 和 && , | 和 ||的区别？**
	**&和&&的区别？**
			左边为true时，&和&&右边的式子都会执行。
			左边为false时，&右边的式子仍然会执行，&&右边的式子不执行（可以直接推理出结果）。
	**|和||的区别**
			左边为false时，|和||右边的式子都会执行。
			左边为true时，|右边的式子仍然会执行，||右边的式子不执行（可以直接推理出结果）。
		


**总结：**&& 和 || 看左边的式子是不是可以推出结果的式子，不是的的话就要执行，是的话就不用执行右边的式子。
	 &  和  | 不管左边式子是什么结果，都要执行右边的式子。


**位运算符：**
**一 运算符
	![在这里插入图片描述](https://img-blog.csdnimg.cn/20200828231518766.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)**

  << 左移 : 在一定范围内每向左移一位，原来的数乘以2
	>> 右移 : 在一定范围内每向右移一位，原来的数除以2
**总结**：&|^~:这些运算符就是把0当false，1当true。
		在一定范围内，左移一位，原来的数乘于2的一次方，两位就是2的二次方，以此类推，例子 ；3 << 2 = 3 * 2的二次方 = 12
		同理，在一定范围内，原来的数除以2
**二 下面两者的区别是什么？**
" >>和>>>的区别？
右移 : 如果是正数最高位用0补，如果是负数最高位用1补
 右右移 : 无论是正数还是负数最高位都用0补

**总结：**>>>结果都是正数
**三元运算符：**
**一 格式：**
	  	（条件表达式）? 表达式1 ： 表达式2;
**二 说明：**
		1.条件表达式的结果为boolean
		2.条件表达式的值为true则执行表达式1，否则执行表达式2
		3.表达式1和表达式2的类型必须一致。(如果可以自动类型提升那么也是可以的)
				比如 ：double d = (m > n)? 100.5 : 100;
		4.三元运算符可以嵌套使用但是不建议
		5.可以使用if-else来替换三元运算符，反之不成立。如果两者都可以使用建议使用三元运算符
			因为效率高一些。	
**总结**：1.能用三元运算符和if-else来解决问题的优先考虑三元运算符。
	    2.不要嵌套使用三元运算符，会增加代码的阅读难度，尽量分开写。

**三 代码：**

		//需求 ：求两个数中的最大值
		int m = 10, n = 5;
		int max = (m > n)? m : n;
		System.out.println(max);
		//需求 ：求三个数中的最大值
		int a = 5, b = 10, c = 20;
		int maxNumber = (a > b)? a : b;
		maxNumber = (maxNumber > c)? maxNumber : c;
		System.out.println(maxNumber);





## 控制语句

**顺序结构**
程序从上到下逐行地执行，中间没任何判断和跳转。

**分支结构**
根据条件，择性地执行某段代码。
if…else和switch-case两种分支语句。

**循环结构**
根据循环条件，重复性的执行某段代码。
while、do…while、for种循环语句。
注：JDK1.5提供了foreach循环，方便的遍历集合、数组元素。



**分支结构：**


**一 if-else**

格式1：
	
	if(条件表达式){
		执行语句;
	}


​		
格式2 : 二选一

​		

```java
if(条件表达式){
			执行语句1;
		}else{
			执行语句2;
		}
	
```

格式3 ：多选一


```java
if(条件表达式1){
		执行语句1;
	}else if(条件表达式2){
		执行语句2;
	}else if(条件表达式3){
		执行语句3;
	}
	else{
		执行语句n;
	}
```

 **说明：**

1.条件表达式的结果为布尔类型
 2.在if-else if结构中如果多个条件表达式的关系为互斥关系那么谁上谁下都可以。
		如果为包含关系那么范围小的在上面范围大的在下面。
 3.在if-else if结构中，else可以省略。那么有可能没有任何一个条件是满足的。

二、**switch-cash**

**格式：**
	
```java
switch(表达式){

case 常量1:
		执行语句;
		break;
case 常量2:
		执行语句2;
		break;
case 常量3:
		执行语句3;
		break;
......

default:
		执行语句n;
		break;

}
```

**说明：**
	1.case后面的常量会和表达式进行匹配，如果匹配成功则进入执行相应的执行语句。
		直到遇到break或者执行到switch-case结构的就会跳出switch-case结构。
		如果和case后面的常量都没有匹配成功那么执行default中的执行语句。
	2.break可以省略。break作用：用来跳出（终止）switch-case结构。
	3.表达式的类型只能是 ：byte,short,int,char,枚举,String
	4.case后面只能跟常量
	5.default的位置是灵活的。当所有case都没有匹配成功时就执行default中的执行语句
	6.使用switch-case的，都可以改写为if-else。反之不成立,都可以使用的情况下用switch-case效率高一些

总结：1.在switch中是可以进行穿透的，可以理解成如果第一个语句没有终结，那么会继续到下一个循环体，一直到
最后面或者是遇到break时程序结束。

例子：

```java
int score = 90;
switch(score / 10){
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 5:
			System.out.println("不合格");
			break;
		case 6:
		case 7:
		case 8:
		case 9:
		case 10:
			System.out.println("合格");
			break;
		}
```

2..用switch时要注意case的位置和顺序



 **循环结构**

 **一、for循环**

 循环的四要素：
	1.初始化条件
	2.循环条件
	3.循环体
	4.迭代条件
	
**二 格式：**

```java
for(初始化条件; 循环条件 ; 迭代条件){
	
		循环体;

}
```

```java
for(1; 2 ; 4){	
		3
}
1.初始化条件只执行一次
2.循环条件的结果为布尔类型，如果为true执行循环体，如果为false则跳出for循环结构
3.执行顺序：  1 -> 2 -> 3 -> 4 ->  2 -> 3 -> 4 ......2
```

三 代码：

​		

```java
//需求 ：输出100以内的偶数,统计偶数的个数,求偶数的和。
		int count = 0; //统计偶数的个数
		int sum = 0; //偶数的和
		for(int i = 1; i <= 100; i++){
			//判断当前i的值是否是偶数
			if(i % 2 == 0){
				sum += i;
				count++;
				System.out.println(i);
			}
		}
		System.out.println("count=" + count + " sum=" + sum);
```

**二、while循环**

**一 循环结构**	

循环的四要素：
	1.初始化条件
	2.循环条件
	3.循环体
	4.迭代条件
	
**二 格式：**

```java
初始化条件;
while(循环条件){
	 循环体;
	 迭代条件;
}
```

注意 ：
	1.必须有迭代条件否则会发生死循环。
	2.for循环和while循环可以相互替换使用

**三 代码：**

		//

```java
需求 ：100以内的偶数，偶数的个数，偶数的总和
		//初始化条件
		int i = 1;
		int count = 0; //统计偶数的个数
		int sum = 0; //偶数的和
		while(i <= 100){//循环条件
			//循环体
			if(i % 2 == 0){
				sum += i;
				count++;
				System.out.println(i);
			}
			i++;	//迭代条件
		}
		System.out.println("count=" + count + " sum=" + sum);	
```

**四 while和for循环的区别？**

while的初始化条件在结构外，for循环的初始化条件在结构内（也可以写在结构外）

**注意**：当while里面的循环条件为true的时候，打印数据的时候是不能一次打印完不换行的，否者不会显示出打印的内容

**三、do-while循环**
**一 循环结构**
循环的四要素：
	1.初始化条件
	2.循环条件
	3.循环体
	4.迭代条件
**二 格式：**
	
 初始化条件;

```java
 do{

	 循环体;
	 迭代条件;
 }while(循环条件);
```

**三 代码：**
	
```java
int i = 1;
do{	
	 if(i % 2 == 0){
		System.out.println(i);
	 }
	 i++;
}while(i <= 100);
```

**四 while和do-while的区别？**

   while的循环体可能一次也不执行，do-while的循环体至少执行一次。


**五、死循环**

**一 格式 ：**

```java
for(; ; ){}

while(true){}

do{
}while(true);
```

**注意**:循环条件最好不要直接写成true。可以使用一个变量。

**二 如何终止循环：**

1.将循环条件的值变为false
2.使用break关键字用来终止循环

**三 代码：**


```java
boolean boo = true;

while(boo){

	  boo = false;
}


while(true){
	
	  break;

}
```

**六、嵌套循环**
**一 嵌套循环** ：一个循环a作为另一个循环b的循环体。那么a循环叫作内层循环，b循环叫作外层循环

**二** 循环体执行的次数 =  外层循环的次数 * 内层循环的次数


**三 说明** ：
可以把外层循环看作行，内层循环看作列。

**四 代码**：

​	

```java
/*
			需求：
					*****
					*****
					*****
					*****
					*****
		*/
		for(int j = 1; j <= 5; j++){ //外层循环控制行
			for(int i = 1; i <= 5; i++){//内层循环控制列
				System.out.print("*");
			}
			System.out.println();
		}

		System.out.println();
		System.out.println("------------------------------------------------");

		/*
			需求：
							 i     j
					*		 1     1
					**		 2	   2
					***		 3	   3
					****	 4	   4
					*****	 5	   5
		*/
		for(int i = 1; i <= 5; i++){
			
			for(int j = 1; j <= i; j++){
				System.out.print("*");
			}

			System.out.println();
		
		}

		System.out.println();
		System.out.println("------------------------------------------------");
		/*
			需求：
								i		j
					*****		1		5
					****		2		4
					***			3		3
					**			4		2
					*			5		1
		*/
		for(int i = 1; i <= 5; i++){
		
			for(int j = 1; j <= 6 - i; j++){
				System.out.print("*");
			}

			System.out.println();
		
		}
		System.out.println();
		System.out.println("------------------------------------------------");
		/*
			需求：
								 i     j	k
					----*		 1     1	4
					---* *		 2	   2	3
					--* * *		 3	   3	2
					-* * * *	 4	   4	1
					* * * * *	 5	   5	0
		*/
		for(int i = 1; i <= 5; i++){
		
			for(int k = 1; k <= 5 - i; k++){ //-
				System.out.print(" ");
			}

			for(int j = 1; j <= i; j++){ //*
				System.out.print("* ");
			}


			System.out.println();
		
		}
```

**总结**：注意转换思维，通过画图去解题




**七、break和continue**

**break :** 
	使用范围 ： 1.在switch-case中   2.在循环结构中 
	作用 ：1.在switch-case结构中用来跳出switch-case结构  2.在循环结构中用来终止当前循环
	，在嵌套循环中 ：用来终止包含它的那个循环的当前循环
例子：

```java
for(int i = 1; i <= 3; i++){
			for(int j = 1; j <= 3; j++){
				System.out.println("j==" + j);
				if(j == 2){
					break; //在嵌套循环中用来结束包含它的那层循环的当前循环
				}
			}
			System.out.println("i===" + i);
		}
		System.out.println();
```

continue
	使用范围 ：循环结构中
	作用 ：在循环结构中用来终止当次循环
	在嵌套循环中 ：用来终止包含它的那个循环的当次循环
```java
for(int i = 1; i <= 3; i++){
			for(int j = 1; j <= 3; j++){
				if(j == 2){
					continue; //在嵌套循环中用来结束包含它的那层循环的当次循环
				}
				System.out.println("j==" + j);
			}
			System.out.println("i===" + i);
		}
```

**如何结束外层循环？**
	1.给外层循环起个名字 ： name : for(; ; ){}
	2.break后面跟循环的名字 ： break name;
```java
lable : for(int i = 1; i <= 3; i++){
			for(int j = 1; j <= 3; j++){
				System.out.println("j==" + j);
				if(j == 2){
					break lable; 
				}
			}
			System.out.println("i===" + i);
		}
		System.out.println("程序结束");
```


**总结**：

嵌套循环中:关于break，在内层循环里面，外层循环不结束，如果是break加外层循环的名字的话就可以结束外层循环。在外层里面的话就直接结束外层循环，外层循环结束的话，整个嵌套循环结束。





## 数组

### 一维数组

**一、数组的声明和初始化**
**声明方式：**

```java
String[] names;
int age[];
```


**静态初始化**：初始化和赋值同时进行

```java
names = new String[]{"aa","cc"};
//下面的声明和初始化不能分开进行
String[] ps = {"aa","cc"};
```

**动态初始化**：初始化和赋值分开进行

String[] ps = new String[3]; //3指的是数组的长度


**注意**：无论是静态初始化还是动态初始化数组一旦创建成功长度不可变。

**二、调用数组中的元素** : 通过索引值调用数组中的元素。索引值(下角标)


是从0开始到(数组的长度-1)
 int[] numbers = {1,2,3,4,5}；

 取值：		
		
int n = numbers[0];

 赋值：
		
numbers[0] = 100;


**三、数组的属性 – length（数组的长度）**

```java
String[] persons = {"小苍","小波","小饭"};

int len = persons.length;
```

**四、数组的遍历 : 根据数组的索引值进行遍历**

```java
String[] persons = {"小苍","小波","小饭"};

for(int i = 0; i < persons.length; i++){

		System.out.println(persons[i]);
}
```

 **五、数组元素的默认值**	
byte short int long -> 0
float double -> 0.0
char -> \u0000
boolean -> false
引用数据类型(数组，接口，类) ->  null



### 数组中常见的算法

```java
//求数值型数组中元素的最大值
		int maxNumber = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if(maxNumber < numbers[i]){
				maxNumber = numbers[i];
			}
		}
		System.out.println("最大值:" + maxNumber);
		
		//求数值型数组中元素的最小值
		int minNumber = numbers[0];
		for (int i = 1; i < numbers.length; i++) {
			if(minNumber > numbers[i]){
				minNumber = numbers[i];
			}
		}
		System.out.println("最小值:" + minNumber);
		
		//求数值型数组中元素的和 ，平均数
		int sum = 0;
		for (int i = 0; i < numbers.length; i++) {
			sum += numbers[i];
		}
		System.out.println("sum=" + sum + "  平均数:" + sum/numbers.length);
		
		//数组的复制
		int[] copyNumbers = new int[numbers.length];
		for (int i = 0; i < copyNumbers.length; i++) {
			copyNumbers[i] = numbers[i];
		}
		//输出数组中的元素
		copyNumbers[0] = 100;
		//思考 ：如果修改了copyNumbers中的数据，numbers中的数据发生变化没？ 没
		String str = Arrays.toString(numbers);//Arrays是操作数组的一个工具类
		System.out.println("numbers=" + str);
		System.out.println("copyNumbers=" + Arrays.toString(copyNumbers));
```



### 二维数组

**一 、二维数组的声明和初始化**
**数组的声明：**

String[][] ps;
	String[] ps[];
	String ps[][];

静态初始化：

```java
String[][] ps = new String[][]{{"aa","110"},{"cc","111"}};
		//下面的声明和初始化不能分开写
		String[][] ps = {{"aa","110"},{"cc","111"}};
```

动态初始化:

```java
String[][] ps = new String[3][2]; //3指的二维数组的长度，2指的是二维数组元素的长度
		String[][] ps = new String[2][];//只是指明了二维数组的长度
		ps[0] = new String[2];
		ps[1] = new String[3];
```

二 调用数组中的元素 :根据索引值来赋值或者获取值

String[][] persons = {{"aa","10"},{"cc","20"}};

**2.1获取值 :**
		String name = persons[1][0]; //1指的是二维数组的索引值，0指的是二维数组元素的索引值
**2.2赋值 ：** 
		persons[1][0] = "abc";
**三 数组的属性 :length**
 String[][] persons = {{"aa","10"},{"cc","20"}};

 获取二维数组的长度 ：persons.length;

 获取二维数组元素的长度 ：persons[0].length;

**四 遍历二维数组**

```java
String[][] persons = {{"aa","10"},{"cc","20"}};

for(int i = 0; i < persons.length; i++){

		for(int j = 0; j < persons[i].length; j++){

			System.out.println(persons[i][j]);
		}

}
```

**五 二维数组元素的默认值 : null**



### Arrays工具类的使用

![在这里插入图片描述](https://img-blog.csdnimg.cn/20200904090447709.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)
**补充**：binarySearch(int[] a, int key)这个方法就是放入一个数组和值，然后返回这个值在数组中的索引值，数组中不存在这个值就返回-1；



### 数组中的异常

数组中常见的异常：
 	
 **1.数组下角标越界** ： ArrayIndexOutOfBoundsException
 		
原因 ：索引值没有在合理的范围之内（0 ~ 数组的长度-1）

 **2.空指针异常** ： NullPointerException
 		
原因 ：不能使用为null的变量去调用属性和方法。	


代码：

​	

```java
	//1.下角标越界：ArrayIndexOutOfBoundsException
		int[] numbers = new int[2]; //0  ~ 1是可以的
//		System.out.println(numbers[2]); 编译错误：超出了索引值的范围
//		System.out.println(numbers[-1]);编译错误：超出了索引值的范围
		
		System.out.println("----------------------------------------");
		
//		2.空指针异常：NullPointerException
		String str = "aaa";
		str = null;
//		System.out.println(str.toUpperCase());编译错误： 因为str的内容为null
		String[][] ps = new String[2][3];
//		System.out.println(ps[0][0].toUpperCase());编译错误： 因为ps[0][0]的内容为null
		String[][] ps2 = new String[2][];//二维数组的元素的默认值为null
		System.out.println(ps2[0][0]);//编译错误：因为ps2[0]为null

```









# 三、面向对象













# 四、异常、断言和日志

处理系统异常信息



## 异常

### 1、异常的体系结构

![在这里插入图片描述](https://img-blog.csdnimg.cn/20201102192314707.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)


```java
|-----Throwable
 		|-----Error : 发生Error是没针对性的代码进行处理的
 		
 		|-----Exception: 发生Exception后针对性的代码进行处理
 			
 			|-----编译时异常：程序在编译时发生的异常(javac 源文件名.java)
 					编译时异常必须处理，否则程序不能执行。
 					FileNotFoundException : 文件找不到异常
 					
 			|-----运行时异常：程序在运行时发生的异常(java 字节码文件名)
 					NullPointerException : 空指针异常
 					ArrayIndexOutOfBounds : 数组下角标越界
```

**二** 从程序执行过程，看编译时异常和运行时异常
    编译时异常： 程序在编译时发生的异常(javac 源文件名.java)
运行时异常：程序在运行时发生的异常(java 字节码文件名)

**三** 【面试题】常见的异常类型，请举例说明：
FileNotFoundException : 文件找不到异常
NullPointerException : 空指针异常
ArrayIndexOutOfBounds : 数组下角标越界





### 2、抓抛模型

抛 （发生异常： 当程序在正常的执行过程中，如果一旦执行到某行代码时发生异常。
		那么系统（JVM将会根据异常的类型
 		创建对应的异常类型的对象并抛出，抛出给代码的执行者。同时终止程序的运行。
 			①系统向外抛   ②手动向外抛 throw
 			
 			
 抓（处理异常：
 			第一种 ： try-catch-finally
 			第二种 ： throws





### 3、异常处理一：try-catch-finally

3.1、异常处理方式一：

```java
1.格式 ： 

try{
	可能会发生异常的代码
}catch(异常类型1 变量名){
	处理异常的代码
}catch(异常类型2 变量名){
	处理异常的代码
}
......
finally{
	一定会执行的代码
}
```

 **2.说明：**

1.在执行try中的代码时一旦发生异常，系统会根据对应的异常类型创建对象并抛出。然后会根据catch后面的异常类型进行匹配，一旦匹配成功则执行相应的代码。执行完毕后如果有finally那么继续执行finally中的代码，然后跳出try-catch结构继续向下执行。如果没和catch后面的异常类型匹配成功，终止程序的执行。如果finally继续执行finally中的代码。
 2.finally可以省略，finally中的代码一定会执行。
 3.catch可以多个，catch后面的异常类型如果存在子父类关系那么子类在上父类在下。如果不存在子父类关系那么谁上谁下都可以
4.可以的结构方式 ： try-finally  try-catch  try-catch-finally

5.处理异常的方式 ： 
	  getMessage()  - 异常信息 （使用自定义异常时用的比较多
	  printStackTrace() - 异常的详细信息

 **3.注意** ：运行时异常一般我们都不处理。



**3.2 finally的再说明**：
  		1.就算在try和catch中执行了return那么finally中的代码也一定会执行。
  		2.就算catch中再次发生异常，finally也一定会执行



### 4、异常处理方式二：throws

格式：throws 异常类型1，异常类型2.......

二. throws和try-catch的区别？

throws :本身没真正的处理掉异常而是把异常向上抛，抛给方法的调用者去处理。
 try-catch-finally :真正的处理掉了异常

三.什么情况下不能使用throws?

1.需要真正处理异常时   2.父类被重写的方法没有throws子类重写的方法也不能throws

四 什么情况下必须使用throws?
		当我们需要调用多个方法进行数据的传递，如果是因为数据的原因造成的异常，
		那么中间任何方法都不能去处理。
		只能由方法的调用者（写数据的那人进行处理。



### 5、手动抛出异常

作用 ：手动向外抛异常
格式 ：throw 异常类的对象
[面试题]throw和throws的区别？
throw : 制造异常
throws : 处理异常





### 6、自定义异常类

一 自定义异常类

  1.自定义一个类并继承Exception（编译时异常或者RuntimeException(运行时异常)
  2.提供两个构造器一个空参一个有参（调用父类的有参构造器）
  3.提供一个serialVersionUID，也可以不写系统默认会添加一个，但是建议显示提供一个


二 代码

```java
public class MyException extends RuntimeException{

	/**
	 * serialVersionUID也可以不写系统默认会添加一个，但是建议显示提供一个
	 */
	private static final long serialVersionUID = -7736287090372348682L;

	public MyException(){
		
	}
	
	public MyException(String message){
		super(message);
	}
}
```



### 7、try-with-Resources

加入资源类实现了`AutoClosable接口`，那么我们可以使用下面快捷的方式，不用在`finally`中关闭资源

```java
try (Resource res = ....) {
    ...
} catch {   
}
```

案例

```java
@Test
public void test1(){
    try (FileInputStream fis = new FileInputStream("text.txt");){
        byte[] bytes = new byte[1024];
        int len;
        while ((len = fis.read(bytes)) != -1) {
            String str = new String(bytes, 0, len);
            System.out.println(str);
        }
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}
```





## 断言

断言就是假设某个属性符合要求，比如变量a是小于10大于100的，如果不是，那么就抛出异常

java中引入了`assert`关键字来进行断言

`assert`的两种写法

```java
assert condition;
assert condition : expression;
```

注意：要使用断言，首先要开启断言，使用虚拟机参数开启

idea中，找到`Edit Configurations`，编制它，在VM Options中填入`-ea`或`-enableassertions`

命令行，使用`java -enableassertions xxx`启动



**代码实现**

```java
// 如果a没有大于2，那么就会报错
int a = 2;
assert a > 2 : a;
```





## 日志

### 概述

我们一开始使用的输出控制台是使用`System.out.println();`语句，但是这个用完了就需要进行删除，很不方便，而且比较耗资源，使用日志来控制信息的输出是最好的

日志的好处

-   可以控制输出的等级
-   自定义显示的位置，可以是控制台、文件等等
-   还可以对记录进行过滤
-   使用格式化方式，例如，纯文本或XML
-   还可以使用多个日志记录器，分别记录不同的包
-   通过日志配置文件进行配置，方便快捷



### 基本日志





### 高级日志









# 五、枚举和注解

## 枚举


枚举类：一个类的对象的个数是有限的可数多个的。


### 一 自定义枚举类

```java
class Season{
	private final String seasonName;
	private final String seasonDes;
	//1.私化构造器
	private Season(String seasonName,String seasonDes){
		this.seasonDes = seasonDes;
		this.seasonName = seasonName;
	}
	
	//2.创建对象
	public final static Season SPRING = new Season("春天","春眠不觉晓");
	public final static Season SUMMER = new Season("夏天","处处蚊子咬");
	public final static Season AUTUMN = new Season("秋天","夜来风雨声");
	public final static Season WINTER = new Season("冬天","冬天穿棉袄");
	
	public String getSeasonName() {
		return this.seasonName;
	}
	public String getSeasonDes() {
		return this.seasonDes;
	}
}
```

  

### 二 使用enum定义枚举类

**1.格式 ：**

权限修饰符（public/缺省的） enum  枚举类名{
	
}

**2.说明：**

   ①使用 enum 定义的枚举类默认继承了 java.lang.Enum类，因此不能再继承其他类
		②枚举类的构造器只能使用 private 权限修饰符
		③枚举类的所实例必须在枚举类中显式列出(, 分隔    ; 结尾)。
		列出的实例系统会自动添加 public static final 修饰
		④必须在枚举类的第一行声明枚举类对象

**3.代码**

```java
enum Season3{
	 //1.声明的对象必须放在首行，多个对象之间用","分开";"结尾
	 //对象前面默认省略的是public static final
	SPRING("春天"),SUMMER("夏天"),AUTUMN("秋天"),WINTER("冬天");
	 
	private final String name;
	private Season3(String name){ //2.构造器默认就是私的也只能是private。
	  this.name = name;
	}	  
	public String getName() {
		return name;
	}	  
}			
```



### 三 枚举类的主要方法

**values()方法**：返回枚举类型的对象数组。该方法可以很方便地遍历所的枚举值。
**valueOf(String str)**：可以把一个字符串转为对应的枚举类对象。要求字符串必须是枚举类对象的“名字”。
			                      如不是，会运行时异常：IllegalArgumentException。



### 四 实现接口的枚举类

```java
interface MyInterface {
	void description();
}

enum Season4 implements MyInterface {
	SPRING {
		@Override
		public void description() {
			System.out.println("这是一个季节的类 SPRING");
		}
	},
	SUMMER {
		@Override
		public void description() {
			System.out.println("这是一个季节的类 SUMMER");
		}
	},
	AUTUMN {
		@Override
		public void description() {
			System.out.println("这是一个季节的类 AUTUMN");
		}
	},
	WINTER {
		@Override
		public void description() {
			System.out.println("这是一个季节的类 WINTER");
		}
	};

}
```



## 注解

注解（ Annotation ）：注解可以对类中的结构进行补充说明，同时不改变原有的结构

**一 jdk内置常用的个注解：**
@Override: 限定重写父类方法, 该注解只能用于方法
@Deprecated: 用于表示所修饰的元素(类, 方法等)已过时。
@SuppressWarnings: 抑制编译器警告



**二 自定义注解**

```java
格式 ： 权限修饰符(public/缺省的)  @interface 注解名{			
		变量的类型  变量名()  default 默认值;
 }
```

**代码：**
		

```java
@interface MyAnn{
	
		}

		@interface MyAnn2{
			//default后面跟默认值。
			String name() default "aaa";//可以理解成属性
		}
```

**注意：**使用自定义注解的时候要改变默认值一定要是给注解里面的值赋值，而不是直接赋值



**三 元注解 ：用在自定义注解上的注解**

```java
@Retention : 用来说明该注解所作用在的注解的生命周期。
    SOURCE: 被编译所抛弃，不使用了。
    CLASS: 编译期间 - 运行期间 （在运行期间该注解就已经死亡了
    RUNTIME: 运行期间 -（在整个运行期间该注解都是存活的

@Target ：用来说明该注解所作用在的注解，可以使用在的结构哪些
@Documented ： 用来说明该注解所作用在的注解，是否可以被javadoc所解析
@Inherited ： 用来说明该注解所作用在的注解，是否可以被子类所继承 

代码实现：
@Retention(RetentionPolicy.RUNTIME)	//元注解
@interface MyAnn{
	String name() default "这是一个注解";
}
```









# 六、集合

## 目录

1.集合的优点
		2.集合的框架体系图
		3.Collection接口的特点和使用
		4.List接口的特点和使用
		5.List接口的实现类
		6.Set接口的特点和使用
		7.Set接口的实现类
		8.Map接口的特点和使用
		9.Map接口的实现类
		10.Collections工具类的使用
		11.泛型的使用

**元素集合：**



![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109202845394.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)


**键值对集合：**


![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109202856246.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70#pic_center)




**集合的优点**
概念：集合是存储一系列对象元素的容器。
集合的优点：
对比数组：1、定长。2、类型统一
集合：1、变长，集合再创建的时候是没长度的，添加元素即可以扩展长度。
类型不固定。集合的存放元素时类型是Object。





## Collection接口的特点：

处于集合继承的顶端，拥有很多集合共有的特性，
一系列对象元素容器，它的元素可以重复，也可以不重复。可以有排序，
可以没有排序。没有具体的实现类。需要更具体的子接口，List和Set来执行具体操作。

**常用方法：**
 * add()添加一个元素进入集合，参数是Object。
如果参数添加的是一个集合的话就是在集合的里面存着一个集合，所以要注意
 * addAll()添加一个集合作为元素，参数是集合。
方法的原理是将要添加的集合遍历放入调用这个方法的集合
 * size()集合的长度：当前集合中的元素数。
 * clear()清除集合中所元素。
 * remove()删除一个元素,参数是Object
 * removeAll()从原集合中删除一个集合中的所有元素，参数是一个集合。
和addAll()方法的原理一样，先遍历，然后再到调用方法的集合里面找要删除的元素
 * contions()方法就是判断集合里面是否存在这个元素，存在返回true，否者false.

Collection接口没获取单个元素的方法，获取单个元素的方法要等到子接口。
Collection con = new Arraylist();//用子接口类实现类的对象


**集合的遍历：**

第一种方式：
迭代器的获取：迭代器对象由集合接口的iterator()方法返回。
注意：迭代器只能使用一次，如果还想要迭代就只能重新再创建一个迭代器，因为next()不会往回走的
哪一个集合接口调用的iterator方法，获取的迭代器就用于遍历这个集合。（谁调遍历谁，只能遍历一次）
iterator里面还有一个	remove()方法

**代码：**

```java
Collection con = new ArrayList();//创建对象
	con.add("苹果");		//添加元素
	Iterator i = con.iterator();	//调用iterator方法
	while(i.hasNext()){	
		String s = (String) i.next();//接收的类型一定要和集合的元素的类型一样
		System.out.println(s);
}
```


**第二种方式：**使用增强for循环遍历


增强for循环用一个冒号分割左侧是元素，右侧是集合。元素的类型就是集合的类型。
从集合里拿出第一个元素赋值给左侧的元素，进入循环。然后知道集合最后一个元素走完循环体退出。
增强for循环实际的底层就是Iterator实现的。

**代码：**


```java
Collection con = new ArrayList();//创建对象
	con.add("苹果");		//添加元素
	for (Object object : con) {	//左边元素 ：右边集合
		String s = (String) object;	//两边的类型一样
		System.out.println(s);
	}
```

**注意**：一定要注意类型转换的类型是否是一致的，不一致会报错。





## List接口的特点和使用

**三种循环方式都可以使用**
**特点**：元素有序、且可重复
序，遍历顺序和插入顺序一致。允许重复记录，两个完全相同的元素存储在集合中时算两个元素。
判定元素重复的原则就是调用对象的equals方法。允许用null做为值。
**常用方法：**
Collection有的，List也有,使用方式也是一样的，下面方法是Collection中没有的。

```java
add(int index,object obj);在指定索引处添加新元素，这个索引必须是0到当前集合size的范围。
addAll(int index,Collection col);在指定索引处添加一个新的集合，这个索引必须是0到当前集合size的范围。
get(int index)返回指定索引处的元素。获取单个元素。
indexOf(Object o)返回指定元素在集合中第一次出现的索引，无法获取第二个以后元素的索引。
如果集合中不包含该元素，则返回-1。
lastIndexOf(Object o)返回指定元素最后一次出现在集合中的索引，如果不包含该元素返回-1。
remove(int index);remove(Object obj):remove方法可以接受索引作为参数，删除索引位置元素，
也可以接受元素作为参数，删除指定元素。（讲人话就是整数会被当做是索引值，而不是要删除的那个整数对象）
set(int index,Object obj)：更新指定索引位置的元素。
```

**遍历方式：**
1.可以使用Collection遍历的两种方式
2.使用索引和传统for循环

**代码：**

```java
List list = new ArrayList();	//创建集合
	list.add("小智");	//添加元素
	//传统for循环加索引
	for (int i = 0; i < list.size(); i++) {
		String s = (String) list.get(i);
		System.out.println(s);
	}
```

**常用三大实现类：**
**ArrayList:**

（使用Debug查看查看运行过程可知道原理）
ArrayList: 是一个可变长度的数组，允许null作为值，允许重复的值，线程不安全。

ArrayList底层维护一个名叫elementData的Object数组，再创建ArrayList对象的时候，底层数组没初始化，长度0。
当第一次调用add方法添加元素的时候，数组初始化，长度是10。
当元素个数组和数组长度一样的时候，再次调用add方法将出现，扩容操作，扩容的长度是原来数组长度的1.5倍。



**LinkedList:**

底层原理：LinkedList的底层是通过链表来实现，是双向非循环链表，就是最后一个元素的next为null。

**链表：**链表是由一系列非连续的节点组成的存储结构，简单分下类的话，链表又分为单向链表和双向链表，而单向/双向链表又可以分为循环链表和非循环链表

LinkedList: 是一个双向链表，允许null作为值，允许重复的值。	
		![在这里插入图片描述](https://img-blog.csdnimg.cn/20201109203509715.png#pic_center)



**add方法的源码（和ArrayList的不同）**：
		**大致思路：**
		①构建一个新节点
		②将新节点作为新的尾节点，如果原来尾节点为null，说明空链表，则将新节点作为头结点，即头结点=尾节点=新		节点，如果不为空，将原来尾节点next=新节点
		③增加链表长度
		此时可以知道，新节点的next=null，即新的尾节点的next=null，那么由此可见，LinkedList只是双向链表不是双向		循环链表



**独有的方法：**
	LinkedList一些独有方法，这些方法可以操作首元素和末元素。
	addFirst()
	addLast()
	removeFirst()
	removeLast()
	getFirst()
	getLast()



**ArrayList和LinkedList的区别**：ArrayList的遍历的查找效率高，增删效率低。LinkedList的遍历，查找效率低，增删效率高。

**Vector:**
 底层可变长度数组。Vector扩容的时候是照两倍扩容。创建对象的同时即初始化数组，长度为10。

**特点：**基本和ArrayList相同，除了Vector是线程安全的以外。效率偏低，不建议使用。








## set接口的特点和使用

**特点：**

这个集合不允许存在重复元素，判定元素重复的方式是调用equals方法，
如果两个元素调用equals的时候返回true，则第二个元素	的添加动作不执行。
Set接口的插入顺序和遍历顺序是不一样的，无序。
**独有方法**：无。
**遍历方式**：iterator和增强for循环


**set的接口实现类**：
**HashSet**：可能有序，可能无序
底层维护一个哈希表，该哈希表实际上是一个Node的数组。
	
当向HashSet添加元素的时候，首先调用元素的hashCode()方法返回该元素的HashCode值，使用这个HashCode值进行运算，得到一个索引，这个索引就是将元素添加进数组的下标。

判断改下标处是否有元素。如果没有则直接添加元素。
如果有元素则调用equals方法判断两个元素是否相等，如果equals返回true，不执行添加操	作。

如果equals返回false，两个元素不相等，但是下标是一样的。将新元素作为来元素的next添加。

**HastSet是如何去重的：**
	首先调用元素的hashCode方法，如果HashCode不相同，则判断两个对象不相同。
	否则，调用equals方法，equals方法返回true则表示两个对象相同，返回false则表示不相同。
	先调用hashCode方法再调用equals方法。

**如果需要我们自定义的类可以被HashSet去重，那么要求我们自定义的类必须重写hshCode和equals两个方法。**

**自我疑问**：既然hashCode可以判断两个对象是否相同，那么为什么还要调用equals方法呢？
**自我解答**：简单的来说就是hashCode不相等，equals就一定不相等，
hashCode相等，equals可能相等 也可能不相等。
这样可以大大的提高效率，所以重写equals方法也要重写hashCode方法

**TreeSet：可以按照业务逻辑排序**
二叉树（下图）：先是放进一个数，先跟自己比，然后后面放进的数要和这个数比较，小于的放左边，大于的放右边。
黑红树：二叉树的进阶版，由于对比第一个数可能会导致一边的数多于另一边的数，导致不平衡，
所以黑红数的做法就是重新排序，将中间的值换成是所以值的中间值，这样两边就会平衡。


TreeSet底层使用红黑树实现。需要添加元素的时候，这个key-value的元素中的key具备排序能力。
因为当向TreeMap中添加元素的时候，需要首先按照key进行排序。如果key不具备排序能力则抛异常。

**排序方式分为：自然排序，定制排序。**
**注意**：集合中的元素必须具备排序能力，如果不具备就会报异常java.langClassCastException（类型转换异常）
		这个时候就要进行自然排序或者是定制排序了。
自然排序：使用元素默认的排序方式。
	需要排序的类要实现Comparable接口，实现compareTo()
compareTo比较器
**代码：**
	

```java
@Override
		public int compareTo(Object o) {
			Book book = (Book)o;
		//按书价排序
			return (int)(this.getPrice()-book.getPrice());
		}
```

**定制排序**：照业务需求的顺序排序（比如逆序）。
	在创建Set对象的时候给出一个叫比较器的实现类对象。然后实现compare()方法。	
返回值为1的时候是顺序，返回值为-1的时候为逆序。
也可以将自然排序中的两个值调换一下位置，这样也可以是倒序排序，但是不推荐使用，需要修改排序的建议定制排序


**代码：内部类**

```java
Set set = new TreeSet(new Comparator() {
	@Override
			public int compare(Object o1, Object o2) {
				Book book1 = (Book) o1;
				Book book2 = (Book) o2;
				//逆序
				int one = book1.getPrice().compareTo(book2.getPrice());
				if(one > 0){	//右边
					return -1;	//变到左边
				}else if(one == 0){
					return 0;
				}else{
					return 1;
				}	
			}
		});
```

**注意**：如果两个都有，那么优先执行定制排序；
**LinkedHashSet：**
是HashSet的子类，也是Set接口的实现类，可以使用Set接口的所方法。是一个既可以去掉重负记录又可以按照插入顺序进行遍历的排序集合。LinkedHashSet的底层是一个哈希表，又是一个链表。

**三者的遍历方式：**
都可以进行迭代器循环、增强for循环、传统for循环，和之前List的传统for循环方式不同：
**代码如下：**

```java
Object[] array = set.toArray();	//变成一个数组
	//然后遍历
	for (int i = 0; i < set.size(); i++) {
		Book book = (Book) array[i];
		System.out.println(book);
	}
```




## map接口的特点和使用

**Map接口**
**特点：**键值对元素集合。存放元素进集合的时候要指定两个值，一个是Key，一个是Value。Map接口的键是一个不允许重复的集合像一个Set，而Map的值允许重复所以更像一个List，但是没有索引。


**独有方法：**

put(Object key,Object value);向集合中存放记录
putAll(Map map);向集合中存放一个完整的集合数据。
Object get(Object key);用键取值的方法。
keySet()；返回所有的键集合，结果是一个Set。
entrySet()；返回所有的键值对关系集合，结果是一个Set。
**遍历方式：**

**遍历键集：**

```java
Iterator iter = map.keySet().iterator();
while(iter.hasNext()){
	Object key = iter.next();
  Object value = map.get(key);
}
```

*关系集遍历*

```java
Iterator iter = map.entrySet().iterator();
while(iter.hasNext()){
	Map.Entry entry = (Map.Entry)iter.next();
  Object key = entry.getKey();
  Object value = entry.getValue();
}
```

**Map的实现类**
**HashMap**
**底层实现**：数组+链表+红黑树(JDK8之后，数组+链表JDK8之前)。影响HashMap存储效率的关键属性有两个，第一个初始化桶的数目，就是底层用于保存数据的那个Node数组的初始化长度。在第一次添加数据的时候初始化长度为16.第二个属性局势负载因子，初识值是0.75.主要作用就是由负载因子去乘数组长度得到一个临界值。所以当数组中的元素达到临界值的时候，数组需要扩容，两倍扩容。HashMap为了保证某一个桶上不要连接太多的元素，所以呢会有个链表变树的动作，首先当一个桶上连接了8个元素的时候，并且此时数组长度达到了64位以上。
**TreeMap**

底层使用红黑树实现。需要添加元素的时候，这个key-value的元素中的key具备排序能力。

因为当向TreeMap中添加元素的时候，需要首先按照key进行排序。如果key不具备排序能力则抛异常。

**排序方式分为：自然排序，定制排序。**	

**LinkedHashMap**
是HashMap的子类，所以基本操作和HashMap一直，底层维护一个数组的同时维护一个链表。所以可以保证插入顺序是遍历顺序。

**HashTable**

HashTable基本上与HashMap一致，除了HashTable是线程安全的和HashTable不允许null作为键和值以外。


**Properties**

这个类是HashTable的子类，用于读取硬盘上的配置文件，这种配置文件需要以.properties。文件中必须是用=分割的键值对，=左侧是键，=右侧是值。



## 栈和队列

### Stack

```java
public class Stack<E> extends Vector<E>
```

Stack 继承 Vector，底层维护一个数组，拥有以下方法：

- push：入栈
- pop：出栈
- peek：获取头元素
- empty：判断是否为空，为空返回 true，否则false
- search：查找给定元素的个数，不存在返回 -1



### Queue

```java
public interface Queue<E> extends Collection<E> {
```

Queue 它是一个接口，继承 Collection

我们看一下 Queue 常用的实现（这里没有涉及并发包下的队列）：

- LinkedList（链表 实现）
- ArrayDequeue（数组实现）














## Collections工具类的使用

**常用方法：**

```java
reverse(List)：反转 List 中元素的顺序
shuffle(List)：对 List 集合元素进行随机排序
sort(List)：根据元素的自然顺序对指定 List 集合元素升序排序
sort(List，Comparator)：根据指定的 Comparator 产生的顺序对 List 集合元素进行排序
swap(List，int， int)：将指定 list 集合中的 i 处元素和 j 处元素进行交换

Object max(Collection)：根据元素的自然顺序，返回给定集合中的最大元素
Object max(Collection，Comparator)：根据 Comparator 指定的顺序，返回给定集合中的最大元素
Object min(Collection)
Object min(Collection，Comparator)
int frequency(Collection，Object)：返回指定集合中指定元素的出现次数
void copy(List dest,List src)：将src中的内容复制到dest中
boolean replaceAll(List list，Object oldVal，Object newVal)：使用新值替换 List 对象的所旧值
```





# 七、泛型

## 1、什么是泛型

统一类型，防止在类型转换的时候发生异常，使用泛型来规定可接收的类型

举个例子：有一个箱子，箱子中放入苹果和梨子，我需要从中获取苹果，这个时候我有可能拿出来的是梨子，那么就不是我想要的，在程序中就会报错

**java 中泛型标记符：**

-   **E** - Element (在集合中使用，因为集合中存放的是元素)
-   **T** - Type（Java 类）
-   **K** - Key（键）
-   **V** - Value（值）
-   **N** - Number（数值类型）
-   **？** - 表示不确定的 java 类型



## 2、使用泛型

-   单个泛型

    ```java
    Set<User> users = new TreeSet<User>();
    ```

    这个集合限定了只有`user`类型才能放入，其他的不能放入

-   多个泛型

    

-   









# 八、IO流

## 1、IO流概述

我们在程序中操作的数据是在内存中的，当我们关闭机器时就会消失，所以我们需要将数据持久化到硬盘中，Java提供一些列的API来操作文件，可以访问文件和目录，将数据转换成二进制存储，将对象序列化到文件中等等。

操作IO流我们有需要注意的点：

1.  所有的IO流对象都实现了Closeable接口，所以在用完之后需要调用close方法关闭流，当然我们也可以使用`try-with-resource`自动关闭
2.  在输出流写出的时候，需要flush一下
3.  注意对象流和数据流两者的用法



### 1.1 完整的流家族

划分出四个顶级父类，它们都是对应的，对应输入和输出

-   操作字节：InputStream和OutputStream
-   操作字符：Reader和Writer

Reader和Writer是用来专门处理Unicode字符的单独的类层次结构，它们的操作都是基于两个字节的Char值的（即Unicode码元）

![image-20230108222921379](D:\学习笔记\img\image-20230108222921379.png)

![image-20230108222939756](D:\学习笔记\img\image-20230108222939756.png)







### 1.2 IO流的体系

![在这里插入图片描述](https://img-blog.csdnimg.cn/20210315194427136.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)





### 1.3 输入/输出流

inputStream中读取字节的方法

| 方法                                       | 解释                                         |
| ------------------------------------------ | -------------------------------------------- |
| abstract int read()                        | 读入一个字节并返回一个字节，结束返回-1.      |
| readAllBytes()                             | 读取流中的所有字节**(9)**                    |
| int read(byte[] b, int off, int len)       | 读入所有字节或者读入某个范围的字节到输入流中 |
| int readNBytes(byte[] b, int off, int len) | 阻塞至所有值都被读入                         |

OutputStream输出字节的方法

| 方法                                   | 解释                                  |
| -------------------------------------- | ------------------------------------- |
| abstract void write(int b)             | 写出一个字节的数据                    |
| void write(byte[] b, int off, int len) | 写出所有字节或某个范围的字节到数组b中 |

当我们完成对输入/输出流的读写时，应用通过调用close来关闭它，节省系统的内存资源。关闭一个输出流的同时还会冲刷用于该输出流的缓冲区，所有数据被置于缓冲区中，以便用更大的包的形式传递的戒子在关闭输出流时都被送出。如果不关闭文件，那么字节的最后一个包可能永远也得不到传递。当然、，我们还可以flush方法来认为的冲刷这些输出。

InputStream、OutputStream、Reader和Writer都实现了Closeable接口，所以都是可以调用close方法来关闭流的。

java.io.Closeable接口扩展了java.lang.AutoCloseable接口。可以使用`try-with-resource`默认关闭流，不用我们手动去关闭流，相当于是调用了close方法。





## 2、操作文件

我们要将数据保存到文件中，首先我们要知道文件的位置在哪，所以第一件事就是要定位文件位置，那么Java在1.0的时候提供了File来操作文件，在Java7.0提供了Path和Files来操作文件，后者是比前者更加的方便和快捷，所以推荐使用



### File类

**File类的常见构造器**：

| 构造器                                  | 作用                                                         |
| --------------------------------------- | ------------------------------------------------------------ |
| public File(String pathname)            | 以pathname为路径创建File对象，可以是绝对路径或者相对路径，<br/>如果pathname是相对路径，则默认的当前路径在系统属性user.dir中存储 |
| public File(String parent,String child) | 以parent为父路径，child为子路径创建File对象。                |
| public File(File parent,String child)   | 放入父路径对象和子路径对象                                   |



**创建File类对象**:

```java
// 全路径
File f1 = new File("E:\\xiaozhi\\java.txt");
// 目录和文件分开
File file = new File("E:\\xiaozhi","java.txt");

File file1 = new File("E:\\xizozhi");
File file2 = new File(file1,"java.txt");
```

**常用方法**：

| 方法            | 作用                                                         |
| --------------- | ------------------------------------------------------------ |
| isDirectory()   | 判断此File对象代表的路径表示是不是文件夹，只有File对象代表路径存在且是一个目录时才返回true，否则返回false。 |
| isFile()        | 判断此File对象代表的路径是否是一个标准文件，只有File对代表路径存在且是一个标准文件时才返回true，否则返回false。 |
| getPath()       | 返回File对象所表示的字符串路径。                             |
| getName()       | 返回此对象表示的文件或目录最后一级文件夹名称。               |
| getParent()     | 返回此File对象的父目录路径名；如果此路径名没有指定父目录，则返回 null。 |
| getParentFile() | 返回File对象所在的父目录File实例；如果File对象没有父目录，则返回 null。 |
| renameTo()      | 重新命名此File对象表示的文件，重命名成功返回true，否则返回false。 |
| mkdir()         | 创建此File类对象指定的目录(文件夹)，不包含父目录。创建成功回true，否则返回false。 |
| mkdirs()        | 创建此File对象指定的目录，包括所有必需但不存在的父目录，创建成功返回true；否则返回false。注意，此操作失败时也可能已经成功地创建了一部分必需的父目录。 |
| createNewFile() | 如果指定的文件不存在并成功地创建，则返回 true；如果指定的文件已经存在，则返回 false；如果所创建文件所在目录不存在则创建失败并出现IOException异常 |
| exists()        | 判断文件或目录是否存在                                       |
| delete()        | 删除File类对象表示的目录或文件。如果该对象表示一个目录，则该目录必须为空才能删除；文件或目录删除成功返回true，否则false。 |
| list()          | 返回由File对象对应目录所包含文件名或文件夹名组成的字符串数组。 |
| listFiles()     | 返回由当前File对象对应目录所包含文件路径或文件夹路径组成的File类型的数组。 |
| separator()     | 指定文件或目录路径时使用斜线或反斜线来写，但是考虑到跨平台，斜线反斜线最好使用File类的separator属性来表示。 |



### Path类







### Files类









## 3、操作文本

文本就是由字符构成的文件，这些文件可以给人类看，二进制文件是人类看不懂的，所以java抽象出Reader和Writer两个抽象类来负责文本的操作。

在Java中字符使用UTF-16编码方式。当然我们也可以指定编码输出，使用`StandardCharsets`中的常量来指定

```java
// StandardCharsets.US_ASCII
// StandardCharsets.ISO_8859_1
// StandardCharsets.UTF_8
// StandardCharsets.UTF_16BE
// StandardCharsets.UTF_16LE
// StandardCharsets.UTF_16
```

`Charset.defaultCharset`可以返回平台所用的编码方式

```java
System.out.println(Charset.defaultCharset());

// UTF-8
```





### 3.1 打印流

我们在学习过程中用到的`System.out.println();`输出用的就是打印流，我们可以控制它输出到那个文件。

```java
// 设置输出的位置
System.setOut(new PrintStream("output.txt"));
System.out.println("我是输出的字符串");
```

其中的Out对象其实就是一个`PrintStream`对象，我们可以称它为输出器。

![image-20230119103424418](D:\学习笔记\img\image-20230119103424418.png)

println方法会自动换行，它会在行中添加对目标系统来说合适的行结束符(window是"\r\n"，UNIX系统是"\n")，也就是通过调用`System.getProperty("line.separator")`获得结束字符。



### 3.2 文本输入流

我们用于获取键盘输入的`scanner`可以获取一个输入流，转换成字符串

```java
Scanner scanner = new Scanner(System.in, "UTF-8");
String next = scanner.next();
System.out.println(next);
```

如果想要将文件一行行地读入，我们可以调用`Files.readAllLines(path, charset)`和`Files.lines()`



### 3.3  字符流

**FileReader**

| 构造方法和主要方法          |                                                              |
| --------------------------- | ------------------------------------------------------------ |
| FileReader(File file)       |                                                              |
| FileReader(String fileName) |                                                              |
| int read();                 | 读取单个字符。返回作为整数读取的字符，如果已达到流末尾，则返回 -1。 |
| int read(char []cbuf);      | 将字符读入数组。返回读取的字符数。如果已经到达尾部，则返回-1。 |
| void close();               | 关闭此流对象。释放与之关联的所有资源。                       |

​			        

**FileWriter**

| 构造方法                                   |                        |
| ------------------------------------------ | ---------------------- |
| FileWriter(File file)                      | 覆盖文件里面原有的内容 |
| FileWriter(File file,boolean append)       | 追加到内容后面         |
| FileWriter(String fileName)                | 覆盖文件里面原有的内容 |
| FileWriter(String fileName,boolean append) | 追加到内容后面         |

| 主要方法                |                                                              |
| ----------------------- | ------------------------------------------------------------ |
| void write(String str)  | 写入字符串。当执行完此方法后，字符数据还并没有写入到目的文件中去。此时字符数据会保存在缓冲区中。此时在使用flush()方法就可以使数据保存到目的文件中去，不过系统会默认提供flush()方法。 |
| void write(char[] cbuf) | 将整个数据写入                                               |
| viod flush()            | 刷新该流中的缓冲。将缓冲区中的字符数据保存到目的文件中去。   |
| viod close()            | 关闭此流。在关闭前会先刷新此流的缓冲区。在关闭后，再写入或者刷新的话，会抛IOException异常。 |



**代码实现**

```java
// 输入
try (Reader reader = new FileReader("output.txt")) {
    char[] buff = new char[1024];
    int len;
    while ((len = reader.read(buff)) != -1) {
        System.out.println(new String(buff, 0, len));
    }
}

// 输出
try (FileWriter writer = new FileWriter("output.txt", true)) {
    writer.write("我是追加的内容");
    // 不用flush，它会自动close
}
```





## 4、操作二进制数据

### 4.1 字节流

通常，我们使用字节流来处理非文本文件(.jpg, .mp3,.mp4,.avi,.doc,.ppt,.xls），使用字符流来处理文本文件(.txt,.java,.py)

它的操作方式和字符流的操作方式一样

-   创建流
-   操作流
-   关闭流

**代码实现**

```java
try (InputStream is = new FileInputStream("1.jpg");
     OutputStream os = new FileOutputStream("2.jpg")) {
    byte[] bytes = new byte[1024];
    int len;
    while ((len = is.read(bytes)) != -1) {
        os.write(bytes, 0, len);
    }
}
```





### 4.2 数据流

将二进制数据转换成Java基本数据类型，比如二进制 -> String

Java提供DataInput和DataOutpu接口定义了对应的方法，由DataInputStream和DataOutputStream实现

写出数据使用DataOutput接口定义的方法

| 方法       | 方法         |
| ---------- | ------------ |
| writeChars | writeFloat   |
| writeByte  | writeDouble  |
| writeInt   | writeChar    |
| writeShort | writeBoolean |
| writeLong  | writeUTF     |

例如，writeInt将整数写出4字节的二进制数量值，其他的方法也是类型的写出Java中设置的字节的值

WriteUTF方法使用修订版的8位Unicode转换格式写出字符串，因为没有其他方法会使用UTF-8的这种修订，所以只有在写出用户Java虚拟机的字符串时才使用WriteUTF方法。例如，当你需要编写一个生成字节码的程序时。

既然有写出，那么也会有写入，写入方法如下

| 方法      | 方法        |
| --------- | ----------- |
| readInt   | readFloat   |
| readByte  | readDouble  |
| readLong  | readChar    |
| readShort | readBoolean |

**注意**：在读入的时候要注意它的顺序要和写入的顺序所用的类型一样，不然会造成数据的混乱



**代码示例**

```java
try (DataOutputStream dos = new DataOutputStream(new FileOutputStream("data.txt"));
     DataInputStream dis = new DataInputStream(new FileInputStream("data.txt"))) {
    dos.writeUTF("我是小智");
    dos.writeChars("我是小智2");
    dos.writeInt(400000);

    System.out.println(dis.readUTF());
    System.out.println(dis.readChar());
    System.out.println(dis.readChar());
    System.out.println(dis.readChar());
    System.out.println(dis.readChar());
    System.out.println(dis.readChar());
    System.out.println(dis.readInt());
}
```

数据流可以做的事对象流也可以，对象流比数据流更强大，但是有许多需要注意的点，后面会讲到





### 4.3 随机流

说是随机流倒不如说是指定流，`RandomAccessFile类`可以将文件指针移到你指定的行数，然后对目标行进行读取或写入数据

可以通过使用字符串“r”(用于读入访问)或“rw”（用于读入/写出访问）作为构造器的第二个参数来指定可以进行的操作

```java
RandomAccessFile randomAccessFile = new RandomAccessFile("test.dat", "r");
RandomAccessFile randomAccessFile = new RandomAccessFile("test.dat", "rw");
```

通过`seek`方法可以将文件指针移动到文件中的任意字节位置，所以使用它要计算好你所需要操作的数据多少字节

| RandomAccessFile方法 |                                                              |
| -------------------- | ------------------------------------------------------------ |
| getFilePointer()     | 返回当前文件指针的位置                                       |
| length()             | 返回当前文件总字节数                                         |
| seek(long n)         | 将文件指针移动到n个字节的位置                                |
| skipBytes(int n)     | 在指针当前位置移动n个字节位置，底层还是调用了seek，但是和seek不同 |



**代码示例**

将员工信息记录到文件中，员工信息用类封装，因此他们的字节长度是一致的，所以通过seek字节来达到存储每一个对象的目的

```java
public class DataIo {

    /**
     * 写出从字符串开头的指定数量的码元
     * @param s     字符串
     * @param size  字节数
     * @param out   数据输出流
     * @throws IOException
     */
    public static void writeFixedString(String s, int size, DataOutput out) throws IOException {
        for (int i = 0; i < size; i++) {
            char ch = 0;
            if (i < s.length()) {
                ch = s.charAt(i);
            }
            out.writeChar(ch);
        }
    }

    /**
     * 从输入流中读入字符
     * @param size  字符串大小
     * @param in    数据输入流
     * @return      字符串
     * @throws IOException
     */
    public static String readFixedString(int size, DataInput in) throws IOException {
        StringBuilder stringBuilder = new StringBuilder(size);
        int i = 0;
        boolean done = false;
        while (!done && i < size) {
            char ch = in.readChar();
            i++;
            if (ch == 0) {
                done = true;
            } else {
                stringBuilder.append(ch);
            }
        }
        // char是两个字节的
        in.skipBytes(2 * (size - i));
        return stringBuilder.toString();
    }
}
```

```java
public static void main(String[] args) throws IOException {
        Employee[] employees = new Employee[3];
        employees[2] = new Employee("小黑", 40000, 2000, 5, 1);
        employees[1] = new Employee("小白", 30000, 2001, 6, 22);
        employees[0] = new Employee("小名", 10000, 1999, 2, 18);

        // 保存到文件中
        try(DataOutputStream out = new DataOutputStream(Files.newOutputStream(Paths.get("data.dat")))) {
            for (Employee e : employees) {
                writeData(e, out);
            }
        }

        // 读取保存的数据
        try (RandomAccessFile in = new RandomAccessFile("data.dat", "r")) {
            int count = (int) (in.length() / Employee.RECORD_SIZE);
            Employee[] employees2 = new Employee[count];
            for (int i = 0; i < count; i++) {
                employees2[i] = readData(in);
            }
            for (Employee employee : employees2) {
                System.out.println(employee);
            }
        }
    }

    /**
     * 写出数据
     * @param e     对象
     * @param out   输出流
     * @throws IOException
     */
    public static void writeData(Employee e, DataOutput out) throws IOException {
        // 写入字符串类型
        DataIo.writeFixedString(e.getName(), Employee.NAME_SIZE, out);
        out.writeDouble(e.getSalary());
        out.writeInt(e.getYear());
        out.writeInt(e.getMonth());
        out.writeInt(e.getDay());
    }

    /**
     * 读取数据
     * @param in    输入流
     * @return      返回@{code Employee}
     * @throws IOException
     */
    public static Employee readData(DataInput in) throws IOException {
        String name = DataIo.readFixedString(Employee.NAME_SIZE, in);
        double salary = in.readDouble();
        int year = in.readInt();
        int month = in.readInt();
        int day = in.readInt();
        return new Employee(name, salary, year, month, day);
    }
```





### 4.4 Zip文档流











## 5、对象流和对象序列化

### 5.1 对象序列化

**概念**

将对象存储到文件中的操作叫序列化，将对象从文件中还原的叫反序列化



**操作**

要想将独享序列化，首先需要实现`Serializable`接口，同时还需要声明序列号，因为在对象被序列化到内存的时候，虚拟机分配的空间所指定的内存地址会发生变化，那么这个时候就需要通过这个序列号找到反序列化回来的对象，然后将它的引用交给变量

```

```









### 5.2 对象流









## 6、缓冲流和内存映射文件

### 6.1 缓冲流





### 6.2 内存映射文件





### 6.3 缓冲区数据结构





## 7、其他流

### 7.1 数组流







### 7.2 转换流











## 字符流

**三部曲**：1.造文件和流	2.数据的输入或输出	3.关闭资源
**FileReader**:

```java
主要构造方法：FileReader(File file)
			FileReader(String fileName)
主要方法：	int read();   // 读取单个字符。返回作为整数读取的字符，如果已达到流末尾，则返回 -1。
         	int read(char []cbuf);//将字符读入数组。返回读取的字符数。如果已经到达尾部，则返回-1。
			void close();         //关闭此流对象。释放与之关联的所有资源。
```

**FileWriter**:

```java
主要构造方法：FileWriter(File file)					覆盖文件里面原有的内容
			   FileWriter(File file,boolean append)		追加到内容后面
			   FileWriter(String fileName)				
			   FileWriter(String fileName,boolean append)
主要方法：void write(String str)   写入字符串。当执行完此方法后，字符数据还并没有写入到目的文件中去。此时字符数据会保存在缓冲区中。此时在使用flush()方法就可以使数据保存到目的文件中去，不过系统会默认提供flush()方法。
		void write(char[] cbuf) : 将整个数据写入
        viod flush()       //刷新该流中的缓冲。将缓冲区中的字符数据保存到目的文件中去。
        viod close()//关闭此流。在关闭前会先刷新此流的缓冲区。在关闭后，再写入或者刷新的话，会抛IOException异常。
```

**代码**：

```java
public void testFileReader() {
    FileReader fr = null;
    try {
        //1.创建file类对象
        File file = new File("java.txt");
        //2.创建流对象
        fr = new FileReader(file);
        //3.读数据
        char[] cbuf = new char[5];
        int len;         //记录读取字符的个数
        while ((len = fr.read(cbuf)) != -1) {
            for (int i = 0; i < len; i++) {
                System.out.print(cbuf[i]);
            }
        }
    } catch (IOException e) {
        e.printStackTrace();
    } finally {
        //4.关流，GC不会回收流的，必须手动关闭
        try {
            if (fr != null)
                fr.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
```


```java
public void testFileWriter() {
        FileWriter fw = null;

        try {
            //1.造文件
            File file = new File("test.txt");

            //2.造流
            fw = new FileWriter(file);

            //3.输出数据的过程
            fw.write("小智是个帅哥");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            //4.关闭资源
            try {
                if (fw != null)
                    fw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```



## 字节流

FileInputStream:和字符流的差不多，没有FileWriter里面的一个构造方法
FileOutputStream:和字符流的差不多

  通常，我们使用字节流来处理非文本文件(.jpg, .mp3,.mp4,.avi,.doc,.ppt,.xls)
           使用字符流来处理文本文件(.txt,.java,.py)

**总结**：①三步走：1.创建file对象和流对象 	2.读或写数据	 3.关闭资源	
	  ②后面创建的内容覆盖前面的
	  ③字符流处理文本文件，字节流可以处理非文本文件和文本文件，
		在处理文本文件上，字符流比字节流更有优势
	  ④ 1. 对于输入流来说，读取的文件一定要存在，否则会报FileNotFoundException
    	 2. 对于输出流来说，要写出到的文件可以不存在。
  如果不存在，则在执行过程中，会自动创建对应的文件
  如果存在，1）使用FileWriter(File file) 或 FileWriter(File file,false):会对已有的文件进行覆盖
            2）FileWriter(File file,true):在现有文件内容的末尾，追加内容
    3. 流，因为都需要进行资源的关闭，所有本章所有的异常，
			只要涉及流资源的关闭，都必须使用try-catch-finally
	  ⑤注意创建数组的长度，因为中文要用三个字节的字符表示，所以如果数组长度不够长就会导致乱码，
		当然，如果你是将文件输出又输入的话就不会，就好像一个人有三分，长度为五的数组是会把一个人手脚分离的。
	  ⑥注意在使用字节流的时候接收的数组类型是byte类型的



## 缓冲流

缓冲流：提高数据读写速度，套接在节点流上
构造方法：里面放的是节点流的对象
使用方法和节点流是类似的

代码：
@Test

```java
public void testBufferedIntputStream() throws IOException {
    //1.
    BufferedInputStream bis = new BufferedInputStream(new FileInputStream("java.txt"));
    //2.
    byte[] cbuf = new byte[1024];
    int len;
    while ((len = bis.read(cbuf)) != -1){
        String str = new String(cbuf,0,len);
        System.out.println(str);
    }
}
```

```java
@Test
public void testBufferedOutputStream() throws IOException {
    //1.
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream("java.txt"));
    //2.
    bos.write("小智是个好学生".getBytes());
    //3.
    bos.close();

}
```


**总结**：①外层close了，里面的可以省略close不写





## 转换流

一、
 * **解码**：字节、字节数组  --->  字符、字符数组、字符串
 * **编码**：字符、字符数组、字符串 ---> 字节、字节数组
![在这里插入图片描述](https://img-blog.csdnimg.cn/20210315214015305.png?x-oss-process=image/watermark,type_ZmFuZ3poZW5naGVpdGk,shadow_10,text_aHR0cHM6Ly9ibG9nLmNzZG4ubmV0L3dlaXhpbl80NjkyMzAxNA==,size_16,color_FFFFFF,t_70)


 * 解码过程中，使用的字符集必须是当初编码时使用的字符集！否则，解码就会出现乱码！

 * 二、**转换流**：
 * InputStreamReader:将输入型的字节流转换为输入型的字符流			编码的过程
 * OutputStreamWriter:将输出型的字符流转换为输出型的字节流			解码的过程




## 对象流

**序列化**    ：把对象转换为字节序列存储于磁盘或者进行网络传输的过程称为对象的序列化。
**反序列化**：把磁盘或网络节点上的字节序列恢复到对象的过程称为对象的反序列化。

**序列化过程**：ObjectOutputStream：实现内存中的数据，写入到具体的文件中
**代码实现**：

```java
 public void testObjectOutputStream() throws Exception {
    ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("object.dat"));
    //操作对象
    oos.writeObject("小智哥");
    //操作自定义对象
    oos.writeObject(new Person(18,"小智"));
    oos.flush();
    //操作基本数据类型
    //oos.writeByte(2);
    //关闭资源
    oos.close();
	}
```

==**注意**：每次WriterObject完之后要接一个flush()刷新==

**反序列化过程**：ObjectInputStream:将磁盘文件中保存的对象，还原为内存中的对象
**代码实现**：

```java
public void testObjectInputStream() throws Exception {

    		ObjectInputStream ois = new ObjectInputStream(new FileInputStream("object.dat"));

   		 String str = (String) ois.readObject();
   		 System.out.println(str);
    		 Person p = (Person) ois.readObject();
   		 System.out.println(p);
    		 ois.close();
	}
```

**对象流的使用**：
 * 1. ObjectInputStream 和 ObjectOutputStream
 * 2. 作用：用于存储和读取基本数据类型数据或对象的处理流。
 *    它的强大之处就是可以把Java中的对象写入到数据源中，也能把对象从数据源中还原回来。

   **面试题**：你是如何理解对象的序列化机制的？
 *  对象序列化机制允许把内存中的Java对象转换成平台无关的二进制流，从而允许把这种二进制流持久地保存在磁盘上， 或通过网络将这种二进制流传输到另一个网络节点。//当其它程序获取了这种二进制流，就可以恢复成原来的Java对象


**要想自定义的类可序列化**，则需要满足：
 * ① 实现接口：java.io.Serializable（可序列化接口）
 * ② 显式声明全局常量：serialVersionUID，用于唯一标识当前类
 * ③ 要想当前类的对象可序列化，必须其所有的属性也是可以序列化的
 *
 * **说明**：1. 默认情况下，基本数据类型的变量都可以序列化
   2. ObjectOutputStream和ObjectInputStream不能序列化static和transient修饰的成员变量

**总结**：①InputStreamReader就是读成我们能看懂的字符
			   OutPutStreamWriter就是转换成计算机看懂的字节
			 ②写的顺序和读的顺序要一致，写的时候是什么数据类型，读的时候也要一致
          ③每次Writer完都要flush()一下刷新，不然是没有写入文件里面的，还在内存里。



## 数组流

**ByteArrayInputStream**

将byte数组转化为流，它继承InputStream或OutputStream

```java
public class ByteArrayInputStream extends InputStream
public class ByteArrayOutputStream extends OutputStream
```

获取它的对象

```java
ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(/* byte数组 */));
ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
```







## 其他流

### 标准流

 标准的输入流：System.in 默认从键盘输入
 标准的输出流：System.out 默认从显示屏输出
    
 重定向：通过System类的setIn，setOut方法对默认设备进行改变。

```java
 public static void setIn(InputStream in)
 public static void setOut(PrintStream out)
```

**总结**：①重定向是指你要存文件的位置，原本是输出在控制台上的，如果进行重定向就会保存在设定的文件里面
      ②要注意里面构造器里面放的类型是不是PrintStream类型和InputStream
		③记得在SetOut传一个true，表示自动刷新，不然是没有数据存进去的
		④要在输出内容的上面写，因为代码是从上开始执行的
**代码实现**：

```java
Scanner sc = new Scanner(System.in);
	System.setOut(new PrintStream(new FileOutputStream("Java_Study\\IO流\\java.txt"),true));
	System.out.println("标准流");
	System.out.println("请输入一个字符");
	String next = sc.next();
	System.out.println(next);
```

### 打印流

 打印流： PrintStream 和 PrintWriter
    1. 提供了一系列重载的print()和println()方法，用于多种数据类型的输出
        2. System.out返回的是PrintStream的实例
	构造方法里面加true刷新

**代码实现**：

```java
public void test打印流() {
    PrintStream ps = null;
    try {
        FileOutputStream fos = new FileOutputStream(new File("E:\\xiaozhi_exer\\java.txt"),true);
        // 创建打印输出流,设置为自动刷新模式(写入换行符或字节 '\n' 时都会刷新输出缓冲区)
        ps = new PrintStream(fos, true);
        if (ps != null) {// 把标准输出流(控制台输出)改成文件
            System.setOut(ps);
        }
        for (int i = 0; i <= 255; i++) { // 输出ASCII字符
            System.out.print((char) i);
            if (i % 50 == 0) { // 每50个数据一行
                System.out.println(); // 换行
            }
        }
    } catch (FileNotFoundException e) {
        e.printStackTrace();
    } finally {
        if (ps != null) {
            ps.close();
        }
    }
}
```



### 数据流（对象流替换）

   DataInputStream 和 DataOutputStream
    **作用**：为了方便地操作Java语言的基本数据类型和String的数据，可以使用数据流。
    **说明**：可以使用对象流替换数据流
**代码实现**：

```java
//数据流
public void testOut() throws IOException {
    DataOutputStream dos = new DataOutputStream(new FileOutputStream("E:\\xiaozhi_exer\\java.txt"));
    dos.writeUTF("小智哥");
    dos.flush();
    dos.close();
}
@Test
public void testIn() throws IOException {
    DataInputStream dis = new DataInputStream(new FileInputStream("E:\\xiaozhi_exer\\java.txt"));
    String s = dis.readUTF();
    System.out.println(s);
}
```





### 随机文件流

RandomAccessFile类的使用：
构造器传参：第一个参数String类型或File类型，第二个参数是r（读）、rw（写）、rws、rwd
 1. 此类直接继承于java.lang.Object类
 2. 此类实现类DataInput和DataOutput接口，此类既可以作为输入流，有可以作为输出流
 3. 如果写出到的文件存在，则不会对文件进行覆盖，而是从头开始对文件内容进行覆盖
 4. 可以实现从文件内容的指定位置开始写入数据：seek(int position)
 5. seek()：对原文件内容进行覆盖，比如输入的是xiaozhi，原内容是HelloWorld123,
		那么最终显示的就是xiaozhirld123,还可以进行插入操作，传的参数就是你要在哪个位置记录。

**代码实现**：

```java
//随机文件流
@Test
public void test输入() throws IOException {
    RandomAccessFile r = new RandomAccessFile("E:\\xiaozhi_exer\\java.txt","r");
    RandomAccessFile rw = new RandomAccessFile("E:\\xiaozhi_exer\\java.txt","rw");

    byte[] cbuf = new byte[1024];
    int len;
    while ((len = r.read(cbuf)) != -1){
        rw.write(cbuf,0,len);
    }
}
```









# 九、反射

## 什么是反射？

反射它是java动态化的关键，它相当于是一面镜子，我们通过它可以得到类中的所有东西，比如私有的属性、构造器、方法，我们都可以通过反射得到它和给它赋值，这就是反射的作用。





## 反射的实现机制

### Class类和获取Class类的实例

我们的java程序运行的步骤：首先我们需要编译我们的java文件，编程完成之后，经过类加载器加载，就到了我们的内存中，然后通过操作平台来执行我们的java程序，这个加载到内存中的过程就叫类的加载，加载到内存中的类也就是运行时类，我们的class对象就可以指向这个运行时类，通过这个运行时类的实例我们可以得到是由那个类加载出来额，从而得到这个类的所有信息

```java
// 四种得到Class实例的方式
Class clazz = Class.forName("com.xiaozhi.java.Person");
Class clazz2 = Person.class;
System.out.println(clazz == clazz2);        // true
Person person = new Person();
Class clazz3 = person.getClass();
System.out.println(clazz3 == clazz);        // true
ClassLoader classLoader = new Reflection().getClass().getClassLoader();
Class clazz4 = classLoader.loadClass("com.xiaozhi.java.Person");
System.out.println(clazz == clazz4);		// true
```



## Class类方法的使用

java类

```java
@MyAnnotation
public class Person extends Persons{

    public String name;
    public char sex;
    private int age;

    @MyAnnotation(value = "小智")
    public void show(String name) throws Exception{
        System.out.println(name + "哇哇哇");
    }
    private void yell () {
        System.out.println("miemiemie");
    }

    public Person() {
    }

    private Person(int age){
        this.age = age;
    }
    public Person(String name, char sex, int age) {
        this.name = name;
        this.sex = sex;
        this.age = age;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public char getSex() {
        return sex;
    }

    public void setSex(char sex) {
        this.sex = sex;
    }

    public int getAge() {
        return age;
    }

    public void setAge(int age) {
        this.age = age;
    }

    @Override
    public String toString() {
        return "Person{" +
                "name='" + name + '\'' +
                ", sex=" + sex +
                ", age=" + age +
                '}';
    }
}
```



```java
public class Reflection {

    public static void main(String[] args) throws Exception {
        // 四种得到Class实例的方式
        Class clazz = Class.forName("com.xiaozhi.java.Person");
        // 通过反射创建对象 newInstance()
        Person person = (Person) clazz.newInstance();
        System.out.println(person);
        System.out.println("========================");

        // 通过反射获取构造器
        Constructor[] constructors = clazz.getConstructors();
        for (Constructor constructor : constructors) {
            System.out.println(constructor);
        }
        // 得到对应参数的构造器，只需要传入参数类型就可以了
        Constructor declaredConstructor = clazz.getDeclaredConstructor(int.class);
        System.out.println(declaredConstructor);

        System.out.println("========================");
        // 通过反射得到全部被public修饰的属性,包括父类的属性
        Field[] fields = clazz.getFields();
        for (Field field : fields) {
            System.out.println(field.getName());
        }
        System.out.println("==========================");
        // 获取的本类中全部的属性（没有父类的）
        for (Field declaredField : clazz.getDeclaredFields()) {
            System.out.println(declaredField);
        }

        System.out.println("==========================");
        // 通过反射获得私有被public修饰的方法(包括父类的)
        Method[] methods = clazz.getMethods();
        for (Method method : methods) {
            System.out.println(method.getName());   // 输出方法名
            System.out.println(method);
        }

        System.out.println("========================");
        // 通过反射得到对应类中的所有方法，是全部的
        Field[] declaredFields = clazz.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            System.out.println(declaredField);
        }
        System.out.println("======================");
        // 属性赋值
        Person person1 = (Person) clazz.newInstance();
        Field age = clazz.getDeclaredField("age");
        age.setAccessible(true);
        age.set(person1,1);

        // 方法赋值
        Method show = clazz.getDeclaredMethod("show", String.class);
        show.setAccessible(true);
        show.invoke(person1,"小智");
    }
}
```

使用反射得到方法的东西

```java
public class MethodTest {

    public static void main(String[] args) throws ClassNotFoundException {
        Class clazz = Class.forName("com.xiaozhi.java.Person");
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            // 注解
            System.out.println("=============注解===========");
            for (Annotation annotation : declaredMethod.getAnnotations()) {
                System.out.println(annotation.annotationType().getSimpleName());
            }
            // 权限修饰符
            System.out.println("=============权限修饰符===========");
            System.out.println(Modifier.toString(declaredMethod.getModifiers()));
            // 方法名
            System.out.println("=============方法名===========");
            System.out.println(declaredMethod.getName());
            // 参数列表
            System.out.println("=============参数列表===========");
            for (Parameter parameter : declaredMethod.getParameters()) {
                System.out.println(parameter);
            }
            // 返回值类型
            System.out.println("=============返回值类型===========");
            System.out.println(declaredMethod.getReturnType().getName());
            // 异常类型
            System.out.println("=============异常类型===========");
            for (Class exceptionType : declaredMethod.getExceptionTypes()) {
                System.out.println(exceptionType.getSimpleName());
            }
        }
    }
}
```

```java
public class ReflectionTest {

    public static void main(String[] args) throws Exception {

        Class<?> clazz = Class.forName("com.xiaozhi.java.Person");
        // 得到运行时类实现的接口
        Class[] interfaces = clazz.getInterfaces();
        for (Class anInterface : interfaces) {
            System.out.println(anInterface.getSimpleName());    // 得到对应接口的名字
            Type[] genericInterfaces = anInterface.getGenericInterfaces();  // 得到接口的泛型的类型
            for (Type genericInterface : genericInterfaces) {
                System.out.println(genericInterface.getTypeName());     // 打印泛型的类型名
            }
        }
        // 得到运行时类所在的包
        Package aPackage = clazz.getPackage();
        System.out.println(aPackage.getName());

        // 得到运行时类的父类和父类的泛型
        Class superclass = clazz.getSuperclass();   // 得到父类
        Type type = superclass.getGenericSuperclass();  // 得到泛型的类型
        System.out.println(type.getTypeName());
    }
}
```





## spring框架的初步原理

通过bean注解的机制原理，通过反射得到有@bean注解的类，然后创建对象，这也就是IOC的底层原理的一部分

```java
public class TestInstance {

    public static void main(String[] args) throws Exception {
        for (int n = 0; n < 100; n++) {
            int i = new Random().nextInt(2);
            switch (i){
                case 0:
                    String classPath = "com.xiaozhi.java.Person";
                    System.out.println(getInstance(classPath));
                    break;
                case 1:
                    System.out.println(getInstance("java.util.Date"));
                    break;
                case 2:
                    System.out.println(getInstance("java.sql.Date"));
                    break;
            }
        }
    }

    public static Object getInstance(String classPath) throws Exception {
        Class clazz = Class.forName(classPath);
        Object instance = clazz.newInstance();
        return instance;
    }
}
```





## 代理模式

### 静态代理

```java
public class StaticProxy {

    public static void main(String[] args) {
        Homeowners homeowners = new Homeowners();   // 房主（被代理类）
        intermediary intermediary = new intermediary(homeowners);   // 中介（代理类）
        intermediary.lease();   // 调用出租方法
    }
}

class Homeowners implements House {

    @Override
    public void lease() {
        System.out.println("出租房子");
    }
}


// 中介,代理类
class intermediary implements House {
    Homeowners homeowners;

    public intermediary(Homeowners homeowners) {
        this.homeowners = homeowners;
    }

    public intermediary() {
    }

    @Override
    public void lease() {
        homeowners.lease();
        System.out.println("中介做了其他事");
    }
}


interface House {
    /**
     * 出租房子
     */
    public void lease();
}
```



### 动态代理

```java
public class DynamicProxy {

    public static void main(String[] args) {
//        new DynamicProxy().huMan();
//        new DynamicProxy().house();
        HuMan proxyNewInstance = (HuMan) ProxyUtils.getProxyNewInstance(new SuperMan());
        proxyNewInstance.say();
    }

    public void huMan(){
        HuMan instance = (HuMan) ProxyTest.getProxyInstance(new SuperMan());
        instance.say();
    }
    public void house(){
        House proxyInstance = (House) ProxyTest.getProxyInstance(new Homeowners());
        proxyInstance.lease();
    }

}

// 创建动态代理的类
class ProxyTest{
    public static Object getProxyInstance(Object obj){ // obj：被代理类的对象
        MyInvocationHandler handler = new MyInvocationHandler(obj);
        return Proxy.newProxyInstance(obj.getClass().getClassLoader(),obj.getClass().getInterfaces(),handler);
    }
}

class MyInvocationHandler implements InvocationHandler{

    private Object obj;

    public MyInvocationHandler(Object obj) {
        this.obj = obj;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object returnValue = method.invoke(obj, args);
        return returnValue;
    }
}

// 被代理类
class SuperMan implements HuMan{

    @Override
    public void say() {
        System.out.println("我相信自己会飞");
    }
}


interface HuMan{
    public void say();
}
```





### 封装成工具类

只需要传入一个被代理的对象，就可以动态的创建代理类，实现动态代理的实现

```java
public class ProxyUtils {

    public static Object getProxyNewInstance(Object obj){
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                Object invoke = method.invoke(obj, args);
                return invoke;
            }
        };
        return Proxy.newProxyInstance(obj.getClass().getClassLoader(),obj.getClass().getInterfaces(),handler);
    }
}
```







------





# 十、网络编程



## IP和端口号

**IP**：是每一台计算机的标注，相当于是地址，我们通过地址找到对应的人，计算机也是，他们通过IP找到对应的计算机

**端口号**：是每个程序的的标识，计算机通过对应的端口号找到运行的程序



## IentAddress类

```java
public static void main(String[] args) throws UnknownHostException {
    // 得到对应的IP
    InetAddress address = InetAddress.getByName("www.baidu.com");
    System.out.println(address);

    // 得到本地主机名或者IP
    System.out.println(InetAddress.getLocalHost());

    // 得到主机名
    System.out.println(address.getHostName());

    // 得到主机IP
    System.out.println(address.getHostAddress());
}
```

**补充**：192.168.0.0~192.168.255.255属于是局域网的范围





## 网络编程

TCP：这是一个可靠的传输协议，因为它经历了三次握手，第一次是询问告诉对方我是谁，第二次是对方告诉它知道我是谁，第三次是我告诉对方我知道了你知道我是谁，然后就可以建立传输通道，彼此之间传输资源。传输资源结束后要经历四次挥手，首先是服务端告诉客户端我已经完成了传输，客户端接收到后告诉服务端我知道了你已经停止了传输，第三次是客户端告诉服务端它已经关闭了通道，最后一次是服务端验证客户端关闭了通道，相对于UDP协议来说，它是效率慢的，一般用于需要安全性高的信息传递

UDP：这是一个不可开的传输协议，它不用管你接收到了没，只管一脑子发送数据包，所以它效率非常高，它不用握手，适用于传输视屏资源



### 简单实现

```java
// 客户端
public class ScoketTest {

    public static void main(String[] args) throws IOException {
        // 创建一个socket对象
        Socket socket = new Socket("localhost",8080);
        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("我是靓仔智".getBytes());
        outputStream.close();
        socket.close();
    }

}
// 服务器端
class SocketServerTest{
    public static void main(String[] args) throws IOException {
        // 创建一个ServerSocket对象
        ServerSocket ss = new ServerSocket(8080);
        Socket accept = ss.accept();    // 监听端口
        InputStream inputStream = accept.getInputStream();
        int len;
        byte[] buffer = new byte[20];
        while ((len = inputStream.read(buffer)) != -1){
            String str = new String(buffer, 0, len);
            System.out.println(str);
        }
    }
}
```





### 服务器实现

```java
/**
 * 服务端
 *
 * @author xiaozhi
 */
public class Server {


    public static void main(String[] args) {
        Server server = new Server();
        try {
            server.openService();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, String> map = new HashMap<>();

    static {
        map.put("获取姓名", "小智");
        map.put("执行python脚本", "已经开始执行");
        map.put("获取女朋友", "滴，请签收一个漂亮大长腿女朋友");
    }

    /**
     * 开启服务
     */
    public void openService() throws IOException {
        System.out.println("===============服务器启动=================");
        // 创建一个socket对象
        ServerSocket socket = new ServerSocket(8080);
        while (true) {
            // 监听端口
            Socket sc = socket.accept();
            // 获取流
            InputStream is = sc.getInputStream();
            // 获取返回内容
            DataInputStream dataInputStream = new DataInputStream(is);
            String content = dataInputStream.readUTF();
            System.out.println("接收来自客户端的信息：" + content);

            // 返回信息
            String result = map.get(content);
            if (StringUtils.isBlank(result)) {
                result = "没有该指令";
            }
            DataOutputStream dataOutputStream = new DataOutputStream(sc.getOutputStream());
            dataOutputStream.writeUTF(result);
            dataOutputStream.flush();
            sc.close();
        }
    }   
}
```

```java
/**
 * 客户端
 *
 * @author xiaozhi
 */
public class Client {

    public static void main(String[] args) {
        Client client = new Client();
        try {
            client.openClient();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void openClient() throws IOException {
        Scanner sc = new Scanner(System.in);
        while (true) {
            // 创建socket对象
            Socket socket = new Socket("localhost", 8080);
            // 获取输出流
            System.out.println("请输入指令：");
            String content = sc.nextLine();
            // 将内容输出
            OutputStream os = socket.getOutputStream();
            DataOutputStream dataOutputStream = new DataOutputStream(os);
            dataOutputStream.writeUTF(content);
            dataOutputStream.flush();

            // 读取服务器返回的内容

            DataInputStream dataInputStream = new DataInputStream(socket.getInputStream());
            System.out.println(dataInputStream.readUTF());

            socket.close();
        }
    }
}
```

注：还可以进阶，可以使用多线程技术进行改进，也可以使用线程池来进行改进







## URL编程

URL（Uniform Resource Locator）:统一资源定位符，它表示internet上某一资源的地址

URL的组成：<传输协议>://<主机名>:<端口号>/<文件名>

```java
public class URLTest {

    public static void main(String[] args) throws Exception {
        URL url = new URL("https://baike.baidu.com/item/%E4%BA%BA%E7%B1%BB%E7%9A%84%E8%B5%B7%E6%BA%90/81923?fr=aladdin");
        System.out.println(url.getProtocol());  // 获取改URL的协议名
        System.out.println(url.getHost());      // 获取改URL的主机名
        System.out.println(url.getPort());      // 获取改URL的端口号
        System.out.println(url.getPath());      // 获取该URL的文件路径
        System.out.println(url.getFile());      // 获取改URL的文件名
        System.out.println(url.getRef());       // 获取该URL在文件中的相对位置
        System.out.println(url.getQuery());     // 获取该URL的查询名，可以理解为携带的参数

        // 将服务端的资源读取进来，也就是服务端response返回的资源
        InputStream is = url.openStream();
        int len;
        byte[] buffer = new byte[1024];
        while ((len = is.read(buffer)) != -1){
            String str = new String(buffer,0,len);
            System.out.println(str);
        }
    }
}
```









 

------







#  十一、GUI

​     •Graphical User Interface(图形用户接口)。

​     •用图形的方式，来显示计算机操作的界面，这样更方便更直观。





## GUI的继承体系

组件：窗口、弹窗、面板、文本框、列表框、按钮、图片、监听事件(鼠标、键盘事件)

![image-20210108181544413](D:\学习笔记\img\image-20210108181544413.png)





## AWT

### Frame窗口的创建

Frame类下的方法

-   setSize		设置窗口大小,width设置宽，height设置高度
-   setBounds();设置窗口起始坐标和窗口大小
-   setResizable()；设置窗口大小是否固定，true不固定，false固定
-   setVisible();设置是否可见
-   addWindowListener()；添加监听事件



**代码实现**

```java
public static void main(String[] args) {
        // 创建一个窗口并起名
        Frame frame = new Frame("小智的GUI");
//        // 设置窗口的大小,width设置宽，height设置高度
//        frame.setSize(500,500);
    
        // 设置窗口起始坐标和窗口大小
        frame.setBounds(300,300,500,500);

        // 设置背景颜色：创建对象传参的方式和直接调它的常量的方式
        frame.setBackground(Color.CYAN);

        // 设置窗口固定,不能进行拉伸和放大，大小固定
        frame.setResizable(false);

        // 设置是否可见
        frame.setVisible(true);

        // 设置窗口关闭的监听事件
        // 适配者模式：传入实现windowListener接口的实现类WindowAdapter
        frame.addWindowListener(new WindowAdapter() {
            // 重写windowClosing方法
            @Override
            public void windowClosing(WindowEvent e) {
                // 结束程序  System.exit(0) 0表示正常关闭，1表示异常关闭
                System.exit(0);
            }
        });
    }
```



### 封装Frame窗口信息

```java
public class MyFrame extends Frame {
    public MyFrame(int x,int y,int width,int height,Color color){
        setBounds(x, y, width, height);
        setBackground(color);
        setVisible(true);
        setResizable(false);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                // 关闭窗口
                System.exit(0);
            }
        });
    }
}
```

只需要创建这个类的对象就可以了，不用再设置窗口





### panel面板

说明：面板就是分出一块区域，相当于画画，天空一块区域，地面一块区域

==**注意：如果没有设置布局，面板是会置底的，就看不到面板了**==

```java
public class CreatePanel {
    public static void main(String[] args) {
        Frame frame = new Frame("小智的窗口");
        frame.setResizable(false);
        frame.setBackground(Color.CYAN);
        frame.setBounds(300,300,500,500);

        // 创建panel对象并添加到Frame窗口中
        Panel panel = new Panel();
        panel.setBounds(50,50,300,300);
        panel.setBackground(Color.red);
        frame.add(panel);
        // 设置布局，如果不添加布局的加面板默认置底的
        frame.setLayout(null);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
```





### 布局管理器

#### 流式布局(FlowLayout)

一行排好

```java
@Test
public void Test1(){
    // 调用封装好的Frame窗口类
    MyFrame myFrame = new MyFrame(600, 600, 300, 300, Color.yellow);
    // 创建按钮
    Button button = new Button("按钮1");
    Button button2 = new Button("按钮2");
    Button button3 = new Button("按钮3");
    // 添加布局
    myFrame.setLayout(new FlowLayout(FlowLayout.LEFT));
    // 将按钮添加到窗口中
    myFrame.add(button);
    myFrame.add(button2);
    myFrame.add(button3);
}
```

![image-20210108191659389](D:\学习笔记\img\image-20210108191659389.png)

#### 东西南北中(BorderLayout)

上下左右中的布局

```java
public static void main(String[] args) {
    MyFrame myFrame = new MyFrame(600, 150, 800, 800,null);
    Button east = new Button("east");
    Button west = new Button("west");
    Button south = new Button("south");
    Button north = new Button("north");
    Button center = new Button("center");
    // 在添加中设置布局
    myFrame.add(east,BorderLayout.EAST);
    myFrame.add(west,BorderLayout.WEST);
    myFrame.add(south,BorderLayout.SOUTH);
    myFrame.add(north,BorderLayout.NORTH);
    myFrame.add(center,BorderLayout.CENTER);
}
```

![image-20210108192919059](D:\学习笔记\img\image-20210108192919059.png)

**说明**：它会自动拉伸



#### 表格布局(GridLayout)

-   rows设置行
-   cols设置列

```java
public static void main(String[] args) {
    MyFrame myFrame = new MyFrame(600, 150, 800, 800,null);
    Button button1 = new Button("but1");
    Button button2 = new Button("but2");
    Button button3 = new Button("but3");
    Button button4 = new Button("but4");
    // 添加布局
    myFrame.setLayout(new GridLayout(2,2));
    myFrame.add(button1);
    myFrame.add(button2);
    myFrame.add(button3);
    myFrame.add(button4);
}
```

![image-20210108193745876](D:\学习笔记\img\image-20210108193745876.png)







#### 添加组件方法的说明

![image-20210108190441426](D:\学习笔记\img\img/20220308101327.png)

说明：所有组件都继承了Component类，所以他们能被添加进来



#### 练习

做出一个这样的布局

![image-20210108200125214](D:\学习笔记\img\image-20210108200125214.png)

**代码实现**

```java
public class Demo {
    public static void main(String[] args) {
        // 创建我们需要的按钮
        Button button1 = new Button("button1");
        Button button2 = new Button("button2");
        Button button3 = new Button("button3");
        Button button4 = new Button("button4");
        Button button5 = new Button("button5");
        Button button6 = new Button("button6");
        Button button7 = new Button("button7");
        Button button8 = new Button("button8");
        Button button9 = new Button("button9");
        Button button10 = new Button("button10");

        // 得到封装好的Frame窗口对象
        MyFrame myFrame = new MyFrame(600, 150, 800, 800,null);
        myFrame.setLayout(new GridLayout(2,1));

        // 大面板一
        Panel panel1 = new Panel(new BorderLayout());
        panel1.add(button1,BorderLayout.WEST);      // dongle方向
        // 小面板
        Panel panel2 = new Panel(new GridLayout(2,1));
        panel2.add(button2);
        panel2.add(button3);
        panel1.add(panel2,BorderLayout.CENTER);
        panel1.add(button4,BorderLayout.EAST);
        myFrame.add(panel1);

        // 大面板二
        Panel panel3 = new Panel(new BorderLayout());
        panel3.add(button5,BorderLayout.WEST);
        // 小面板
        Panel panel4 = new Panel(new GridLayout(2,2));
        panel4.add(button6);
        panel4.add(button7);
        panel4.add(button8);
        panel4.add(button9);
        panel3.add(panel4,BorderLayout.CENTER);
        panel3.add(button10,BorderLayout.EAST);
        myFrame.add(panel3);
    }
}
```



#### 小总结

1.  Frame是一个顶级的窗口
2.  panel无法单独显示，必须加入到某个容器中
3.  布局管理器
    -   流式
    -   东西南北中
    -   表格
4.  大小、定位、可见性、颜色，窗口关闭监听事件





### 事件监听

**说明**：当事件被触发了该干什么，例如，按钮被点击了，执行按钮绑定的事件里的代码

思路：创建一个事件类实现ActionListener类，在里面写的是触发事件后执行的代码，然后将它添加到按钮的事件中，当触发按钮的事件，那么它就会被调用

```java
public class ButtonClickEvent {
    public static void main(String[] args) {
        new ButtonClick();
    }
}
class ButtonClick {
    private Frame frame;
    // 初始化
    ButtonClick(){
        frame = new Frame("小智的GUI");
        frame.setBounds(300,300,500,500);
        frame.setVisible(true);
        Button button = new Button();
        button.addActionListener(new ClickEvent());
        frame.add(button);
        frame.pack();   // 自动填充
        windowClose();  // 关闭窗口
    }
    // 关闭窗口
    private void windowClose(){
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}
// 事件类
class ClickEvent implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        System.out.println("小智是个靓仔");
    }
}
```





### 输入框TestField监听

-   textField类单行输入框，不能进行换行
-   setEchoChar()方法设置它以什么形式展现
-   setText()设置文本内容

```java
public class TestFieldListener {
    public static void main(String[] args) {
        new FieldListent();
    }
}
class FieldListent{
    FieldListent(){
        // 封装好的类
        MyFrame myFrame = new MyFrame(300,300,500,500,null);
        TextField textField = new TextField();
        textField.setEchoChar('*');
        // 按下Enter键就会触发事件
        textField.addActionListener(new FiedListentEvent());
        myFrame.add(textField);
    }
}
// 事件类
class FiedListentEvent implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent e) {
        TextField field = (java.awt.TextField) e.getSource();
        System.out.println(field.getText());
        // 设置文本内容
        field.setText("");
    }
}
```





### 计算机的简易实现

oop原则：组合大于继承，优先使用组合

回顾组合、内部类的知识



![image-20210110140304752](D:\学习笔记\img\image-20210110140304752.png)

**代码实现**

```java
public class CalculatorDemo {
    public static void main(String[] args) {
        new Calculator();
    }
}
class Calculator extends Frame {
    TextField field1;
    TextField field2;
    TextField field3;
    Calculator() {
        setBounds(300,300,400,400);
        // 创建组件
        field1 = new TextField(20);   // 字符数
        field2 = new TextField(20);   // 字符数
        field3 = new TextField(20);   // 字符数
        Button button = new Button("=");
        button.addActionListener(new CalculatorEvent(this));
        Label label = new Label("+");
        // 添加组件
        add(field1);
        add(label);
        add(field2);
        add(button);
        add(field3);
        // 布局
        setLayout(new FlowLayout());
        setVisible(true);
        pack();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
}

// 事件
class CalculatorEvent implements ActionListener {
    private Calculator calculator;
    CalculatorEvent(Calculator calculator){
        this.calculator = calculator;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        // 将两个数相加显示在第三个文本框
        int n1 = Integer.parseInt(calculator.field1.getText());
        int n2 = Integer.parseInt(calculator.field2.getText());
        String sum = "" + (n1 + n2);
        calculator.field3.setText(sum);
        calculator.field1.setText("");
        calculator.field2.setText("");
    }
}
```

**结果显示**

![image-20210110142448245](D:\学习笔记\img\image-20210110142448245.png)

==**说明**：代码实现中我们可以看到，我们通过属性获取另一个类的属性，这种方式就是组合==



**代码优化	-	内部类**

```java
public class CalculatorDemo {
    public static void main(String[] args) {
        new Calculator();
    }
}
class Calculator extends Frame {
    TextField field1;
    TextField field2;
    TextField field3;
    Calculator() {
        setBounds(300,300,400,400);
        // 创建组件
        field1 = new TextField(20);   // 字符数
        field2 = new TextField(20);   // 字符数
        field3 = new TextField(20);   // 字符数
        Button button = new Button("=");
        button.addActionListener(new CalculatorEvent());
        Label label = new Label("+");
        // 添加组件
        add(field1);
        add(label);
        add(field2);
        add(button);
        add(field3);
        // 布局
        setLayout(new FlowLayout());

        setVisible(true);
        pack();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    // 事件
    public class CalculatorEvent implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int n1 = Integer.parseInt(field1.getText());
            int n2 = Integer.parseInt(field2.getText());
            String sum = "" + (n1 + n2);
            field3.setText(sum);
            field1.setText("");
            field2.setText("");
        }
    }
}
```

==**说明**：用内部类实现就可以不用创建对象也能调用外部类的方法和属性==





### 画笔（paint）

**说明**：重写paint方法，在方法里面进行画笔操作就可以了

**注意**：画笔它是不会自己恢复到原来的颜色的

```java
class PaintDemo extends Frame {
    PaintDemo(){
        setVisible(true);
        setBounds(300,300,400,400);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
    }
    @Override
    public void paint(Graphics g) {
        g.setColor(Color.CYAN);
        g.fillOval(50,50,40,30);
        g.setColor(Color.green);
        g.drawRect(90,90,40,40);
    }
}
```





### 鼠标监听

重写方法

-   mousePressed()按压鼠标事件
-   mouseReleased() 松开鼠标事件
-   mouseEntered()滑动鼠标事件
-   mouseExited() 失去焦点事件
-   mouseClicked() 鼠标单击事件



**注意**：鼠标的坐标是相对于当前窗口的值的，比如窗口大小是400x400,那么鼠标的x、y值最大不能超过400

**案例**：用鼠标监听事件模拟画图工具

**代码实现**

```java
public class TestMouseListener {
    public static void main(String[] args) {
        new MyFrame();
    }
}
// 窗体
class MyFrame extends Frame {
    private ArrayList points;
    MyFrame(){
        points = new ArrayList();
        setVisible(true);
        setBounds(300,300,400,400);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        });
        // 鼠标监听器，针对这个窗口
        this.addMouseListener(new MouseListenerEvent());
    }
    @Override
    public void paint(Graphics g) {
        // 将存在集合中的点拿出来
        Iterator iterator = points.iterator();
        while (iterator.hasNext()){
            Point point = (Point) iterator.next();
            g.setColor(Color.BLUE);
            g.fillOval(point.x,point.y,10,10);
        }
    }

    // 保存鼠标坐标到集合中的方法
    public void getMouse(Point point){
        // 添加到集合中
        points.add(point);
    }

    // 事件
    public class MouseListenerEvent extends MouseAdapter {
        @Override
        public void mouseClicked(MouseEvent e) {
            getMouse(new Point(e.getX(),e.getY()));
            // 调用一次就重画一次
            repaint();
        }
    }
}
```





### 窗口监听

```java
public class WindowlistenerTest {
    public static void main(String[] args) {
        new MyFrame2().info();
    }
}
class MyFrame2 extends Frame {
    public void info() {
        setVisible(true);
        setBounds(300, 300, 400, 400);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
            @Override
            // 再次得到焦点的时候就会触发
            public void windowActivated(WindowEvent e) {
                MyFrame2 source = (MyFrame2) e.getSource();
                source.setTitle("小智的窗口");
            }
        });
    }
}
```





### 键盘监听

```java
public class TestKeyListener {
    public static void main(String[] args) {
        new Myframe().info();
    }
}
class Myframe extends Frame {
    public void info(){
        setVisible(true);
        setBounds(300,300,500,500);
        addKeyListener(new KeyEven());
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(WindowConstants.DO_NOTHING_ON_CLOSE);
            }
        });
    }
    public class KeyEven extends KeyAdapter {
        @Override
        public void keyPressed(KeyEvent e) {
            // 得到按下键的值
            int keyCode = e.getKeyCode();
            if (keyCode == KeyEvent.VK_UP){
                System.out.println("我按下了上键");
            }
        }
    }
}
```







### 总结

①监听事件都是一样的，通过继承或实现对应的类，然后重写里面对应的方法就可以实现监听

②容器都是独立的个体，要将他们添加到Frame顶级窗口中才能显示出来

③source方法可以得到一个类的全部资源







## Swing

**说明**：它是AWT的子类，封装了AWT的许多功能，功能比AWT更加的丰富



### JFrame窗体

**补充**：SwingConstants接口中许多布局的常量

使用的方式和AWT的类似，前面多了个J，实际都是差不多的



**代码实现**

```java
public class JFrameTest {
    public static void main(String[] args) {
        new MyJFrameTest();
    }
}
class MyJFrameTest extends JFrame {
    public void info(){
        setVisible(true);
        setBounds(300,300,400,400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }
}
```



### Container容器

**说明**：如果直接设置窗体的颜色是不行的，要在Container容器里面设置才可以

```java
public class ContainerTest {
    public static void main(String[] args) {
        new MyJFrame().info();
    }
}
class MyJFrame extends JFrame {
    public void info(){
        this.setVisible(true);
        this.setBounds(300,300,400,400);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        // 获得一个容器
        Container container = this.getContentPane();
        container.setBackground(Color.CYAN);
    }
}
```



### 绝对定位

**使用**：setLayout()方法传null值就可以了

**what？**：这也是布局的一种，用坐标来定位，每个位置都是定死的

```java
public class LayoutTest {
    public static void main(String[] args) {
        new MyJFrame3().info();
    }
}
class MyJFrame3 extends JFrame {
    public void info(){
        setVisible(true);
        setBounds(300,300,400,400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(null);    // 设为绝对布局
        Button button = new Button("click");
        button.setBounds(30,30,40,40);
        add(button);
        button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                System.out.println("按钮被点击了");
            }
        });
    }
}
```





### 弹窗JDialog

JDialog继承Dialog

```java
public class TestJDialog {
    public static void main(String[] args) {
        new JDoalogFrame().info();
    }
}
class JDoalogFrame extends JFrame {
    public void info(){
        Container container = getContentPane();
        container.setLayout(null);
        setVisible(true);
        setBounds(300,300,400,400);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JButton jButton = new JButton("弹窗");
        jButton.setSize(300,300);
        container.add(jButton);
        jButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                new DialogEvent();
            }
        });
    }
}
// 弹窗类
class DialogEvent extends JDialog{
    public DialogEvent(){
        setVisible(true);
        setLayout(null);
        setBounds(300,300,400,400);
    }
}
```





### 标签

**说明**：可以在lable、button或者其他的容器上话图标

-   getSource()方法，获取到工程/目录下的所有资源



**代码实现**

**在标签或者按钮上画图标**

```java
public class IconDemo extends JFrame implements Icon {
    private int width;
    private int height;

    public IconDemo(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void init(){
        IconDemo iconDemo = new IconDemo(30,30);
        // 图标可以放在标签上也可以是按钮上
        JLabel jLabel = new JLabel("IconDemo",iconDemo,SwingConstants.CENTER);
        Container container = this.getContentPane();
        container.add(jLabel);
        this.setVisible(true);
        this.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.setBounds(300,300,400,400);
    }
    public IconDemo() {
    }
    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
        g.fillOval(x,y,width,height);
    }
    @Override
    public int getIconWidth() {
        return this.width;
    }
    @Override
    public int getIconHeight() {
        return this.height;
    }
    public static void main(String[] args) {
        new IconDemo().init();
    }
}
```



**画图片实现**

```java
public class ImageIconDemo extends JFrame {
    public void init(){
        JLabel jLabel = new JLabel("ImageIcon");
        // 得到图片资源
        URL url = ImageIconDemo.class.getResource("tp.jpg");
        // 创建ImageIcon对象
        ImageIcon imageIcon = new ImageIcon(url);
        // 设置标签的图标
        jLabel.setIcon(imageIcon);
        Container container = getContentPane();
        // 将标签添加到容器中
        container.add(jLabel);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(300,300,400,400);
    }
    public static void main(String[] args) {
        new ImageIconDemo().init();
    }
}
```







### 面板

当内容超过了当前窗口的最大值，那么它就会有两条滑轨可以上下左右滑动



**代码实现**

```java
public class JpanelTest extends JFrame {
    public JpanelTest(){
        Container container = getContentPane();
        container.setLayout(new GridLayout(2,3,10,10));// 后面两个参数是每个面板之间的间距
        JPanel jPanel = new JPanel(new GridLayout(1,2));
        jPanel.add(new JButton("1"));
        jPanel.add(new JButton("2"));
        jPanel.add(new JButton("3"));
        jPanel.add(new JButton("4"));
        container.add(jPanel);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(300,300,400,400);
    }
    public static void main(String[] args) {
        new JpanelTest();
    }
}
```



**Scroll面板和文本域**

可以滑动的面板，当内容超过了当前窗口的最大值就会有滑动效果

==注意：文本域其实自带滑动条==

```java
public class JScrollPanelTest extends JFrame {
    public JScrollPanelTest(){
        Container container = getContentPane();
        // 创建一个文本域对象
        TextArea textArea = new TextArea();
        textArea.setText("我是小智");
        JScrollPane jScrollPane = new JScrollPane(textArea);
        container.add(jScrollPane);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(300,300,400,400);
    }
    public static void main(String[] args) {
        new JScrollPanelTest();
    }
}
```





### 按钮

-   普通按钮 JButton

-   单选按钮（单选项）
-   复选按钮（多选项）



**代码实现**

-   **在按钮上放图标**

    ```java
    public class AddImageToButton extends JFrame {
        public AddImageToButton(){
            Container container = getContentPane();
            URL resource = AddImageToButton.class.getResource("tP.jpg");
            ImageIcon imageIcon = new ImageIcon(resource);
            JButton jButton = new JButton();
            jButton.setIcon(imageIcon);
            jButton.setToolTipText("图片按钮");
            jButton.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    System.out.println("我点击了按钮");
                }
            });
            container.add(jButton);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new AddImageToButton();
        }
    }
    
    ```

-   **单选按钮JRadioButton**

    说明：由于我们只能选择一个，所以我们可以将他们分组，在组中我们只能选择一个

    ```java
    public class ButtonTest extends JFrame {
        public void init(){
            Container container = getContentPane();
            JButton jButton = new JButton();
            JRadioButton radioButton1 = new JRadioButton("radioButton1");
            JRadioButton radioButton2 = new JRadioButton("radioButton2");
            JRadioButton radioButton3 = new JRadioButton("radioButton3");
            ButtonGroup buttonGroup = new ButtonGroup();
            buttonGroup.add(radioButton1);
            buttonGroup.add(radioButton2);
            buttonGroup.add(radioButton3);
            container.add(radioButton1,BorderLayout.NORTH);
            container.add(radioButton2,BorderLayout.CENTER);
            container.add(radioButton3,BorderLayout.SOUTH);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new ButtonTest().init();
        }
    }
    ```

-   **复选按钮**

    ```java
    public class ButtonTest2 extends JFrame {
        public void init(){
            Container container = getContentPane();
            JCheckBox jCheckBox = new JCheckBox("jCheckBox");
            JCheckBox jCheckBox2 = new JCheckBox("jCheckBox2");
            container.add(jCheckBox,BorderLayout.NORTH);
            container.add(jCheckBox2,BorderLayout.SOUTH);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new ButtonTest2().init();
        }
    }
    ```





### 列表

-   **下拉框**

    ```java
    public class JComboBoxTest extends JFrame{
        public void init(){
            Container container = getContentPane();
            JComboBox box = new JComboBox();
            box.addItem("帅哥");
            box.addItem("美女");
            box.addItem("人妖");
            container.add(box);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new JComboBoxTest().init();
        }
    }
    ```

-   **列表框**

    ```java
    public class JListTest extends JFrame {
        public void init(){
            Container container = getContentPane();
    //        String[] students = {"小智","大大智","坏人"};
            Vector students = new Vector();
            students.add("小智");
            students.add("大大智");
            students.add("坏人");
            students.add("好人");
            JList jList = new JList(students);
            container.add(jList);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new JListTest().init();
        }
    }
    ```

    ==**补充**：他们的获取值的方式都是一样的，通过监听事件来获取它当前的值或这改变当前的值==



### 文本框

-   文本框

    ```java
    public class TextTest1 extends JFrame {
        public TextTest1(){
            Container container = getContentPane();
            JTextField jTextField = new JTextField();
            jTextField.setText("欢迎来到小智联盟");
            container.add(jTextField);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new TextTest1();
        }
    }
    ```

-   密码框

    ```java
    public class TextTest2 extends JFrame {
        public TextTest2(){
            Container container = getContentPane();
            JPasswordField jPasswordField = new JPasswordField();
            // 可以设置它的展示
            jPasswordField.setEchoChar('*');
            container.add(jPasswordField);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new TextTest2();
        }
    }
    ```

-   文本域

    ```java
    public class TextTest3 extends JFrame {
        public TextTest3(){
            Container container = getContentPane();
            TextArea textArea = new TextArea();
            textArea.setText("欢迎来到德莱联盟");
            container.add(textArea);
            setVisible(true);
            setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            setBounds(300,300,400,400);
        }
        public static void main(String[] args) {
            new TextTest3();
        }
    }
    ```





### 定时器Time

**说明**：在一定的时间内执行规定的次数的方法，它执行的是

==导包的时候注意是Swing包下的==

调用start()方法开启定时任务







------





# JDBC

## 一、JDBC是什么

**全拼**：Java Database Connecor

java语言连接数据库技术
**实现**:Java代码操作数据库
**实现方式**：java提供接口，由各大数据库厂商来写实现类，因为每个厂商的数据库底层实现都是不一样的,这也叫接口驱动方式

**补充**：C#和Java的方式是正好相反，C#是自己写好各大数据库的连接代码

**异常**：MySQLSyntaxErrorException表示sql语法错误



## 二、JDBC的使用

### 1、JDBC相关的API

四大接口和一个类

-   DriverManager				驱动管理类
-   Connection				数据库连接对象
-   Statement					操作sql语句并携带返回
-   PreparedStatement		操作sql语句并携带返回
-   Resultset					结果集——>只有查询的时候才会有



### 2、使用

#### 2.1 直接添加数据的方式

①导入jar包
②编写代码连接数据库

1.  **加载驱动**

    `Class.forname("com.mysql.jdbc.Driver");`

    说明：返回的对象不用我们操作，DriverManager底层会操作这个对象

2.  **获取Connection对象，导包的时候注意是java.sql包下的**

    通过DriverManager.getConnection("")返回Connection对象

    字符串里面放：jdbc:mysql://主机名:端口号/数据库名?user=用户名&password=密码&使用Unicode=true或false&字符集=编码格式

    **例子**：`Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/myemployees?user=root&password=root&useUnicode=true&characterEncoding=utf-8");`

    **注意**：如果没有密码可以不写，但是password=要写

3.  **编写SQL语句**

    String sql = "语句";		

    **注意**：用java操作数据库的时候，sql语句必须是对的，所以可以先去SQLyong中写好语句再复制进来，复制的时候不要把分号也复制了

4.  **创建Statement st = Connection对象.createStatement();**

    注意：Stratment也是java.sql包下的

5.  **通过 ResultSet rs = st.executeQuery(sql);得到一个结果集**

6.  **遍历结果集**	--> 这个时候才是返回数据的时候，返回的结果集是索引

    实现：while配合ResultSet对象的next()方法，和iterator的用法是类似的，

    也是先进行判断，然后再进行读取数据，

    然后通过ResultSet对象中的get数据类型("列名")得到相应的数据

7.  **关闭资源**

    rs.close();		关闭结果集

    st.close();		关闭Statement

    con.close();		关闭连接



**代码实现**

```java
public static void main(String[] args) {
        //第一个程序
        try {
            // 1.加载驱动
            Class.forName("com.mysql.jdbc.Driver");
            // 2.获得连接对象
            Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db4?user=root&password=root&useUnicode=true&characterEncoding=utf-8");
            // 3.编写sql语句
            String sql = "SELECT id,ename,salary FROM emp";

            // 4.执行sql语句
            Statement st = con.createStatement();

            // 5.执行sql语句，返回一个结果集
            ResultSet rs = st.executeQuery(sql);

            // 6.遍历rs结果集
            // while配合ResultSet对象的next()方法，和iterator的用法是类似的，
            //				也是先进行判断，然后再进行读取数据，
            //				然后通过ResultSet对象中的get数据类型("列名")得到相应的数据
            System.out.println("id\t\t名字\t\t工资");
            while (rs.next()){
                Integer id = rs.getInt("id");
                String name = rs.getString("ename");
                Double salary = rs.getDouble("salary");

                System.out.println(id + "\t" + name + "\t" + salary);
            }
            // 7.关闭资源
            rs.close();
            st.close();
            con.close();
        } catch (Exception throwables) {
            throwables.printStackTrace();
        }
    }
}
```



#### 2.2 动态传入数据的式

这种方式更加灵活，第1种方式只能添加一条固定的数据

说明：①sql语句要用"+要添加的变量+"这样的形式将变量包起来
		   ②修改数据和删除数据是没有结果集返回的
				用st.executeUpdate(String str)方法可以返回一个int类型的值
				如果这个值等于1，就是添加成功了，如果是其他数据那就是添加失败
			③关资源的时候，不用关结果的那个资源，因为它返回的是一个值



**代码实现**

```java
public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    System.out.println("输入id：");
    int id = sc.nextInt();
    System.out.println("输入姓名：");
    String name = sc.next();
    System.out.println("输入性别");
    String gender = sc.next();
    System.out.println("输入工资：");
    String salary = sc.next();
    System.out.println("输入加入日期：");
    String date = sc.next();
    System.out.println("输入部门id：");
    int did = sc.nextInt();
    try {
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db3?user=root&password=root&useUnicode=true&characterEncoding=utf-8");
        String sql = "INSERT INTO emp VALUES("+id+",'"+name+"','"+gender+"',"+salary+",'"+date+"',"+did+")";
        Statement st = con.createStatement();
        int index = st.executeUpdate(sql);
        if (index == 1){
            System.out.println("添加成功");
        }else{
            System.out.println("添加失败");
        }
        st.close();
        con.close();
    } catch (Exception throwables) {
        throwables.printStackTrace();
    }
}
```



#### 2.3 通过PreparedStatement对象用控制台的方式

**说明**：PreparedStatement(	PS)是Statement(St)接口的子接口
==它们两者的区别？==
		①PS在写sql语句的时候可以用占位符 ? 代替变量，然后再来填充占位符的值，St是"+变量+"
		②PS在执行之前，有一个预编译的动作，使得SQL语句执行效率搞，St不是
		③PS在创建对象的时候就需要传一个sql语句的参数，St是创建之后调用方法的时候再传的
	

**填充数据**：set类型(占位符的位置,变量名)
			从左到右，索引从1开始
		
**注意**：占位符 ? 不要用双引号引起来，程序会以为是个字符串

**结论**：以后一律使用PS



**代码实现**

```java
public static void main(String[] args) {
    Scanner sc = new Scanner(System.in);
    System.out.println("输入id：");
    int id = sc.nextInt();
    System.out.println("输入姓名：");
    String name = sc.next();
    System.out.println("输入性别");
    String gender = sc.next();
    System.out.println("输入工资：");
    Double salary = sc.nextDouble();
    try {
        Class.forName("com.mysql.jdbc.Driver");
        Connection con = DriverManager.getConnection("jdbc:mysql://localhost:3306/db3?user=root&password=root&useUnicode=true&characterEncoding=utf-8");
        String sql = "INSERT INTO emps VALUES (2,?,?,?);";
        PreparedStatement ps = con.prepareStatement(sql);   // 创建对象的时候通过传参传入sql
        ps.setString(1,name);       // 填充数据
        ps.setString(2,gender);
        ps.setDouble(3,salary);
        long index = ps.executeLargeUpdate();
        if (index == 1){
            System.out.println("添加成功");
        }else{
            System.out.println("添加失败");
        }
        ps.close();
        con.close();
    } catch (Exception throwables) {
        throwables.printStackTrace();
    }
}
```

**注意**：
	①每次增、删、改的时候要记得	ps.executeUpdate();	更新一下
	
**补充**：PrepareStatement和Statement的作用是执行sql脚本





## 三、JDBCUtils的使用

就是封装成Utils类，以后都用这种方式



### 1.JDBCUtils类

将获取连接对象和关闭资源的功能放入JDBCUtils类中
说明：JDBCUtils类是一个自定义名字叫这个得类



**代码实现**

-   JDBCUtils类:

    ```java
    // 获取连接
    public static Connection getConnection() throws SQLException, ClassNotFoundException {
        Connection con = null;
        Class.forName("com.mysql.jdbc.Driver");
        con = DriverManager.getConnection(
            "jdbc:mysql://localhost:3306/db3?																user=root&password=root&useUnicode=true&characterEncoding=utf-8");
        return con;
    }
    
    // 关闭资源
    public static void getClose(Connection con, Statement st, ResultSet rs) {
        // 判断，当没结果集的时候不用关
        try {
            if (rs != null) {
                rs.close();
            }
            if (con != null){
                con.close();
            }
            if (st != null){
                st.close();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
    ```

-   TestJDBCUtils类

    ```java
    @Test
    public void Test01(){
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        try {
            con = JDBCUtils.getConnection();
            String sql = "select * from user";
            ps = con.prepareStatement(sql);
            rs = ps.executeQuery();
            while(rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int age = rs.getInt("age");
                System.out.println(id + "\t" + name + "\t" + age);
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            JDBCUtils.getClose(con, ps, rs);
        }
    
    }
    ```





### 2.配件文件的方式

==这种方式常用==

原理：用IO流读取配置文件jdbc.properties，pproperties是map的实现类，这个文件以键值对的形式保存的。

**配置文件方式的好处**：因为java代码修改之后要进行重新编译，而配置文件则不用进行编译，更方便我们去修改里面的内容



**代码实现**

```java
// 获取连接
public static Connection getConnection() throws SQLException, ClassNotFoundException, IOException {
    // 读取配置文件
    Properties pt = new Properties();
    pt.load(new FileInputStream(new File("jdbc.properties")));
    Connection con = null;
    String driver = pt.getProperty("driver");
    String url = pt.getProperty("url");
    String userName = pt.getProperty("username");
    String password = pt.getProperty("password");
    Class.forName(driver);
    con = DriverManager.getConnection(url,userName,password);
    return con;
}
```





## 四、连接池

### 4.1	Druid（德鲁伊）连接池

**介绍**：阿里巴巴旗下的产品
**产生的原因**：一直连着数据库不行的，占资源，多次连接也不行，所以想到创建一个连接池来产生链接，这个链接的两端分别是数据库和程序，当程序不连的时候，链接进入等待状态，当程序再次连的时候就响应，这样处理就不会断开数据库连接又连上

<img src="D:\学习笔记\img\image-20220315011527240.png" alt="image-20220315011527240" style="zoom:70%;" />

**注意**：当链接没有程序访问的时候，到达等待时间之后就会断开和数据库的连接，可以自己设置这个等待时间的时长，还有一个问题就是它会不会全部断开呢？不会



1.  纯代码方式

    ```java
    public static Connection getConn() throws ClassNotFoundException, SQLException {
        //必须设置的内容
        DruidDataSource ds = new DruidDataSource();//创建数据源对象
        //德鲁伊数据源设置属性
        ds.setUrl("jdbc:mysql://localhost:3306/db3");
        ds.setUsername("root");
        ds.setPassword("root");
        ds.setDriverClassName("com.mysql.jdbc.Driver");
    
        //择设置
        ds.setInitialSize(10);		//默认值0，连接数量
        ds.setMaxActive(20);		//默认值8，最大连接数
        ds.setMinIdle(1);			//默认值0，最小连接数
    
        // 下面的代码可写可不写，不写不会影响使用数据库
        // masIdle是Durid方便DBCP用户迁移而增加的，maxIdle是一个混乱的概念。连接池只应该有													maxpoolSize
        //        	ds.setMaxIdle(5);
        //
        //        	// 连接获取的最大等待时间，单位毫秒。配置了maxWait之后，缺省启用公平锁，并发效率会所下载
        //        	ds.setMaxWait(1000);
        //
        //        	// 配置时隔多久一次检测，检测需要关闭的空闲连接，单位毫秒
        //        	ds.setTimeBetweenEvictionRunsMillis(60000);
        //
        //        	// 配置一个连接池最小生存时间，单位毫秒
        //        	ds.setMinEvictableIdleTimeMillis(300000);
        //
        //        	// 是否缓存preparedStatement，也就是PSCache。PSCache对支持游标的数据库性
        //        	ds.setPoolPreparedStatements(true);
        //
        //        	ds.setMaxPoolPreparedStatementPerConnectionSize(10);
        //
        //        	ds.setFilters("stat,wall");
    
    
        Connection conn = ds.getConnection();
        System.out.println(conn);
        return conn;
    }
    ```

    

2.  配置文件方式

    导入druid.properties配置文件
    配置文件中的 rewriteBatchedStatements=true：是否进行批处理

    ```java
    public static Connection getConn() throws Exception {
        Properties pro = new Properties();	//键值对集合对象
        pro.load(JDBCUtilsDruid2.class.getClassLoader().getResourceAsStream("druid.properties"));
        DataSource ds = DruidDataSourceFactory.createDataSource(pro);
        Connection conn = ds.getConnection();
        return conn;
    }
    ```

    **说明**：上面的方式的关资源方式是一样的，所以没有把代码写上





## 五、JDBC事物处理

转账，中间出现了异常，转钱的那个人账户钱少了，但是收到人的钱没有增加，
这种情况就是没有进行事物处理的状态，所以开启事物就可以解决问题了

1.  con.setAutoCommit(false);		设置自动提交的关闭

    自动提交关闭就是开启提交事物了

2.  con.commit()：提交事物	->		在更新的后面

3.  在catch里面放入con.rollback()进行回滚事物，加了事物之后出现异常时就会进行回滚。

**代码实现**

```java
@Test
public void test() {
    Connection con = null;
    try {
        con = JDBCUtilsDruid2.getConn();
        // 解决办法，开启事物
        con.setAutoCommit(false);
        String sql = "UPDATE emps SET salary=salary-500 WHERE id = 1";
        PreparedStatement ps = con.prepareStatement(sql);
        ps.executeUpdate();

        //System.out.println(1/0);  //在中间加入一个异常

        PreparedStatement ps2 = con.prepareStatement(sql);
        String sql2 = "UPDATE emps SET salary=salary+500 WHERE id = 2";
        ps2.executeUpdate(sql2);

        con.commit();       // 提交事物
    } catch (Exception e) {
        e.printStackTrace();
        try {
            con.rollback();     // 回滚事物
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }
}
```







## 六、JDBC-common-dbutils

**插件的使用**

### 1、commons-dbutils-1.3的使用

​		Apache提供的一个对JDBC进行简单封装的开源工具类库，
​		它能够简化JDBC应用程序的开发，同时也不会影响程序的性能。



### 2、为什么需要Dbutils ？

​	在使用Dbutils 之前，我们Dao层使用的技术是JDBC，那么分析一下JDBC的弊端：
​	①数据库链接对象、sql语句操作对象，封装结果集对象，这三个对象会重复定义
​	②封装数据的代码重复，而且操作复杂，代码量大
​	③释放资源的代码重复



### 3、主要的类

**QueryRunner**：SQL语句的操作对象，可以设置查询结果集的封装策略，线程安全。

构造方法：

1.  QueryRunner()：创建一个与数据库无关的QueryRunner对象，后期再操作数据库的会后，需要手动给一个Connection对象，它可以手动控制事务。 

    Connection.setAutoCommit(false); 设置手动管理事务 

    Connection.commit(); 提交事务 

2.  QueryRunner(DataSource ds)：创建一个与数据库关联的queryRunner对象，后期再操作数据库的时候，不需要Connection对象，自动管理事务。 

    DataSource：数据库连接池对象。

    

**构造函数与增删改查方法的组合**： 

-   QueryRunner() 

    ​	update(Connection conn, String sql, Object... params)

    ​	query(Connection conn, String sql,ResultSetHandler rsh, Object... params) 			

-   QueryRunner(DataSource ds) 

    ​	update(String sql, Object... params)

    ​	query(String sql, ResultSetHandler rsh, Object... params)





### 4、ResultSetHandle的实现类

**ResultSetHandle**：封装数据的策略对象------将封装结果集中的数据，转换到另一个对象
**策略**：封装数据到对象的方式（示例：将数据库保存在User、保存到数组、保存到集合）
**方法介绍**：handle（ResultSet rs）

DBUtils给我们提供了10个ResultSetHandler实现类，分别是：
	①ArrayHandler： 将查询结果的第一行数据，保存到Object数组中
	②ArrayListHandler 将查询的结果，每一行先封装到Object数组中，然后将数据存入List集合
	③BeanHandler 将查询结果的一行数据，封装到user对象
	④BeanListHandler 将查询结果的每一行封装到user对象，然后再存入List集合
	⑤ColumnListHandler 查询指定字段所有的值封装到List集合中
	⑥MapHandler 将查询结果的第一行数据封装到map结合
	⑦MapListHandler 将查询结果的每一行封装到map集合，再将map集合存入List集合
	⑧BeanMapHandler 将查询结果的每一行数据，封装到User对象，再存入map集合中
	⑨KeyedHandler 将查询的结果的每一行数据，封装到map1，然后将map1集合（有多个）存入map2集合（只有一个）
	⑩ScalarHandler 查询指定对象的指定字段的值或者是查询封装类似count、avg、max、min、sum…函数的执行结果
以上10个ResultSetHandler实现类，常用的BeanHandler、BeanListHandler和ScalarHandler



**注意**：我们在使用BeanListHandler时如果表中命名是带下划线的，那么我们就要开启驼峰命名来防止属性值为null

QueryRunner runner=new QueryRunner(druidDataSource); //开启驼峰映射 
BeanProcessor bean = new GenerousBeanProcessor(); 
RowProcessor processor = new BasicRowProcessor(bean); String sql=""; 
//此处Area传入列表需要的泛型 
List<Area> list=runner.query(sql,new BeanListHandler<Area>(Area.class,processor))



**代码实现**

```java
@Test
public void test1() {
    try {
        // 查询操作
        QueryRunner qr = new QueryRunner(JDBCUtilsDruid2.getDS());
        String sql = "SELECT ?,?,?,? FROM emps where id = ?";
        Student stu = qr.query(sql, new BeanHandler<>(Student.class), "id","name","gender","salary","id=1");

        //            for (Student stu:list) {
        //                System.out.println(stu);
        //            }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

@Test
public void test2() {
    // 增删改操作

    String sql = "UPDATE emps SET NAME=?,salary=? WHERE id = ?";
    try {
        QueryRunner qr = new QueryRunner(JDBCUtilsDruid2.getDS());
        qr.update(sql, "花花",26344, 5);
    } catch (Exception throwables) {
        throwables.printStackTrace();
    }
}
```





## 七、数据库类型对应的Java类型

![image-20220315013228383](D:\学习笔记\img\image-20220315013228383.png)







------







# Java常用类

## Objects类

![image-20220315174056075](D:\学习笔记\img\image-20220315174056075.png)

![image-20220315174103370](D:\学习笔记\img\image-20220315174103370.png)





## 大数类

### 1、BigInteger

Integer类作为int的包装类，能存储的最大整型值为231-1，Long类也是限的，最大为263-1。如果要表示再大的整数，不管是基本数据类型还是他们的包装类都无能为力，更不用说进行运算了。

**代码实现**

```java
int maxValue = Integer.MAX_VALUE;
System.out.println(maxValue);//2147483647
maxValue += 1;
System.out.println(maxValue);
System.out.println("---------------------------------------------");
BigInteger a = new BigInteger(String.valueOf(2147483647));
BigInteger b = new BigInteger(String.valueOf(1));
BigInteger sum = a.add(b);
System.out.println(sum);
```



### 2、 BigDecimal

一般的Float类和Double类可以用来做科学计算或工程计算，但在商业计算中，要求数字精度比较高，故用到java.math.BigDecimal类。

**方法**：

-   public BigDecimal add(BigDecimal value);//加法
-   public BigDecimal subtract(BigDecimal value); //减法 
-   public BigDecimal multiply(BigDecimal value); //乘法
-   public BigDecimal divide(BigDecimal value); //除法



**代码实现**

```java
System.out.println(1 - 0.41);
System.out.println("-----------------------------------");

BigDecimal a = new BigDecimal(String.valueOf(1));
BigDecimal b = new BigDecimal(String.valueOf(0.41));
BigDecimal subtract = a.subtract(b);
System.out.println(subtract);
```









## 时间类

### Date类

#### 1 java.util.Date

两个构造器：

-   new Date(): 获取当前
-   new Date(long date): 获取毫秒数对应的时间

两个方法：

-   toString() : 输出时间的内容
-   getTime() : 获取时间对应的毫秒数	 



#### 2 java.sql.Date

一个构造器：

-   new Date(long date); 获取毫秒数对应的日期 		

两个方法：

-   toString() : 输出日期的内容
-   getTime() : 获取日期对应的毫秒数

​			

#### 3 如何将java.util.Date转换成java.sql.Date

java.util.Date date = new java.util.Date();
(java.sql.Date)date;





### LocalDate类

![image-20220315164410150](D:\学习笔记\img\image-20220315164410150.png)



 **代码实现一个日历**

```java
@Test
public void Test06(){
    LocalDate date = LocalDate.now();
    int month = date.getMonthValue();
    int today = date.getDayOfMonth();

    date = date.minusDays(today - 1); // set to start of month
    DayOfWeek weekday = date.getDayOfWeek();
    int value = weekday.getValue(); // 1 = Monday, . . . , 7 = Sunday

    System.out.println("Mon Tue Wed Thu Fri Sat Sun");
    for (int i = 1; i < value; i++)
        System.out.print("    ");
    while (date.getMonthValue() == month)
    {
        System.out.printf("%3d", date.getDayOfMonth());
        if (date.getDayOfMonth() == today)
            System.out.print("*");
        else
            System.out.print(" ");
        date = date.plusDays(1);
        if (date.getDayOfWeek().getValue() == 1) System.out.println();
    }
    if (date.getDayOfWeek().getValue() != 1) System.out.println();
}
```



### LocalDateTime





### Instant

![image-20220315165449087](D:\学习笔记\img\image-20220315165449087.png)



### SimpleDateFormat

用来格式化日期和时间

-   format(Date date) : 将Date转成字符串
-   parse(String time) : 将字符串转成Date的格式 - 注意：字符串中时间的格式和当前对象设置的时间格式必须一致



**代码实现**

```java
Date date = new Date();
		
SimpleDateFormat sdf = new SimpleDateFormat();
//将时间转成字符串
String format = sdf.format(date);//19-11-8 下午4:46
System.out.println(format);

System.out.println("---------------------------------------");

SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
System.out.println(sdf2.format(date));//2019-11-08T16:47:39.584+0800

System.out.println("-------------------------- -----------------------------------");

SimpleDateFormat sdf3 = new SimpleDateFormat("h:mm a");
System.out.println(sdf3.format(date));//4:50 下午

System.out.println("--------------------------------- -----------------");

SimpleDateFormat sdf4 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
System.out.println(sdf4.format(date));//2019-11-08 16:50:02
//将字符串转成时间
Date date2 = sdf4.parse("2019-11-08 16:50:02");
//注意：SimpleDateFormat设置的时间格式和字符串内容的时间格式必须一致否则转换失败
//		date2 = sdf4.parse("4:50 下午");
System.out.println(date2);
```



### DateTimeFormatter

用来格式化日期和时间

```java
LocalDateTime localDateTime = LocalDateTime.now();

//预定义的标准格式
DateTimeFormatter dtf = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
//本地化相关的格式
dtf = DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL);
//自定义
dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss");

//将日期时间转成字符串
String str = dtf.format(localDateTime);
System.out.println(str);

//将字符串转化成日期和时间
TemporalAccessor parse = dtf.parse("2019-11-09 10:35:22");
System.out.println(parse);
```





## Math类

Math : 提供了一系列静态方法用于科学计算。其方法的参数和返回值类型一般为double型。

-   abs     绝对值
-   acos,asin,atan,cos,sin,tan  角函数
-   sqrt     平方根
-   pow(double a,doble b)     a的b次幂
-   log    自然对数
-   exp    e为底指数
-   max(double a,double b)
-   min(double a,double b)
-   random()      返回0.0到1.0的随机数
-   long round(double a)     double型数据a转换为long型（四舍五入
-   toDegrees(double angrad)     弧度—>角度
-   toRadians(double angdeg)     角度—>弧度





## System类

- **native long currentTimeMillis()**

    该方法的作用是返回当前的计算机时间，时间的表达格式为当前计算机时间和GMT时间(格林威治时间)1970年1月1号0时0分0秒所差的毫秒数。

- getenv() || getenv(name)

    获取环境变量

- *getProperties*()

    获取 JVM 环境变量





## String类

### 1、说明

-   String类被final所修饰，该类不能被继承

-   String实现了Serializable接口 ： 该类的对象可以被序列化。

    （序列化：可以将对象写到磁盘上，也可以将不同进程间的对象进行传递

-   String实现了Comparable接口 ： 可以用来比较内容（往往通过该接口实现排序的功能

-   String(StringBuffer,StringBuilder)实现了CharSequence接口 ：实现该接口就可以获取字符串的长度，获取指定索引位置上的字符等。

-   String底层其是一个char[]而且被final所修饰。（String是一个不可变的字符序列）



### 2、String类的特点

-   **String是不可变的字符序列**：

    无论是给String重新赋值，字符串拼接，通过方法修改内容 原字符串都不会发生改变只会创建一个新的String的实例。

-   **字符串拼接**：

    在字符串拼接时，如果变量参与拼接那么就会调用StringBuilder中的toString方法

    new一个String的对象并返回。

**代码实现**

```java
String s = "hellojava";
String s2 = "hello" + "java";
String s3 = "hello";
String s4 = "java";
String s5 = s3 + "java";
String s6 = "hello" + s4;
String s7 = s3 + s4;

//intern() : 首先去常量找查找是否和字符串结果匹配的内容如果直接返回。没创建新的。
String s8 = (s3 + s4).intern();
System.out.println(s == s2);//true
System.out.println(s == s5);//false
System.out.println(s == s6);//false
System.out.println(s == s7);//false
System.out.println(s5 == s6);//false
System.out.println(s == s8);//true
```



### 3、内存的存储结构

1.  String s = "abc"; //在常量池中会创建一个"abc"

2.  String  s =  new String("abc");//在堆中会创建一个对象，在常量池中会创建一个"abc"

    注意 ：常量池没有重复的内容。

![image-20220315170149358](D:\学习笔记\img\image-20220315170149358.png)



### 4、常用方法

-   **int length()**：

    返回字符串的长度： return value.length

-   **char charAt(int index)**： 

    返回某索引处的字符return value[index]

-   **boolean isEmpty()**：

    判断是否是空字符串：return value.length == 0

-   **String toLowerCase()**：

    使用默认语言环境，将 String 中的所字符转换为小写

-   **String toUpperCase()**：

    使用默认语言环境，将 String 中的所字符转换为大写

-   **String trim()**：

    返回字符串的副本，忽略前导空白和尾部空白

-   **boolean equals(Object obj)**：

    比较字符串的内容是否相同

-   **boolean equalsIgnoreCase(String anotherString)**：

    与equals方法类似，忽略大小写

-   **String concat(String str)**：

    将指定字符串连接到此字符串的结尾。 等价于用“+”

-   **String substring(int beginIndex)**：

    返回一个新的字符串，它是此字符串的从beginIndex开始截取到最后的一个子字符串。 

-   **String substring(int beginIndex, int endIndex)** ：

    返回一个新字符串，它是此字符串从beginIndex开始截取到endIndex(不包含)的一个子字符串。 

-   **boolean contains(CharSequence s)**：

    当且仅当此字符串包含指定的 char 值序列时，返回 true

-   **int indexOf(String str)**：

    返回指定子字符串在此字符串中第一次出现处的索引 

-   **int indexOf(String str, int fromIndex)**：

    返回指定子字符串在此字符串中第一次出现处的索引，从指定的索引开始 

-   **int lastIndexOf(String str)**：

    返回指定子字符串在此字符串中最右边出现处的索引 

-   **int lastIndexOf(String str, int fromIndex)**：

    返回指定子字符串在此字符串中最后一次出现处的索引，从指定的索引开始反向搜索 

    注：indexOf和lastIndexOf方法如果未找到都是返回-1

-   **boolean endsWith(String suffix)**：

    测试此字符串是否以指定的后缀结束 

-   **boolean startsWith(String prefix)**：

    测试此字符串是否以指定的前缀开始 

-   **boolean startsWith(String prefix, int toffset)**：

    测试此字符串从指定索引开始的子字符串是否以指定前缀开始

-   **String replace(char oldChar, char newChar)**：

    返回一个新的字符串，它是通过用 newChar 替换此字符串中出现的所 oldChar 得到的。 

-   **String replace(CharSequence target, CharSequence replacement)**：

    使用指定的字面值替换序列替换此字符串所匹配字面值目标序列的子字符串。 

-   **String replaceAll(String regex, String replacement)**：

    使用给定的 replacement 替换此字符串所匹配给定的正则表达式的子字符串。 

-   **String replaceFirst(String regex, String replacement)**：

    使用给定的 replacement 替换此字符串匹配给定的正则表达式的第一个子字符串。 

-   **String[] split(String regex)**：

    根据给定的匹配拆分此字符串。 

-   **Instream condePoints()**：

    将这个字符串的码点作为一个流返回。调用toArray将他们放在一个数组中

-   **String join(CharSequence delimiter, CharSequence...elements)**：

    返回一个新字符串，用给定的定界符连接所有元素

-   **String strip()**：

    返回一个新字符串。这个字符串将删除原始字符串头部和尾部空格 ==11==

-   **boolean isBlank()**：

    如果字符串为空或者由空格组成，返回true ==11==

-   **String repeat(int count)**：

    返回一个字符串，将当前字符串重复count次 ==11==











## Arrays类

![image-20220315173714115](D:\学习笔记\img\image-20220315173714115.png)

![image-20220315173721007](D:\学习笔记\img\image-20220315173721007.png)







------







# Java8新特性

## Lambda表达式

lambda是一个匿名函数，可以把它理解成匿名内部类的加强，使语法更加的简洁

“->” lambda操作符或者箭头操作符

```java
public class LambdaTest {

    @Test
    public void Test1() {
        // 消费型接口
        Consumer<String> c = System.out::println;
        c.accept("我是小智");
    }

    @Test
    public void Test2() {
        // 供给型接口
        Supplier<String> s = () -> {
            return "小智";
        };
        System.out.println(s.get());
    }

    @Test
    public void Test3() {
        // 函数型接口
        Function<Integer, String> f = x -> "小智" + 2;
        System.out.println(f.apply(5));
    }

    @Test
    public void Test4(){
        // 判断型接口
        Predicate<Integer> p = x -> x > 1?true:false;
        System.out.println(p.test(5));
    }
}
```



## 函数式接口

只包含一个**抽象方法**的接口，就称为是函数式接口，我们可以用**@Functionallnterface**注解来检验这个接口是不是函数式接口

![image-20210314155309007](D:\学习笔记\img\image-20210314155309007.png)

**其他接口**

![image-20210314155319648](D:\学习笔记\img\image-20210314155319648.png)

## 方法的引用

```java
// 方法的引用
@Test
public void Test5(){
    Comparator<Integer> c = Integer::compare;
    System.out.println(c.compare(5, 4));
}

// 对象的引用
@Test
public void Test6(){
    Function<Integer,MyClass> f = MyClass::new;
    System.out.println(f.apply(5).getAge());
}
class MyClass{
    private Integer age;

    public MyClass(Integer age) {
        this.age = age;
    }

    public Integer getAge() {
        return age;
    }
}

// 方法的引用
@Test
public void Test7(){
    BiPredicate<String ,String> b = String::equals;
    System.out.println(b.test("小智", "小智"));
}

// 数组的引用
@Test
public void Test8(){
    Function<Integer,Integer[]> f = Integer[]::new;
    System.out.println(f.apply(5).length);
}
```

==**总结**：参数必须是相同的类型和数量的才能使用方法的引用，左边是类型，右边是方法，这就是方法的引用==





## Stream流

### 1、概述

主要对数据进行过滤，拿到我们需要的数据。

因为从数据库获取数据是到Java层来进行数据处理的，所以stream就是帮我们做数据处理的

stream流简洁、高效、强大。

Stream流它是链式编程

**流程**：
		①创建Stream对象
		②中间操作 --> 条件检索
		③终止操作(终端操作)



### 2、Stream流的创建

```java
@Test
public void Test1() {
    // 方式一：从集合中获取流
    Collection list = new ArrayList();
    list.add("西瓜");
    list.add("苹果");
    list.add("香蕉");
    Stream stream1 = list.stream();  // 正向流
    stream1.forEach(System.out::println);
}

@Test
public void Test2(){
    // 方式二：从数据中获取流
    Integer[] integers = {1,5,10,2,9};
    Stream<Integer> stream = Arrays.stream(integers);
    stream.forEach(System.out::println);
}

@Test
public void Test3(){
    // 方式三：从散列数据中获取流
    Stream stream = Stream.of("小智", 2, 5, 6);
    stream.forEach(System.out::println);
}

@Test
public void Test4(){
    // 方式四：获取无限流
    Stream.generate(Math::random).forEach(System.out::println);
}

@Test
public void test05(){
    // 方式五：从文件流中获取 -> 获取的每一个元素对应文件中的每一行
    File file = new File("E:\\Work\\IDEAWorkspace\\ex-demo-test\\easy_demo\\src\\main\\java\\com\\xiaozhi\\idea\\debug\\com\\xiaozhi\\java\\lambda\\test.txt");
    try {
        Stream<String> stream = Files.lines(file.toPath());
        stream.forEach(System.out::println);
    } catch (IOException e) {
        throw new RuntimeException(e);
    }
}

// 方式六：创建一个空的流
Stream<Object> empty = Stream.empty();

// 方式七：生成一个有限序列，例如：0、1、2、3、4......
/*
	iterate(final T seed, final UnaryOperator<T> f)
		seed：初始值
		UnaryOperator<T>：会反复的将该函数应用到之前的结果
	注意：不加任何条件的话它会一直生成下去，也就是无限流，所以要给定条件让它结束
*/
// 下面就是一个无限流
Stream<BigInteger> iterate = Stream.iterate(BigInteger.ZERO, n -> n.add(BigInteger.ONE));
```





### 3、中间操作

```java
public class StreamCenter {
    private Stream stream;

    {
        // 从集合中获取流
        Collection list = new ArrayList();
        list.add("西瓜");
        list.add("苹果");
        list.add("香蕉");
        list.add("火龙果");
        list.add("香蕉");
        list.add("葡萄");
        stream = list.stream();
    }

    @Test
    public void Test1() {
        // 过滤筛选
        // 过滤掉苹果，接收一个判断器，会将为true的数据记录下来，false的记录会被过滤掉
        stream.filter(x -> x.equals("苹果") ? false : true).forEach(System.out::println);
    }

    @Test
    public void Test2() {
        // 过滤重复元素
        stream.distinct().forEach(System.out::println);
    }

    @Test
    public void Test3() {
        // 截断流
        stream.limit(3).forEach(System.out::println);
    }

    @Test
    public void Test4() {
        // 跳过流
        stream.skip(4).forEach(System.out::println);
    }

    @Test
    public void Test5() {
        // 映射,比如将String类型的转换成Integer类型
        // 接收一个参数，然后返回值，我的理解来说就是一个值映射成其他值
        stream.map(x -> "水果：" + x).forEach(System.out::println);
        /*
            结果显示
                水果：西瓜
                水果：苹果
                水果：香蕉
         */
    }

    public static Stream stringToArray(String str) {
        char[] chars = str.toCharArray();
        return Stream.of(chars);
    }

    @Test
    public void test06() {
        // flatMap -> 和map一样的功能，但是flatMap会将多个流合成一个流
        // 场景：将字符串转换为数组
        Stream<String> stringStream = Stream.of("adfasd", "sadfsd", "sfsadf");
        Stream<String> stringStream2 = Stream.of("adfasd", "sadfsd", "sfsadf");
        // 这种方式的结果是转换成三个流
        Stream<Stream> streamStream = stringStream.map(x -> stringToArray(x));
        // 使用flatMap它就会将多个流合成一个流输出
        Stream stream = stringStream2.flatMap(x -> stringToArray(x));
    }
    
    

    @Test
    public void Test6() {
        // 将现有的流转换成其他的流
        Stream.of(this.stream).forEach(System.out::println);
    }

    @Test
    public void Test7() {
        Integer[] integers = {2, 2, 5, 1, 4, 9, 7};
        Stream<Integer> stream = Arrays.stream(integers);
        // 自然排序
        stream.sorted().forEach(System.out::println);
        // 定制排序
//        stream.sorted((x,y) -> (Integer) x > (Integer)y ? -1 : 1).forEach(System.out::println);
    }

    @Test
    public void Test8() {
        // 类型转换
        String[] strs = {"1", "23", "654", "10"};
        Stream<String> stream = Arrays.stream(strs);
        stream.mapToInt(x -> Integer.valueOf(x)).forEach(System.out::println);
    }
    
    @Test
    public void Test9() {
        // peek，元素和原来流中的相同，但是每次获取都会调用一个cusmer消费函数，在函数中可以执行对应的操作
        // 适合调试stream流，获取一个值输出一下，惰性输出
        stream.peek(s -> System.out.println("输出：" + s)).toArray();
    }
}
```



### 4、终止操作

-   max：最大值
-   min：元素最小值
-   findFirst：返回第一个元素
-   findAny：返回任意一个元素
-   anyMatch：任意一个元素匹配返回true
-   allMatch：所有元素匹配返回true
-   noneMatch：没有元素匹配返回true

```java
@Test
public void Test9() {
    // 返回元素总数
    long count = stream.filter(x -> "苹果".equals(x) ? false : true).distinct().count();
    System.out.println(count);
}

@Test
public void Test10() {
    // 求流中最大值和最小值
    Integer[] integers = {2, 2, 5, 1, 4, 9, 7};
    Stream<Integer> stream = Arrays.stream(integers);
    // Optional<Integer> max = stream.max((x, y) -> (int) x > (int) y ? 1 : -1);
    // System.out.println(max);
    Optional<Integer> min = stream.min((x, y) -> (int) x > (int) y ? 1 : -1);
    System.out.println(min);
}

@Test
public void Test11(){
    // 归约 -> 加总操作
    Integer[] integers = {2, 2, 5, 1, 4, 9, 7};
    Stream<Integer> stream = Arrays.stream(integers);
    Optional<Integer> reduce = stream.reduce((x, y) -> x + y);
    System.out.println(reduce.get());   // 得到对应的值
}


```





### 5、收集操作

将操作完的流进行收集，就是统计最后的结果集

```java
@Test
public void testIterator(){
    // iterator
    Iterator<String> iterator = stream.filter(x -> x.length() == 2).iterator();
    while (iterator.hasNext()) {
        String next = iterator.next();
        System.out.println(next);
    }
}

@Test
public void testForEach(){
    // forEach的方式是和iterator一样的，遍历元素
    stream.forEach(System.out::println);
    
    // 注意：并行流上，forEach是会任意顺序遍历元素的，forEachOrdered可以按顺序输出】
    stream.forEachOrdered(System.out::println);
}

@Test
public void testCollect(){
    // 放入到数组中
    String[] strings = stream.toArray(String[]::new);
    
    // 将流放入到容器中
    List<String> list = stream.collect(Collectors.toList());
    Set<String> set = stream.collect(Collectors.toSet());
    // 放入到指定容器类型
    TreeSet<String> treeSet = stream.collect(Collectors.toCollection(TreeSet::new));
    // 假如收集的是字符串，那么可以将它们连接起来
    String str = stream.collect(Collectors.joining());
    // 连接字符串并且字符串之间用符号隔开
    String strJoin = stream.collect(Collectors.joining(","));
}

@Test
public void testResultReduction(){
    /*
            结果约简，计算总和、数量、平均值、最小值、最大值
         */
    IntSummaryStatistics statistics = stream.collect(Collectors.summarizingInt(String::length));
    System.out.println(statistics.getMax());
    System.out.println(statistics.getCount());
    System.out.println(statistics.getMin());
    System.out.println(statistics.getSum());
    System.out.println(statistics.getAverage());
}


class Person {
    private Integer id;
    private String name;
    ......
}

@Test
public void testCollectToMap(){
    /*
    	收集数据到Map中
    */
    
    //        Map<Integer, String> map = personStream.
    //                collect(Collectors.toMap(Person::getId, Person::getName));

    // 如果实际元素是当前收集的对象的话，可以调用 Function.identity()，这样的话value就是流中的对象了
    Map<Integer, Person> personMap = personStream.
        collect(Collectors.toMap(Person::getId, Function.identity()));
    System.out.println(personMap);

    /*
     	获取国家对应的语言
    */
    Stream<Locale> locales = Stream.of(Locale.getAvailableLocales());
    Map<String, String> collect = locales.collect(Collectors.toMap(
        Locale::getDisplayLanguage,
        loc -> loc.getDisplayLanguage(loc),
        // 重复的两个选一个
        (existingValue, newValue) -> existingValue
    ));
    System.out.println(collect);

    /* 这个例子在Java8中报错!!! */
    //        locales = Stream.of(Locale.getAvailableLocales());
    //        locales.collect(Collectors.toMap(
    //                Locale::getDisplayCountry,
    //                l -> Set.of(l.getDisplayLanguage()),
    //                (a, b) -> {
    //                    Set<String> union = new HashSet<>();
    //                    union.addAll(b);
    //                    return union;
    //                }
    //        ));
    
    
    /**
    	Collectors#collectingAndThen()：将数据进行收集，然后再对数据进行操作
    */
    Collectors.collectingAndThen(Collectors.toList(), 
                                // 对子节点的数据进行排序
                                list -> list.stream().sorted(Comparator.comparing(SysDept::getDeptSort))
                                        .collect(Collectors.toList()))));
}

```



**群组和分区**

对收集的数据进行操作，让它们进行分组和分区

***分组***

```java
/*
   分组操作：Collectors.groupingBy(Function)
 */
Stream<Locale> locales = Stream.of(Locale.getAvailableLocales());
// 按照国家分组
//        Map<String, List<Locale>> listMap = locales.collect(Collectors.groupingBy(Locale::getCountry));
//        System.out.println(listMap);


// 分组并统计每个分组的个数
// 要求：男女分组并计算人数
Map<String, Long> map = persons.collect(Collectors.groupingBy(Person::getSex, Collectors.counting()));

// 分组并统计每个分组的总数
// 要求：统计不同班级的总班费
// 输出 {"一班": 123, "二班": 333}
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.summingLong(Person::getAmount)));

// 分组并统计每个分组的平均值
// 要求：统计不同班级的平均班费
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.averagingLong(Person::getAmount)));

// 分组并Join分组List，将List做拼接
// 要求：统计不同班级的学生并将他们的名字用"，"拼接成字符串
// 输出 -> map {"二班", "Names：[小明, 小黑, ......]"}
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.Joining(", ", "Names：[", "]")));


// 分组并转换分组为收集所需要的信息
// 要求：统计不同班级的学生并获取他们名字的集合
// 输出 -> map {"二班", ["小明", "小黑", ......]}
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.mapping(Person::getName, Collectors.toList())));
// 或者
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.mapping(Person::getName, Collectors./* toSet() */)));

// 分组并再次分组
// 要求：统计不同班级学生中男生和女生的人数
// 输出 -> map {"一班": {"男": 3, "女": 100, "中性": 2000}, "二班": {......}}
Map<String, Integer> map = persons.collect(Collectors.groupingBy(Person::getClassName, Collectors.groupingBy(Person::getSex, Collections.counting()));	// 我擦，套娃
```



***分区***

```java
/*
        分区操作：Collectors.partitioningBy(Predicate)
     */
Map<Boolean, List<Locale>> map = locales.collect(Collectors.partitioningBy(l -> l.getLanguage().equals("en")));
List<Locale> list = map.get(true);
System.out.println(list);
```



### 6、并行流

当遇到数据量非常大的时候需要用到并行流，并行流的开销大，所以小数据量是不需要并行流的

```java
    @Test
    public void test(){
        /*
            并行流的获取
                1、从集合中获取          parallelStream()
                2、顺序流转换为并行流     parallel()
         */
        Stream<String> parallelStream = dataList.parallelStream();
//        Stream<String> parallel = parallelStream.parallel();
    }
```

**注意**：使用并行流的时候要注意容器的共享问题





### 题目

①生成10个int类型随机数的数组

```java
int[] array = Stream.generate(Math::random).mapToInt(x -> (int)(x * 100) + 1)
        .limit(10)
        .toArray();
```









## Optional类型

Optional<T>对象是一种包装器对象，它是一种更安全的形式，可以有效的防止空指针异常的出现。

在以往，我们在操作对象之前都需要对对象进行判空，如果是**对象中调用另外一个对象的这种多层嵌套调用**，那么判空的操作将会看起来不是那么的优雅，所以Java8为了简化这个操作，就出现了Optinal这个类库，这个就是用来代替我们做判空的操作，让我们可以更加专注业务而不是空指针的处理。



### 1、创建Optional

```java
@Test
public void test01(){
    User user = null;
    // 创建一个空的Optional对象
    Optional<Object> empty = Optional.empty();

    // 使用of去接收参数如果为空的话就会报空指针异常，所以参数是不能为空的
    Optional<User> optionalUser1 = Optional.of(user);
    
    // 如果不知道对象是否为空，那么就使用 ofNullable,这样就不会报错
    Optional<User> optionalUser2 = Optional.ofNullable(user);
}
```



### 2、使用Optional

-   ifPresent(Consumer<? super T> consumer)

    对象不为空就执行lambda函数

-   ifPresentElse

    对象不为空执行第一个函数，为空执行第二个函数

```java
@Test
public void testUseOptional(){
    // 这个user假设是从数据库查询出来的
    User user = null;
    Optional<User> optionalUser = Optional.ofNullable(user);
    // 对象为空，则返回false，对象不为空，返回true
    boolean isNull = optionalUser.isPresent();
    System.out.println(isNull);
    // 对象不为空就执行lambda函数，为空就不执行
    optionalUser.ifPresent(System.out::println);
    // 对象为空执行第一个lambda，不为空执行第二个lambda（这个是java9的API）
    optionalUser.ifPresentElse(System.out::println, () -> log.error("对象为空"));
}
```



### 3、空值处理

如果Optional中包装的对象为空值，我们可以给它赋值

```java
@Test
public void testHandleNull(){
    User user = null;
    Optional<User> optionalUser = Optional.ofNullable(user);
    // 如果值不存在，那么就返回我们设置的值
    user = optionalUser.orElse(createUser());
    System.out.println(user);

    // 通过计算获取值
    User user1 = optionalUser.orElseGet(this::createUser2);
    System.out.println(user1);

    // 空值时抛出异常
    User user2 = optionalUser.orElseThrow(IllegalStateException::new);
    System.out.println(user2);
}
```



### 4、Optional管道化

对Optional中的数据进行处理，例如：我们需要的不是当前Optional的对象，而是它里面的另一个对象的调用，那么这个时候我们就可以使用map来获取，最后接收的还是Optional对象，那么这个时候我们就可以对这个对象进行判空操作，省去了包装的过程

```java
@Test
public void testOptionalStream(){
    User user = createUser();
    Optional<User> optionalUser = Optional.of(user);
    // 我们需要的时Dog对象
    Optional<Dog> userDog = optionalUser.map(User::getDog);
    // 我们还可以对Dog进行map操作
    Optional<String> dogName = userDog.map(Dog::getName);
    dogName.ifPresent(System.out::println);
}
```







# Java9-17新特性

## 如何学习新特性

对于新特性，我们应该从哪几个角度学习新特性呢？

1.  语法层面：

    -   比如JDK5中的自动拆箱、自动装箱、enum、泛型
    -   比如JDK8中的lambda表达式、接口中的默认方法、静态方法
    -   比如JDK10中局部变量的类型推断
    -   比如JDK12中的switch
    -   比如JDK13中的文本块

2.  API层面

    -   比如JDK8中的Stream、Optional、新的日期时间、HashMap的底层结构
    -   比如JDK9中String的底层结构
    -    新的 / 过时的 API

3.  底层优化

    -   比如JDK8中永久代被元空间替代、新的JS执行引擎
    -   比如新的垃圾回收器、GC参数、JVM的优化

    



## 工具

### JShell（9）

Java 终于拥有了像Python 和 Scala 之类语言的REPL工具（交互式编程环境，read - evaluate - print - loop）：*jShell*。以交互式的方式对语句和表达式进行求值。*即写即得*、*快速运行*。利用jShell在没有创建类的情况下，在命令行里直接声明变量，计算表达式，执行语句。

**使用**

在cmd中输入`JShell`即可进入交互模式



### JDK11：更简化的编译运行程序

在之前，运行java程序需要使用`javac`编译java文件，然后再`java class`文件运行。

JDK11之后可以直接使用`java java文件`运行

![image-20230312232043863](D:\学习笔记\img\img01\image-20230312232043863.png)



## 语法

### try-catch资源关闭

JDK7引入了`try(资源)`方式将资源自动关闭，前提是继承了`AutoCloseable`

```java
@Test
public void test(){
    try(var fis = new FileInputStream("test.txt");
        var fos = new FileOutputStream("test.txt")) {
        // 略
    } catch (IOException e) {
        // 略
    }
}
```

JDK9继续改进，直接使用`try(变量名)`即可自动关闭资源

**示例**

```java
@Test
public void test(){
    var reader = new InputStreamReader(System.in);
    try (reader) {
        System.out.println(reader.read());
    } catch (IOException e) {
        System.out.println(e);
    }
    reader = new InputStreamReader(System.in);
}
```

![image-20230308181448979](D:\学习笔记\img\img01\image-20230308181448979.png)

==注意：使用变量名关闭的资源必须声明为`final`，第二次赋值会直接报错==



### 局部变量类型推断

Java10新特性，可以使用var来接收变量，前提是这个类型必须是已知的，比如`new对象`，接收方法返回值

```java
var str = "";
var list = new ArrayList<String>();
var aaa = list.get(1);	// 接收方法返回值
```

变量是未知类型时不能使用，比如变量的声明、数组的 `{}`创建方式

```java
var arr = {1, 2, 3};	// 报错
var i;	// 报错
```



### instanceof的模式匹配

使用`instanceof`的时候都需要判断一下，然后再向下转型，Java14版本中引入省去这个步骤的方式，在判断之后直接声明转型的变量名

Java14之前

```java
@Test
public void test14Before(){
    Object o = new String("hello word");
    if (o instanceof String) {
        String str = (String) o;
        System.out.println(str.concat("Java"));
    }
}
```

Java14之后

```java
@Test
public void test14After(){
    Object o = new String("hello word");
    // 关键字后追加变量名
    if (o instanceof String str) {
        System.out.println(str.concat("Java"));
    }
}
```



### switch表达式

#### ①`->`语句

JDK12使用`->`来代替`break`，意思就是使用了`->`的就不会有case穿透，更加简洁优雅

```java
@Test
public void test01(){
    // JDK12之前
    int a = 1;
    switch (a) {
        case 1:
            System.out.println("1");
            break;
        case 2:
            System.out.println("2");
            break;
        case 3:
        case 4:
        case 5:
            System.out.println("3");
            break;
        default:
            System.out.println("无匹配");
    }

    // JDK12之后
    switch (a) {
        case 1 -> System.out.println("1");
        case 2 -> System.out.println("2");
        case 3, 4, 5 -> System.out.println("3");
        default -> System.out.println("无匹配");
    }
}
```



#### ②yield语句

JDK13引入`yield`关键字，它可以结束`switch语句`并返回值。yield和return的区别在于：return会直接跳出当前循环或者方法，而yield只会跳出当前switch块。

```java
@Test
public void test02(){
    int a = 1;
    int res = switch (a) {
        case 1 -> 1;	// 等价于 case 1: yield 1;
        case 2 -> 2;
        case 3, 4, 5 -> 4;
        default -> {
            System.out.println("匹配失败");
            yield -1;
        }
    };
    System.out.println(res);
    
    // 也可以使用 : + yield方式
    int res = switch (a) {
            case 1 : yield  1;
            case 2 : yield 2;
            case 3, 4, 5 : yield 4;
            default: {
                System.out.println("匹配失败");
                yield -1;
            }
        };
    System.out.println(res);
}
```

==注意：不能`:`和`->`混用==



#### ③模式匹配

**JDK17的预览特性：switch的模式匹配**

非模式匹配

```java
@Test
public void test(){
    double d = 1.0d;
    System.out.println(formatter(d));
}

public static String formatter(Object o) {
    String formatStr = "unkonwn";
    if (o instanceof Integer i) {
        formatStr = String.format("int %d", i);
    } else if (o instanceof Long l) {
        formatStr = String.format("long %d", l);
    } else if (o instanceof Double d) {
        formatStr = String.format("double %f", d);
    }
    return formatStr;
}
```

使用switch模式匹配

```java
@Test
public void test(){
    double d = 1.0d;
    System.out.println(formatter(d));
}

public static String formatter(Object o) {
    return switch (o) {
        case Integer i -> String.format("int %d", i);
        case Long l -> String.format("long %d", l);
        case Double d -> String.format("double %f", d);
        default -> o.toString();
    };
}
```





### 文本块

```java
@Test
public void test01(){
    String str = "     <dependency>\n" +
            "                    <groupId>junit</groupId>\n" +
            "                    <artifactId>junit</artifactId>\n" +
            "                    <version>4.13.2</version>\n" +
            "                </dependency>";
}
```

```java
@Test
public void test02(){
    String str = """
                 <dependency>
                     <groupId>junit</groupId>
                     <artifactId>junit</artifactId>
                     <version>4.13.2</version>
                 </dependency>
            """;
}
```

跟pthon的一毛一样~~~



### Record 

*背景*

早在2019年2月份，Java 语言架构师 Brian Goetz，曾写文抱怨“*Java太啰嗦*”或有太多的“繁文缛节”。他提到：开发人员想要创建纯数据载体类（plain data carriers）通常都必须编写大量低价值、重复的、容易出错的代码。如：构造函数、getter/setter、equals()、hashCode()以及toString()等。

以至于很多人选择使用IDE的功能来自动生成这些代码。还有一些开发会选择使用一些第三方类库，如Lombok等来生成这些方法。

***\*JDK14中预览特性：神说要用record，于是就有了。\****实现一个简单的数据载体类，为了避免编写：构造函数，访问器，equals()，hashCode () ，toString ()等，Java 14推出record。

*record* 是一种全新的类型，它本质上是一个 *final* 类，同时所有的属性都是 *final* 修饰，它会自动编译出 *public get* 、*hashcode* 、*equals*、*toString*、构造器等结构，减少了代码编写量。

具体来说：当你用*record* 声明一个类时，该类将自动拥有以下功能：

-   获取成员变量的简单方法，比如例题中的 name() 和 partner() 。**注意区别于我们平常getter()的写法**。
-    一个 equals 方法的实现，执行比较时会比较该类的所有成员属性。
-   重写 hashCode() 方法。
-   一个可以打印该类所有成员属性的 toString() 方法。
-   只有一个构造方法。

此外：

-   还可以在record声明的类中定义静态字段、静态方法、构造器或实例方法。

-   不能在record声明的类中定义实例字段；类不能声明为abstract；不能声明显式的父类等。

    不能声明显示的父类是因为这个类已经有父类了，是`Record`类，和Eunm一样都是隐式父类



**代码示例**

```java
public record Person(String name, Integer age) {

    public Person() {
        // 必须调用this()
        this("", 0);
    }
    
    public static void main(String[] args) {
        var person = new Person("小智", 12);
        System.out.println(person.name());
        System.out.println(person.age());
    }
}
```





### 密封类 sealed class

背景：

在 Java 中如果想让一个类不能被继承和修改，这时我们应该使用 *final* 关键字对类进行修饰。不过这种要么可以继承，要么不能继承的机制不够灵活，有些时候我们可能想让某个类可以被某些类型继承，但是又不能随意继承，是做不到的。Java 15 尝试解决这个问题，引入了 *sealed* 类，被 *sealed* 修饰的类可以指定子类。这样这个类就只能被指定的类继承。

具体使用：

-   使用修饰符*sealed*，可以将一个类声明为密封类。密封的类使用保留关键字*permits*列出可以直接扩展（即extends）它的类。
-    *sealed* 修饰的类的机制具有传递性，**它的子类必须使用指定的关键字进行修饰**，且只能是 *final*、*sealed*、*non-sealed* 三者之一。
    -   final：这个类不被任何类所继承
    -   sealed：指定可以继承的类
    -   non-sealed：所有类都可以继承

**代码示例**

```java
sealed class Animal permits Cat, Dog {

}
sealed class Cat extends Animal permits Rabbit{

}

non-sealed class Dog extends Animal {

}

class Laohu extends Dog{
    
}

non-sealed class Rabbit extends Cat {
    
}
```





## API

### String存储结构和API变更

#### 存储结构变化

String 再也不用 char[] 来存储了，改成了 byte[] 加上编码标记，节约了一些空间。

![image-20230308194944719](D:\学习笔记\img\img01\image-20230308194944719.png)



#### API变化

**JDK11新特性：新增了一系列字符串处理方法**

| ***\*描述\****       | ***\*举例\****                                 |
| -------------------- | ---------------------------------------------- |
| 判断字符串是否为空白 | " ".isBlank(); // true                         |
| 去除首尾空白         | " Javastack ".strip(); // "Javastack"          |
| 去除尾部空格         | " Javastack ".stripTrailing(); // " Javastack" |
| 去除首部空格         | " Javastack ".stripLeading(); // "Javastack "  |
| 复制字符串           | "Java".repeat(3);// "JavaJavaJava"             |
| 行数统计             | "A\nB\nC".lines().count(); // 3                |



**JDK12新特性：**

-   String 实现了 Constable 接口

    Optional<? **extends** ConstantDesc> describeConstable();

-   String新增方法：transform(Function)



### JDK17：标记删除Applet API





### 其他结构变化

#### JDK9：UnderScore(下划线)使用的限制）





## GC方面新特性

GC是Java主要优势之一。 然而，当GC停顿太长，就会开始影响应用的响应时间。随着现代系统中内存不断增长，用户和程序员希望JVM能够以高效的方式充分利用这些内存， 并且无需长时间的GC暂停时间。

#### G1 GC

JDK9以后默认的垃圾回收器是G1GC。

***\*JDK10 : 为G1提供并行的Full GC\****

G1最大的亮点就是可以尽量的避免full gc。但毕竟是“尽量”，在有些情况下，G1就要进行full gc了，比如如果它无法足够快的回收内存的时候，它就会强制停止所有的应用线程然后清理。

在Java10之前，一个单线程版的标记-清除-压缩算法被用于full gc。为了尽量减少full gc带来的影响，在Java10中，就把之前的那个单线程版的标记-清除-压缩的full gc算法改成了支持多个线程同时full gc。这样也算是减少了full gc所带来的停顿，从而提高性能。

你可以通过*-XX:ParallelGCThreads*参数来指定用于并行GC的线程数。

***\*JDK12：可中断的 G1 Mixed GC\****

***\*JDK12：增强G1，自动返回未用堆内存给操作系统\****



#### Shenandoah GC

***\*JDK12：Shenandoah GC：低停顿时间的GC\****

![img](D:\学习笔记\img\img01\wps1.jpg) 

Shenandoah 垃圾回收器是 Red Hat 在 2014 年宣布进行的一项垃圾收集器研究项目 Pauseless GC 的实现，旨在***\*针对 JVM 上的内存收回实现低停顿的需求\****。据 Red Hat 研发 Shenandoah 团队对外宣称，Shenandoah 垃圾回收器的暂停时间与堆大小无关，这意味着无论将堆设置为 200 MB 还是 200 GB，都将拥有一致的系统暂停时间，不过实际使用性能将取决于实际工作堆的大小和工作负载。

Shenandoah GC 主要目标是 99.9% 的暂停小于 10ms，暂停与堆大小无关等。

这是一个实验性功能，不包含在默认（Oracle）的OpenJDK版本中。

Shenandoah开发团队在实际应用中的测试数据：

![img](D:\学习笔记\img\img01\wps2.jpg) 

***\*JDK15：Shenandoah垃圾回收算法转正\****

Shenandoah垃圾回收算法终于从实验特性转变为产品特性，这是一个从 JDK 12 引入的回收算法，该算法通过与正在运行的 Java 线程同时进行疏散工作来减少 GC 暂停时间。Shenandoah 的暂停时间与堆大小无关，无论堆栈是 200 MB 还是 200 GB，都具有相同的一致暂停时间。

Shenandoah在JDK12被作为experimental引入，在JDK15变为Production；之前需要通过*-XX:+UnlockExperimentalVMOptions* *-XX:+UseShenandoahGC*来启用，现在只需要*-XX:+UseShenandoahGC*即可启用



#### 革命性的 ZGC

***\*JDK11：引入革命性的 ZGC\****

ZGC，这应该是JDK11最为瞩目的特性，没有之一。 

ZGC是一个并发、基于region、压缩型的垃圾收集器。

ZGC的设计目标是：支持TB级内存容量，暂停时间低（<10ms），对整个程序吞吐量的影响小于15%。 将来还可以扩展实现机制，以支持不少令人兴奋的功能，例如多层堆（即热对象置于DRAM和冷对象置于NVMe闪存），或压缩堆。

***\*JDK13：ZGC:将未使用的堆内存归还给操作系统\****

***\*JDK14：ZGC on macOS和windows\****

• JDK14之前，ZGC仅Linux才支持。现在mac或Windows上也能使用ZGC了，示例如下：

  -XX:+UnlockExperimentalVMOptions -XX:+UseZGC

• ZGC与Shenandoah目标高度相似，在尽可能对吞吐量影响不大的前提下，实现在任意堆内存大小下都可以把垃圾收集的停顿时间限制在*十毫秒以内*的低延迟。

![img](D:\学习笔记\img\img01\wps3.jpg) 

 

![img](D:\学习笔记\img\img01\wps4.jpg) 

***\*JDK15：ZGC 功能转正\****

ZGC是Java 11引入的新的垃圾收集器，经过了多个实验阶段，自此终于成为正式特性。

但是这并不是替换默认的GC，默认的GC仍然还是G1；之前需要通过*-XX:+UnlockExperimentalVMOptions*、*-XX:+UseZGC*来启用ZGC，现在只需要*-XX:+UseZGC*就可以。相信不久的将来它必将成为默认的垃圾回收器。

ZGC的性能已经相当亮眼，用“令人震惊、革命性”来形容，不为过。未来将成为服务端、大内存、低延迟应用的首选垃圾收集器。

怎么形容Shenandoah和ZGC的关系呢？异同点大概如下：

• 相同点：性能几乎可认为是相同的

• 不同点：ZGC是Oracle JDK的，根正苗红。而Shenandoah只存在于OpenJDK中，因此使用时需注意你的JDK版本

***\*JDK16：ZGC 并发线程处理\****

在线程的堆栈处理过程中，总有一个制约因素就是safepoints。在safepoints这个点，Java的线程是要暂停执行的，从而限制了GC的效率。

回顾：

我们都知道，在之前，需要 GC 的时候，为了进行垃圾回收，需要所有的线程都暂停下来，这个暂停的时间我们称为 ***\*Stop The World\****。

而为了实现 STW 这个操作， JVM 需要为每个线程选择一个点停止运行，这个点就叫做***\*安全点（Safepoints）\****。

而ZGC的并发线程堆栈处理可以保证Java线程可以在GC safepoints的同时可以并发执行。它有助于提高所开发的Java软件应用程序的性能和效率。



## 小结与展望

随着云计算和 AI 等技术浪潮，当前的计算模式和场景正在发生翻天覆地的变化，不仅对 Java 的发展速度提出了更高要求，也深刻影响着 Java 技术的发展方向。***\*传统的大型企业或互联网应用，正在被云端、容器化应用、模块化的微服务甚至是函数(FaaS， Function-as-a-Service)所替代。\****

***\*Java 需要在新的计算场景下，改进开发效率。\****比如，Java 代码虽然进行了一些类型推断等改进，更易用的集合 API 等，但仍然给开发者留下了过于刻板、形式主义的印象，这是一个长期的改进方向。

Java虽然标榜面向对象编程，却毫不顾忌的加入*面向接口编程思想*，又扯出*匿名对象*的概念，每增加一个新的东西，对Java的根本（面向对象思想）的一次冲击。





