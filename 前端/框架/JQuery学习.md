# 一、JQuery入门



## JQ的使用

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>TestJQuery的使用</title>
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
     JQuery的使用
         ①引入JQ的文件
           <script src="JQuery文件路径">
         ②上一个标签是引入JQuery文件，下一个标签写入代码
         ③使用核心函数

     注意：①绑定事件的时候要注意JQ的写法是没有on的，也不用 用 “=” 将事件和函数连接起来
            格式是：$("选择器").click(function() {
                函数体;
            });
          ②使用JQ一定要导入JQ文件
          ③$是一个函数
          ④添加响应函数
            1.使用JQ查询到标签对象
            2.使用标签对象.事件名
                例如：标签对象.click(function(){
                        函数体;
                     });
  */



        $(function () {     //表示页面加载完之后，相当于window.onload=function(){};
            alert("HelloWorld")
        });

        $(function () {
            $("#but01").click(function () {
                alert('Hello');
            });
        });
    </script>

</head>
<body>

    <button id="but01">按钮</button>
</body>
</html>
```



## 核心函数

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test核心函数</title>
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            核心函数：$
                ①传入参数为 [函数] 时：页面加载完自动调用
                ②传入参数为HTML字符串时：根据这个字符串创建元素节点对象
                ③传入参数为 [选择器] 时：
                    $("#id选择器")     id选择器
                    $("标签名")        标签选择器
                    $(".class")       类选择器

                ④传入参数为dom对象时：把dom对象转换为JQ对象
         */

    </script>
</head>
<body>

</body>
</html>
```



## JQ对象和dom对象

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Test</title>
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            一、区分JQ对象和dom对象
                ①dom对象alert()出来的效果是     [object HTML标签名Element]
                  JQ对象alert()出来的效果是      [object Object]
                ②JQ包装的dom对象也是JQ对象
                    例如：$(dom对象)得到JQ对象
                ③JQ提供API查询到的也是JQ对象
                    例如：通过$("#id")得到的JQ对象

            二、JQ对象的本质
                ①JQ不能使用dom对象的属性和方法，dom对象也不能使用JQ对象的属性和方法

                ②dom对象转JQ对象
                    1.创建dom对象
                    2.dom对象放入核心函数$中

                ③JQ对象转dom对象
                    1.创建JQ对象
                    2.JQ对象是个数组，所以可以通过下标取值的方式取出相应的dom对象


            注意：代码是由上到下依次执行的，那么如果查询得到的标签是在页面加载的时候执行的，那么它的值就是undefined
                 所以，一般得到对象的操作都是在页面加载完之后，一定要注意是在加载页面加载前执行还是页面加载后执行

         */

        window.onload = function () {
            var divobj = document.getElementById("div01");
            alert($(divobj))    //dom对象转JQ对象
            alert(divobj)       //[object HTMLDivElement]
        }

        $(function () {
            alert($("#div01"))  //通过id选择器得到JQ对象
        });

        $(function () {
            //1.创建JQ对象
            var div01 = $("#div01");
            //2.取出dom对象
            var getdom = div01[0];
            alert(getdom)
        });
    </script>
</head>
<body>

    <div id="div01" value="div"></div>
</body>
</html>
```









# 二、选择器

## 1	-	基本选择器

```html
   <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            id选择器       :     #id
            标签名选择器    :     标签名
            类选择器       :    .class
            所有元素选择器  :      *
            组合选择器     :     多个选择器
         */

        $(function () {
            //1.选择 id 为 one 的元素 "background-color","#bbffaa"
            $("#btn1").click(function () {
                $("#one").css("background-color","#bbffaa");
            });
            //2.选择 class 为 mini 的所有元素
            $("#btn2").click(function () {
                $(".mini").css("background-color","#bbffaa")
            })
            //3.选择 元素名是 div 的所有元素
            $("#btn3").click(function () {
                $("div").css("background-color","#bbffaa")
            })

            //4.选择所有的元素
            $("#btn4").click(function () {
                $("*").css("background-color","#bbffaa")
            })

            //5.选择所有的 span 元素和id为two的元素
            $("#btn5").click(function () {
                $("span,#two").css("background-color","#bbffaa")
            })
        });

    </script>
</head>
<body>
    <!-- 	<div>
            <h1>基本选择器</h1>
        </div>	 -->
    <input type="button" value="选择 id 为 one 的元素" id="btn1" />
    <input type="button" value="选择 class 为 mini 的所有元素" id="btn2" />
    <input type="button" value="选择 元素名是 div 的所有元素" id="btn3" />
    <input type="button" value="选择 所有的元素" id="btn4" />
    <input type="button" value="选择 所有的 span 元素和id为two的元素" id="btn5" />

    <br>
    <div class="one" id="one">
        id 为 one,class 为 one 的div
        <div class="mini">class为mini</div>
    </div>
    <div class="one" id="two" title="test">
        id为two,class为one,title为test的div
        <div class="mini" title="other">class为mini,title为other</div>
        <div class="mini" title="test">class为mini,title为test</div>
    </div>
    <div class="one">
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini"></div>
    </div>
    <div class="one">
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini" title="tesst">class为mini,title为tesst</div>
    </div>
    <div style="display:none;" class="none">style的display为"none"的div</div>
    <div class="hide">class为"hide"的div</div>
    <div>
        包含input的type为"hidden"的div<input type="hidden" size="8">
    </div>
    <span class="one" id="span">^^span元素^^</span>
</body>
</html>
```



## 2	-	层次选择器

```html
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            层次选择器
                ①ancestor descendant：祖先选择器
                    1.ancestor是祖先，descendant是后代
                    2.被包含在里面的就是它的后代
                ②parent > child     ：父子选择器
                    1.parent是父  child是子
                    2.在子代下面的是孙代，这个就不能作为是子代了
                ③prev + next        ：下一个选择器
                    匹配所有紧接在 prev 元素后的 next 元素
                ④prev ~ siblings    ：
                    匹配 prev 元素之后的所有 siblings 元素
         */

        $(document).ready(function(){
            //1.选择 body 内的所有 div 元素     --      祖先选择器
            $("#btn1").click(function(){
                $("body div").css("background", "#bbffaa");
            });

            //2.在 body 内, 选择div子元素       --      父子选择器
            $("#btn2").click(function(){
                $("body > div").css("background", "#bbffaa");
            });

            //3.选择 id 为 one 的下一个 div 元素     --      下一个选择器
            $("#btn3").click(function(){
                $("#one + div").css("background", "#bbffaa");
            });

            //4.选择 id 为 two 的元素后面的所有 div 兄弟元素
            $("#btn4").click(function(){
                $("#two ~ div").css("background", "#bbffaa");
            });
        });

    </script>
</head>
<body>
    <input type="button" value="选择 body 内的所有 div 元素" id="btn1" />
    <input type="button" value="在 body 内, 选择div子元素" id="btn2" />
    <input type="button" value="选择 id 为 one 的下一个 div 元素" id="btn3" />
    <input type="button" value="选择 id 为 two 的元素后面的所有 div 兄弟元素" id="btn4" />
    <br><br>
    <div class="one" id="one">
        id 为 one,class 为 one 的div
        <div class="mini">class为mini</div>
    </div>
    <div class="one" id="two" title="test">
        id为two,class为one,title为test的div
        <div class="mini" title="other">class为mini,title为other</div>
        <div class="mini" title="test">class为mini,title为test</div>
    </div>
    <div class="one">
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini"></div>
    </div>
    <div class="one">
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini">class为mini</div>
        <div class="mini" title="tesst">class为mini,title为tesst</div>
    </div>
    <div style="display:none;" class="none">style的display为"none"的div</div>
    <div class="hide">class为"hide"的div</div>
    <div>
        包含input的type为"hidden"的div<input type="hidden" size="8">
    </div>
    <span id="span">^^span元素^^</span>
</body>
</html>
```



## 3	-	基本的过滤选择器

```html
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            基本的过滤选择器：
                :first 				获取第一个元素
                :last 				获取最后一个元素
                :not(selector) 		除开选择器中的元素
                :even 				匹配索引值为偶数的元素，从0开始计数
                :odd 				匹配索引值为奇数的元素，从0开始计数
                :eq(index)		 	匹配一个给定索引值的元素，从0开始计数
                :gt(index)			匹配所有大于给定索引值的元素
                :lt(index)			匹配所有小于给定索引值的元素
                :header				匹配如 h1, h2, h3之类的标题元素
                :animated			匹配所有正在执行动画效果的元素

                说明：可以多个选择器一起使用，案例11就是很好的例子
         */

        $(document).ready(function () {
            function anmateIt() {
                $("#mover").slideToggle("slow", anmateIt);
            }

            anmateIt();
        });

        $(document).ready(function () {
            //1.选择第一个 div 元素  
            $("#btn1").click(function () {
                $("div:first").css("background", "#bbffaa");
            });

            //2.选择最后一个 div 元素
            $("#btn2").click(function () {
                $("div:last").css("background", "#bbffaa");
            });

            //3.选择class不为 one 的所有 div 元素
            $("#btn3").click(function () {
                $("div:not(.one)").css("background", "#bbffaa");
            });

            //4.选择索引值为偶数的 div 元素
            $("#btn4").click(function () {
                $("div:even").css("background", "#bbffaa");
            });

            //5.选择索引值为奇数的 div 元素
            $("#btn5").click(function () {
                $("div:odd").css("background", "#bbffaa");
            });

            //6.选择索引值为大于 3 的 div 元素
            $("#btn6").click(function () {
                $("div:gt(3)").css("background", "#bbffaa");
            });

            //7.选择索引值为等于 3 的 div 元素
            $("#btn7").click(function () {
                $("div:eq(3)").css("background", "#bbffaa");
            });

            //8.选择索引值为小于 3 的 div 元素
            $("#btn8").click(function () {
                $("div:lt(3)").css("background", "#bbffaa");
            });

            //9.选择所有的标题元素
            $("#btn9").click(function () {
                $(":header").css("background", "#bbffaa");
            });

            //10.选择当前正在执行动画的所有元素
            $("#btn10").click(function () {
                $(":animated").css("background", "#bbffaa");
            });
            //11.选择没有执行动画的最后一个div
            $("#btn11").click(function () {
                $("div:not(:animated):last").css("background", "#bbffaa");
            });
        });
    </script>
</head>
<body>

<input type="button" value="选择第一个 div 元素" id="btn1"/>
<input type="button" value="选择最后一个 div 元素" id="btn2"/>
<input type="button" value="选择class不为 one 的所有 div 元素" id="btn3"/>
<input type="button" value="选择索引值为偶数的 div 元素" id="btn4"/>
<input type="button" value="选择索引值为奇数的 div 元素" id="btn5"/>
<input type="button" value="选择索引值为大于 3 的 div 元素" id="btn6"/>
<input type="button" value="选择索引值为等于 3 的 div 元素" id="btn7"/>
<input type="button" value="选择索引值为小于 3 的 div 元素" id="btn8"/>
<input type="button" value="选择所有的标题元素" id="btn9"/>
<input type="button" value="选择当前正在执行动画的所有元素" id="btn10"/>
<input type="button" value="选择没有执行动画的最后一个div" id="btn11"/>

<h3>基本选择器.</h3>
<br><br>
<div class="one" id="one">
    id 为 one,class 为 one 的div
    <div class="mini">class为mini</div>
</div>
<div class="one" id="two" title="test">
    id为two,class为one,title为test的div
    <div class="mini" title="other">class为mini,title为other</div>
    <div class="mini" title="test">class为mini,title为test</div>
</div>
<div class="one">
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini"></div>
</div>
<div class="one">
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini" title="tesst">class为mini,title为tesst</div>
</div>
<div style="display:none;" class="none">style的display为"none"的div</div>
<div class="hide">class为"hide"的div</div>
<div>
    包含input的type为"hidden"的div<input type="hidden" size="8">
</div>
<div id="mover">正在执行动画的div元素.</div>
</body>
</html>
```



## 4	-	内容过滤选择器

```html
	<script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
		<script type="text/javascript">
			$(document).ready(function(){
				function anmateIt(){
					$("#mover").slideToggle("slow", anmateIt);
				}
	
				anmateIt();				
			});
			/*
				内容过滤选择器：
					:contains(text)		标签中的文本内容是否包含这个字符串
					:empty				空的，标签中间无内容
					:has(selector)		匹配含有选择器所匹配的元素的元素
						selector：选择器
						说明：比如要给含有p标签的div标签赋值
							$("div:has("p")").value		还不懂看文档！！！

					注意：①使用这个过滤器的时候要知道是得到包含选择器的那个元素，而不是选择器的那个元素
						 ②写选择器的时候可以不用双引号，双引号会报错

					:parent				非空的元素
			*/
			
			$(document).ready(function(){
				//1.选择 含有文本 'di' 的 div 元素
				$("#btn1").click(function(){
					$("div:contains('di')").css("background", "#bbffaa");
				});

				//2.选择不包含子元素(或者文本元素) 的 div 空元素
				$("#btn2").click(function(){
					$("div:empty").css("background", "#bbffaa");
				});

				//3.选择含有 class 为 mini 元素的 div 元素
				$("#btn3").click(function(){
					$("div:has(.mini)").css("background", "#bbffaa");
				});

				//4.选择含有子元素(或者文本元素)的div元素
				$("#btn4").click(function(){
					$("div:parent").css("background", "#bbffaa");
				});
			});
		</script>
	</head>
	<body>		
		<input type="button" value="选择 含有文本 'di' 的 div 元素" id="btn1" />
		<input type="button" value="选择不包含子元素(或者文本元素) 的 div 空元素" id="btn2" />
		<input type="button" value="选择含有 class 为 mini 元素的 div 元素" id="btn3" />
		<input type="button" value="选择含有子元素(或者文本元素)的div元素" id="btn4" />
		
		<br><br>
		<div class="one" id="one">
			id 为 one,class 为 one 的div
			<div class="mini">class为mini</div>
		</div>
		<div class="one" id="two" title="test">
			id为two,class为one,title为test的div
			<div class="mini" title="other">class为mini,title为other</div>
			<div class="mini" title="test">class为mini,title为test</div>
		</div>
		<div class="one">
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini"></div>
		</div>
		<div class="one">
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini" title="tesst">class为mini,title为tesst</div>
		</div>
		<div style="display:none;" class="none">style的display为"none"的div</div>
		<div class="hide">class为"hide"的div</div>
		<div>
			包含input的type为"hidden"的div<input type="hidden" size="8">
		</div>
		<div id="mover">正在执行动画的div元素.</div>
	</body>
</html>

```



## 5	-	可见性过滤选择器

```html
		<script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
		<script type="text/javascript">
			$(document).ready(function(){
				function anmateIt(){
					$("#mover").slideToggle("slow", anmateIt);
				}
	
				anmateIt();	
			});
			/*
			 	可见性过滤器
					:hidden			匹配所有不可见元素，或者type为hidden的元素
					:visible		匹配所有的可见元素
			*/

			$(document).ready(function(){
				//1.选取所有可见的  div 元素
				$("#btn1").click(function(){
					$("div:visible").css("background", "#bbffaa");
				});

				//2.选择所有不可见的 div 元素
				//不可见：display属性设置为none，或visible设置为hidden
				$("#btn2").click(function(){
					$("div:hidden").show("slow").css("background", "#bbffaa");
				});

				//3.选择所有不可见的 input 元素
				$("#btn3").click(function(){
					alert($("input:hidden").val());
				});	
			});
		</script>
	</head>
	<body>		
		<input type="button" value="选取所有可见的  div 元素" id="btn1">
		<input type="button" value="选择所有不可见的 div 元素" id="btn2" />
		<input type="button" value="选择所有不可见的 input 元素" id="btn3" />
		<input type="hidden" value="这是一个不可见的 input 元素">
		
		<br>
		<div class="one" id="one">
			id 为 one,class 为 one 的div
			<div class="mini">class为mini</div>
		</div>
		<div class="one" id="two" title="test">
			id为two,class为one,title为test的div
			<div class="mini" title="other">class为mini,title为other</div>
			<div class="mini" title="test">class为mini,title为test</div>
		</div>
		<div class="one">
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini"></div>
		</div>
		<div class="one">
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini">class为mini</div>
			<div class="mini" title="tesst">class为mini,title为tesst</div>
		</div>
		<div style="display:none;" class="none">style的display为"none"的div</div>
		<div class="hide">class为"hide"的div</div>
		<div>
			包含input的type为"hidden"的div<input type="hidden" value="123456789" size="8">
		</div>
		<div id="mover">正在执行动画的div元素.</div>
	</body>
</html>
```



## 6	-	属性过滤选择器

```html
<script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
<script type="text/javascript">
	/*
		属性过滤选择器：
			attribute：表示属性
			[attribute]							含有这个元素的
			[attribute=value]					含有这个元素并且是这个值的
			[attribute!=value]					含有这个元素但是不是这个值的
			[attribute^=value]					含有这个元素并且是以这个值开头的
			[attribute$=value]					含有这个元素并且是以这个值结尾的
			[attribute*=value]					含有这个元素并且里面内容包含这个值的
			[selector1][selector2][selectorN]	复合型选择器，多个条件一起使用

		注意：①value值在写的时候可以用单引号引起来或者不写，但是不能用双引号引起来
			 ②
	*/

	$(function() {
		//1.选取含有 属性title 的div元素
		$("#btn1").click(function() {
			$("div[title]").css("background", "#bbffaa");
		});
		//2.选取 属性title值等于'test'的div元素
		$("#btn2").click(function() {
			$("div[title='test']").css("background", "#bbffaa");
		});
		//3.选取 属性title值不等于'test'的div元素(*没有属性title的也将被选中)
		$("#btn3").click(function() {
			$("div[title!=test]").css("background", "#bbffaa");
		});
		//4.选取 属性title值 以'te'开始 的div元素
		$("#btn4").click(function() {
			$("div[title^='te']").css("background", "#bbffaa");
		});
		//5.选取 属性title值 以'est'结束 的div元素
		$("#btn5").click(function() {
			$("div[title$='est']").css("background", "#bbffaa");
		});
		//6.选取 属性title值 含有'es'的div元素
		$("#btn6").click(function() {
			$("div[title*='es']").css("background", "#bbffaa");
		});

		//7.首先选取有属性id的div元素，然后在结果中 选取属性title值 含有'es'的 div 元素
		$("#btn7").click(function() {
			$("div[id][title*='es']").css("background", "#bbffaa");
		});
		//8.选取 含有 title 属性值, 且title 属性值不等于 test 的 div 元素
		$("#btn8").click(function() {
			$("div[title!=test]").css("background", "#bbffaa");
		});
	});
</script>
</head>
<body>
	<input type="button" value="选取含有 属性title 的div元素." id="btn1" />
	<input type="button" value="选取 属性title值等于'test'的div元素." id="btn2" />
	<input type="button"
		value="选取 属性title值不等于'test'的div元素(没有属性title的也将被选中)." id="btn3" />
	<input type="button" value="选取 属性title值 以'te'开始 的div元素." id="btn4" />
	<input type="button" value="选取 属性title值 以'est'结束 的div元素." id="btn5" />
	<input type="button" value="选取 属性title值 含有'es'的div元素." id="btn6" />
	<input type="button"
		value="组合属性选择器,首先选取有属性id的div元素，然后在结果中 选取属性title值 含有'es'的 div 元素."
		id="btn7" />
	<input type="button"
		value="选取 含有 title 属性值, 且title 属性值不等于 test 的 div 元素." id="btn8" />

	<br>
	<br>
	<div class="one" id="one">
		id 为 one,class 为 one 的div
		<div class="mini">class为mini</div>
	</div>
	<div class="one" id="two" title="test">
		id为two,class为one,title为test的div
		<div class="mini" title="other">class为mini,title为other</div>
		<div class="mini" title="test">class为mini,title为test</div>
	</div>
	<div class="one">
		<div class="mini">class为mini</div>
		<div class="mini">class为mini</div>
		<div class="mini">class为mini</div>
		<div class="mini"></div>
	</div>
	<div class="one">
		<div class="mini">class为mini</div>
		<div class="mini">class为mini</div>
		<div class="mini">class为mini</div>
		<div class="mini" title="tesst">class为mini,title为tesst</div>
	</div>
	<div style="display: none;" class="none">style的display为"none"的div</div>
	<div class="hide">class为"hide"的div</div>
	<div>
		包含input的type为"hidden"的div<input type="hidden" value="123456789"
			size="8">
	</div>
	<div id="mover">正在执行动画的div元素.</div>
</body>
</html>
```



## 7	-	表单对象属性过滤选择器

```html
	<script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
		<script type="text/javascript">
			$(function(){				
		/*
			表单对象属性过滤选择器
				:input			匹配所有 input, textarea, select 和 button 元素
				:text			匹配所有的单行文本框
				:password		匹配所有密码框
				:radio			匹配所有单选按钮
				:checkbox		匹配所有复选框
				:submit			匹配所有提交按钮
				:image			匹配所有图像域
				:reset			匹配所有重置按钮
				:button			匹配所有按钮
				:file			匹配所有文件域
				:hidden			匹配所有不可见元素，或者type为hidden的元素

			表单对象的属性
				:enabled		匹配所有可用元素
				:disabled		匹配所有不可用元素
				:checked		匹配所有选中的被选中元素(复选框、单选框等，不包括select中的option)
				:selected		匹配所有选中的option元素

			val()：JQ对象提供的一个方法，可以操作表单项的value值，修改和获取
					修改：在括号里面写入要修改的值
					获取：什么都不写就可以了

			each()：是JQ对象提供用来遍历的方法，each()遍历的function函数中有一个this对象，
					表示当前遍历的对象
			语法：JQ对象.each(function(){
					遍历的代码;
				});
		*/		
				//1.对表单内 可用input 赋值操作
				$("#btn1").click(function(){
					$(":input").val("New Value");
				});
				//2.对表单内 不可用input 赋值操作
				$("#btn2").click(function(){
					$(":input:disabled").val("New Value Too");
				});
				//3.获取多选框选中的个数  使用size()方法获取选取到的元素集合的元素个数
				$("#btn3").click(function(){
					alert($(":checked").size())
				});
				//4.获取多选框，每个选中的value值
				$("#btn4").click(function(){
					var str = "";
					var eles = $(":checked");
					console.log(eles);
					for(var i=0;i<eles.size();i++){
						str += "【"+$(eles[i]).val()+"】";
					}
					alert(str)
				});
				//5.获取下拉框选中的内容  
				$("#btn5").click(function(){
					var str = "";
					/**
					 注意这个选择器的特殊，因为select里面的option是真正的被选择项，
					   所以 :selected 选择器和 select[name='test']选择器的关系是子父关系
					   必须按照基本选择器选择后代的方法选
					*/
					var els = $(":selected");
					console.log(els);		//控制台输出
					for(var i=0;i<els.size();i++){
						str += "【"+$(els[i]).val()+"】";	//获取每个option元素value的值
					}
					alert(str)
				});
			})	
		</script>
	</head>
	<body>
		<h3>表单对象属性过滤选择器</h3>
		 <button id="btn1">对表单内 可用input 赋值操作.</button>
  		 <button id="btn2">对表单内 不可用input 赋值操作.</button><br /><br />
		 <button id="btn3">获取多选框选中的个数.</button>
		 <button id="btn4">获取多选框选中的内容.</button><br /><br />
         <button id="btn5">获取下拉框选中的内容.</button><br /><br />
		 
		<form id="form1" action="#">			
			可用元素: <input name="add" value="可用文本框1"/><br>
			不可用元素: <input name="email" disabled="disabled" value="不可用文本框"/><br>
			可用元素: <input name="che" value="可用文本框2"/><br>
			不可用元素: <input name="name" disabled="disabled" value="不可用文本框"/><br>
			<br>
			
			多选框: <br>
			<input type="checkbox" name="newsletter" checked="checked" value="test1" />test1
			<input type="checkbox" name="newsletter" value="test2" />test2
			<input type="checkbox" name="newsletter" value="test3" />test3
			<input type="checkbox" name="newsletter" checked="checked" value="test4" />test4
			<input type="checkbox" name="newsletter" value="test5" />test5
			
			<br><br>
			下拉列表1: <br>
			<select name="test" multiple="multiple" style="height: 100px" id="sele1">
				<option>浙江</option>
				<option selected="selected">辽宁</option>
				<option>北京</option>
				<option selected="selected">天津</option>
				<option>广州</option>
				<option>湖北</option>
			</select>
			
			<br><br>
			下拉列表2: <br>
			<select name="test2">
				<option>浙江</option>
				<option>辽宁</option>
				<option selected="selected">北京</option>
				<option>天津</option>
				<option>广州</option>
				<option>湖北</option>
			</select>
		</form>		
	</body>
</html>
```



## 8	-	元素筛选方法

```html
 <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        $(document).ready(function () {
            function anmateIt() {
                $("#mover").slideToggle("slow", anmateIt);
            }

            anmateIt();
            /*
                过滤
                                eq() 获取给定索引的元素 功能跟 :eq() 一样
                first() 		获取第一个元素 							 功能跟 :first 一样
                last() 			获取最后一个元素 							功能跟 :last 一样
                filter(exp) 	留下匹配的元素
                is(exp) 		判断是否匹配给定的选择器，只要有一个匹配就返回，true
                has(exp) 		返回包含有匹配选择器的元素的元素 			   功能跟 :has 一样
                not(exp) 		删除匹配选择器的元素 					       功能跟 :not 一样
                children(exp) 	返回匹配给定选择器的子元素 			 功能跟 parent>child 一样
                find(exp) 		返回匹配给定选择器的后代元素 			功能跟 ancestor descendant 一样
                next() 			返回当前元素的下一个兄弟元素 			功能跟 prev + next 功能一样
                nextAll() 		返回当前元素后面所有的兄弟元素 		功能跟 prev ~ siblings 功能一样
                nextUntil() 	返回当前元素到指定匹配的元素为止的后面元素
                parent() 		返回父元素
                prev(exp) 		返回当前元素的上一个兄弟元素
                prevAll() 		返回当前元素前面所有的兄弟元素
                prevUnit(exp) 	返回当前元素到指定匹配的元素为止的前面元素
                siblings(exp) 	返回所有兄弟元素
    
                串联
                add() 把 add 匹配的选择器的元素添加到当前 jquery 对象中
    
    
                标签名选择器：表示是这个标签的属性值一样
                    例如：在body标签下选择所有id为123的div
                        $("body").children("div#123")		这个例子很明显可以知道它的用法
    
                补充：事件的函数中也是有this的，this表示当前的对象，也就是这个事件的对象，this.checked表示
                    当前的选中状态，返回true或false
    
                总结：①一定要清楚改变的是那个对象的值
                     ②分清楚得到的是dom对象还是JQ对象，因为两者的方法是不同的
            */

            //(1)eq()  选择索引值为等于 3 的 div 元素
            $("#btn1").click(function () {
                $("div").eq(3).css("background-color", "#bfa");
            });
            //(2)first()选择第一个 div 元素
            $("#btn2").click(function () {
                //first()   选取第一个元素
                $("div").first().css("background-color", "#bfa");
            });
            //(3)last()选择最后一个 div 元素
            $("#btn3").click(function () {
                //last()  选取最后一个元素
                $("div").last().css("background-color", "#bfa");
            });
            //(4)filter()在div中选择索引为偶数的
            $("#btn4").click(function () {
                //filter()  过滤   传入的是选择器字符串
                $("div").filter(":even").css("background-color", "#bfa");
            });
            //(5)is()判断#one是否为:empty或:parent
            //is用来检测jq对象是否符合指定的选择器
            $("#btn5").click(function () {
                alert($("#one").is(":parent"))
            });

            //(6)has()选择div中包含.mini的
            $("#btn6").click(function () {
                //has(selector)  选择器字符串    是否包含selector
                $("div").has(".mini").css("background-color", "#bfa");
            });
            //(7)not()选择div中class不为one的
            $("#btn7").click(function () {
                //not(selector)  选择不是selector的元素
                $("div").not(".one").css("background-color", "#bfa");
            });
            //(8)children()在body中选择所有class为one的div子元素
            $("#btn8").click(function () {
                //children()  选出所有的子元素
                $("body").children("div.one").css("background-color", "#bfa");
            });


            //(9)find()在body中选择所有class为mini的div元素
            $("#btn9").click(function () {
                //find()  选出所有的后代元素
                $("body").find("div.mini").css("background-color", "#bfa");
            });
            //(10)next() #one的下一个div
            $("#btn10").click(function () {
                //next()  选择下一个兄弟元素
                $("#one").next("div").css("background-color", "#bfa");
            });
            //(11)nextAll() #one后面所有的span元素
            $("#btn11").click(function () {
                //nextAll()   选出后面所有的元素
                $("#one").nextAll("div").css("background-color", "#bfa");
            });
            //(12)nextUntil() #one和span之间的元素
            $("#btn12").click(function () {
                //
                $("#one").nextUntil("span").css("background-color", "#bfa")
            });
            //(13)parent() .mini的父元素
            $("#btn13").click(function () {
                $(".mini").parent().css("background-color", "#bfa");
            });
            //(14)prev() #two的上一个div
            $("#btn14").click(function () {
                //prev()  
                $("#two").prev("div").css("background-color", "#bfa")
            });
            //(15)prevAll() span前面所有的div
            $("#btn15").click(function () {
                //prevAll()   选出前面所有的元素
                $("span").prevAll("div").css("background-color", "#bfa")
            });
            //(16)prevUntil() span向前直到#one的元素
            $("#btn16").click(function () {
                //prevUntil(exp)   找到之前所有的兄弟元素直到找到exp停止
                $("span").prevUntil("#one").css("background-color", "#bfa")
            });
            //(17)siblings() #two的所有兄弟元素
            $("#btn17").click(function () {
                //siblings()    找到所有的兄弟元素，包括前面的和后面的
                $("#two").siblings().css("background-color", "#bfa")
            });


            //(18)add()选择所有的 span 元素和id为two的元素
            $("#btn18").click(function () {

                //   $("span,#two,.mini,#one")
                $("span").add("#two")
                    .add(".mini").css("background-color", "#bfa");

            });
        });


    </script>
</head>
<body>
<input type="button" value="eq()选择索引值为等于 3 的 div 元素" id="btn1"/>
<input type="button" value="first()选择第一个 div 元素" id="btn2"/>
<input type="button" value="last()选择最后一个 div 元素" id="btn3"/>
<input type="button" value="filter()在div中选择索引为偶数的" id="btn4"/>
<input type="button" value="is()判断#one是否为:empty或:parent" id="btn5"/>
<input type="button" value="has()选择div中包含.mini的" id="btn6"/>
<input type="button" value="not()选择div中class不为one的" id="btn7"/>
<input type="button" value="children()在body中选择所有class为one的div子元素" id="btn8"/>
<input type="button" value="find()在body中选择所有class为mini的div后代元素" id="btn9"/>
<input type="button" value="next()#one的下一个div" id="btn10"/>
<input type="button" value="nextAll()#one后面所有的span元素" id="btn11"/>
<input type="button" value="nextUntil()#one和span之间的元素" id="btn12"/>
<input type="button" value="parent().mini的父元素" id="btn13"/>
<input type="button" value="prev()#two的上一个div" id="btn14"/>
<input type="button" value="prevAll()span前面所有的div" id="btn15"/>
<input type="button" value="prevUntil()span向前直到#one的元素" id="btn16"/>
<input type="button" value="siblings()#two的所有兄弟元素" id="btn17"/>
<input type="button" value="add()选择所有的 span 元素和id为two的元素" id="btn18"/>


<h3>基本选择器.</h3>
<br/><br/>
文本框<input type="text" name="account" disabled="disabled"/>
<br><br>
<div class="one" id="one">
    id 为 one,class 为 one 的div
    <div class="mini">class为mini</div>
</div>
<div class="one" id="two" title="test">
    id为two,class为one,title为test的div
    <div class="mini" title="other"><b>class为mini,title为other</b></div>
    <div class="mini" title="test">class为mini,title为test</div>
</div>

<div class="one">
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini"></div>
</div>
<div class="one">
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini">class为mini</div>
    <div class="mini" title="tesst">class为mini,title为tesst</div>
</div>
<div style="display:none;" class="none">style的display为"none"的div</div>
<div class="hide">class为"hide"的div</div>
<span id="span1">^^span元素 111^^</span>
<div>
    包含input的type为"hidden"的div<input type="hidden" size="8">
</div>
<span id="span2">^^span元素 222^^</span>
<div id="mover">正在执行动画的div元素.</div>
</body>
</html>
```









# 三、DOM操作

## 1	-	dom属性操作

```html
<script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
	<script type="text/javascript">
		/*
		 	dom属性操作
			 		html()		和dom对象的 innerHTML 功能一样
					text()		和dom对象的 innerText 功能一样
					不写就是获取值，写就是给标签赋值
					val()		设置和获取表单项的值
						设置：在括号里面给它赋值		获取：括号里面不写参数就可以了
						var([])：大括号里面可以赋多个值，单选，复选，下拉都可以一起放，之间用逗号隔开

					attr()：可以设置和获取属性值
						设置：要传两个参数，属性名和值
						不推荐操作：checked、readonly、selected、disabled等等
						还可以操作非标准的属性：比如自定义属性
					prop()：可以设置和获取属性值
						设置：一个是属性名，一个是true或false，true表示选中，false表示没有选中
						只推荐操作：checked、readonly、selected、disabled等等

					说明：attr()在不选中的时候返回的结果是undefined，官方认为这是一个错误，所以用
						prop()来弥补attr()的不足，prop()
		 */

		$(function(){
			$("#btn1").click(function(){
				alert($(":text").val());
			})

			$("btn2").click(function () {
				alert($(":checked").prop())
			})
		})

	</script>

</head>
<body>
<button id="btn1">获取文本框的name值</button>
<button id="btn2">获取多个表单项的值</button>
<form action="#" id="form1">
	文本框:<input  name="a" value="abc" type="text"/><br/>
	多选框:<input type="checkbox" checked=checked name="interest" value="篮球">
	<input type="checkbox" name="interest" value="zuqiu">
	<input type="checkbox" name="interest" value="乒乓">
	<input type="checkbox" name="interest" value="御马">
	<br/>
	向往的城市
	<select >
		<option selected="selected">北京</option>
		<option>上海</option>
		<option>深圳</option>
	</select>
</form>
</body>
</html>
```



## 2	-	dom增加删改

```html
  <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
        内部插入：
            appendTo()		a.appendTo(b)		把a添加到b后面，最后一个子元素
            prependTo()		a.prependTo(b)		把a添加到b前面，第一个子元素

        外部插入：
            insertAfter()	a.insertAfter(b)	把a插到b前面
            insertBefore()	a.insertBefore(b)	把a插到b后面

        替换：
            replaceWith()	a.replaceWith(b)	b替换所有a
            replaceAll()	a.replaceAll(b)		a替换所有b

        删除：
            remove()		a.remove()			删除整个a标签，文本也删除
            emtry()			a.emtry()			删除a标签里面的子元素（文本节点）

        补充：①a标签比较特殊，alter()返回的是这个超链接的地址
             ②confirm("提示的内容"):提示框函数，提交就会显示提示的内容，点击确定返回true，取消返回false
             ③调用函数的时候要注意this的指向是哪个对象


        */
        $(function () {
            $("#btn01").click(function () {
                //创建一个"广州"节点,添加到#city下[appendTo()]
                //子节点.appendTo(父节点)
                $("<li>广州</li>").appendTo("#city");	// 最后一个子节点
            });


            $("#btn02").click(function () {
                //创建一个"广州"节点,添加到#city下[prependTo()]
                $("<li>广州</li>").prependTo("#city");	// 第一个子节点
            });


            $("#btn03").click(function () {
                //将"广州"节点插入到#bj前面[insertBefore()]
                //前边.insertBefore(后边的)
                $("<li>广州</li>").insertBefore("#bj");
            });


            $("#btn04").click(function () {
                //将"广州"节点插入到#bj后面[insertAfter()]
                //后边.insertAfter(前边的)
                $("<li>广州</li>").insertAfter("#bj");

            });

            $("#btn05").click(function () {
                //使用"广州"节点替换#bj节点[replaceWith()]
                //被替换的.replaceWith()
                $("#bj").replaceWith("<li>广州</li>");

            });

            $("#btn06").click(function () {
                //使用"广州"节点替换#bj节点[replaceAll()]
                //新的节点.replaceAll(旧的节点)
                $("<li>广州</li>").replaceAll("#bj");

            });

            $("#btn07").click(function () {
                //删除#rl节点[remove()]
                $("#rl").remove();
            });

            $("#btn08").click(function () {
                //掏空#city节点[empty()]
                $("#city").empty();
            });

            $("#btn09").click(function () {
                //读取#city内的HTML代码
                alert($("#city").html());
            });

            $("#btn10").click(function () {
                //设置#bj内的HTML代码
                $("#bj").html("阳江");
            });
        });
    </script>
</head>
<body>
<div id="total">
    <div class="inner">
        <p>
            你喜欢哪个城市?
        </p>

        <ul id="city">
            <li id="bj">北京</li>
            <li>上海</li>
            <li>东京</li>
            <li>首尔</li>
        </ul>

        <br>
        <br>

        <p>
            你喜欢哪款单机游戏?
        </p>

        <ul id="game">
            <li id="rl">红警</li>
            <li>实况</li>
            <li>极品飞车</li>
            <li>魔兽</li>
        </ul>

        <br/>
        <br/>

        <p>
            你手机的操作系统是?
        </p>

        <ul id="phone">
            <li>IOS</li>
            <li id="android">Android</li>
            <li>Windows Phone</li>
        </ul>
    </div>

    <div class="inner">
        gender:
        <input type="radio" name="gender" value="male"/>
        Male
        <input type="radio" name="gender" value="female"/>
        Female
        <br>
        <br>
        name:
        <input type="text" name="name" id="username" value="abcde"/>
    </div>
</div>
<div id="btnList">
    <div>
        <button id="btn01">创建一个"广州"节点,添加到#city下[appendTo()]</button>
    </div>
    <div>
        <button id="btn02">创建一个"广州"节点,添加到#city下[prependTo()]</button>
    </div>
    <div>
        <button id="btn03">将"广州"节点插入到#bj前面[insertBefore()]</button>
    </div>
    <div>
        <button id="btn04">将"广州"节点插入到#bj后面[insertAfter()]</button>
    </div>
    <div>
        <button id="btn05">使用"广州"节点替换#bj节点[replaceWith()]</button>
    </div>
    <div>
        <button id="btn06">使用"广州"节点替换#bj节点[replaceAll()]</button>
    </div>
    <div>
        <button id="btn07">删除#rl节点[remove()]</button>
    </div>
    <div>
        <button id="btn08">掏空#city节点[empty()]</button>
    </div>
    <div>
        <button id="btn09">读取#city内的HTML代码</button>
    </div>
    <div>
        <button id="btn10">设置#bj内的HTML代码</button>
    </div>
</div>
</body>
</html>
```



## 3	-	CSS样式操作

```html
<style type="text/css">
	
	div{
		width:100px;
		height:260px;
	}
	div.whiteborder{
		border: 2px white solid;
	}
	div.redDiv{
		background-color: red;
	}
	div.blueBorder{
		border: 5px blue solid;
	}
	
</style>

<script type="text/javascript" src="script/jquery-1.7.2.js"></script>
<script type="text/javascript">

	/*
		样式操作：
			addClass()		添加样式		多个样式之间用空格隔开
			removeClass()	删除
			toggleClass()	有就删除，没有就添加
			offset()		设置和获取所在的位置
			设置：
				语法：dom对象.offset({
					top: 左上角到顶上的距离,		// Y轴
					left:右上角到左边边上的距离   // X轴
				})

			补充：JQ中的同一类方法的用法都差不多

			注意：添加类的时候不用带上.，添加样式和选择器不一样
	 */

	$(function(){
		
		var $divEle = $('div:first');
		
		$('#btn01').click(function(){
			//addClass() - 向被选元素添加一个或多个类
			$divEle.addClass("blueBorder").addClass("redDiv");
		});
		
		$('#btn02').click(function(){
			//removeClass() - 从被选元素删除一个或多个类 
			$divEle.removeClass("blueBorder");
		});
	
		$('#btn03').click(function(){
			//toggleClass() - 对被选元素进行添加/删除类的切换操作 
			$divEle.toggleClass();
		});
		
		$('#btn04').click(function(){
			// 修改top和left值
			$divEle.offset({
				top: 10,	// 可以看做是Y轴
				left:10		// 可以看做是X轴
			});
			//offset() - 返回第一个匹配元素相对于文档的位置。
			console.log($divEle.offset());
		});
	})
</script>
</head>
<body>

	<table align="center">
		<tr>
			<td>
				<div class="border">
				</div>
			</td>
			
			<td>
				<div class="btn">
					<input type="button" value="addClass()" id="btn01"/>
					<input type="button" value="removeClass()" id="btn02"/>
					<input type="button" value="toggleClass()" id="btn03"/>
					<input type="button" value="offset()" id="btn04"/>
				</div>
			</td>
		</tr>
	</table>

	<br /> <br />
	<br /> <br />
</body>
</html>
```



## 4	-	事件

**事件操作**

```html
    <script type="text/javascript" src="../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
            事件操作
                ①JQ事件和原生JS事件放在一起谁先执行？
                    JQ会先执行，JS后执行
                ②为什么会这样
                    JQ加载：JQ事件在完成加载浏览器之后执行
                    JS加载：原生JS事件在完成加载浏览器之后，
                            除了要等浏览器内核解析创建好的dom对象，还要等待标签显示加载完成

                ③执行的次数：
                    原生JS的页面加载完成之后，只会执行最后一个赋值函数，就像给变量赋值一样
                    JQ会将事件记录下来，然后依次执行，就是说可以执行多个
         */

        // 代码实现
        window.onload = function () {
            alert("我是原生JS的事件1")
        }
        window.onload = function () {
            alert("我是原生JS的事件2")
        }
        window.onload = function () {
            alert("我是原生JS的事件3")
        }

        $(function () {
            alert("我是JQ的事件")
        })
        $(function () {
            alert("我是JQ的事件2")
        })
        $(function () {
            alert("我是JQ的事件3")
        })
    </script>
```



**JQ中的其他的事件处理方法**

```html
    <script type="text/javascript" src="../../script/jquery-1.7.2.js"></script>
    <script type="text/javascript">

        /*
            ①click()		绑定单击事件，触发单击事件，传函数参数是绑定，不传就是触发事件
                            触发事件：执行事件
				
				②blur()、change()、submit()和JS中的事件一样功能
				
				③mouseover()	鼠标移入
				
				④mouseout()		鼠标移出
				
				⑤bind()			可以给元素一次性绑定一个或者多个事件
					第一个参数：事件名，多个事件用空格隔开
					第二个参数：函数
					注意：一个引号引起来就好了，然后多个事件用空格隔开
				
				⑥one()		使用跟bind()一样，但是one()事件绑定的事件只响应一次
				
				⑦unbind()	跟bind()相反的操作，解除事件绑定，多个绑定用空格隔开，没有参数表示全部解除
					注意：因为unbind是解除绑定的操作，所以不用给函数它
				
				⑧live()		用来绑定事物，它可以用来绑定选择器匹配的所有元素的事件，
                哪怕这个元素是后面动态创建出来的也有效，意思是只要是这个元素就一定绑定这个事件，除非解除。
         */
        $(function () {
        	// 单击事件
            $("h5").click(function () {
				alert("我被单击了")
            });

			// 触发事件
			$("#btn").click(function () {
				$("h5").click();	// 单击按钮的时候会触发h5绑定的事件，这就是触发事件
			});

			// // 鼠标移入事件
			// $("h5").mouseover(function () {
			// 	console.log("鼠标移入了")
			// });

			// // 鼠标移出事件
			// $("h5").mouseout(function () {
			// 	console.log("鼠标移出了")
			// });
			// bind()事件
			$("h5").bind("mouseover mouseout",function () {
				console.log("bind事件被触发了")
			});

			// one()事件
			$("h5").one("mouseover mouseout",function () {
				console.log("one()事件被触发了")
			})

			// unbind()
			$("h5").unbind("mouseover mouseout")

			// live()事件
			$("button").live("click ",function () {
				alert("live()事件被触发了")
			})
			// 动态添加button标签
			$("<button id='btn2'>按钮2</button>").appendTo("body div:first");
        });

    </script>
```

​	

**事件的冒泡**

```html
	<script type="text/javascript" src="jquery-1.7.2.js"></script>
		<script type="text/javascript">
			/*
				事件的冒泡：冒泡就是事件的向上传导，子元素和父元素绑定同样的事件，
						   子元素的事件被触发，父元素的响应事件也会触发

				阻止事件的冒泡：子元素里面return false;
					比如 a标签的 跳转
			 */
			$(function(){
				$("#content").click(function () {
					alert("我是div");
				});

				$("span").click(function () {
					alert("我是span");
					return false;
				});

				$("a").click(function () {
					return false;	// 就不会跳转了
				});
			});
		</script>
```



**事件对象**

```html
	<script type="text/javascript" src="jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
        	事件对象：当我们发生一个事件以后，某个事件其实就是一个对象
        		获取原生JS的事件对象：事件的函数里面传个参数event
        		获取JQ的事件对象：和JS的获取方式一样


        		补充：curdate()		当前时间
         */
        window.onload = function () {
            var areaDiv = document.getElementById("areaDiv");
            var showMsg = document.getElementById("showMsg");
            // JS获取事件对象
            // 鼠标移入事件
            areaDiv.onmouseover = function (event) {
                var x = event.pageX;
                var y = event.pageY;
            };
        };
    </script>
</head>
<body>
<div id="areaDiv"></div>
<div id="showMsg"></div>
</body>
</html>
```





## 5	-	动画

```html
<script type="text/javascript" src="script/jquery-1.7.2.js"></script>
    <script type="text/javascript">
        /*
		    动画：
		        show()          将隐藏变成显示的
		        hidder()        将显示变成隐藏的
		        toggle()        可见的就隐藏，隐藏的就可见
            淡入淡出动画：
                fadeIn()        淡入（慢慢可见）
                fadeOut()       淡出（慢慢消失）
                fadeToggle()    淡入/淡出
                fadeTo()        在指定时间内慢慢的将透明度修改到指定的值
                    第一个参数是"slow"
                    第二个参数是透明度       0：透明    0.5：半透明
                    第三个参数是回调函数

		        以上动画方法都可以添加参数
		            ①第一个参数：执行的时长，以毫秒为单位
		            ②第二个参数：动画的回调函数，就是动画结束之后执行的函数

        */
        $(function () {
            //显示   show()
            $("#btn1").click(function () {
                $("#div1").show();
            });
            //隐藏  hide()
            $("#btn2").click(function () {
                $("#div1").hide();
            });
            //切换   toggle()
            $("#btn3").click(function () {
                $("#div1").toggle();
            });

            //淡入   fadeIn()
            $("#btn4").click(function () {
                $("#div1").fadeIn();
            });
            //淡出  fadeOut()
            $("#btn5").click(function () {
                $("#div1").fadeOut();
            });

            //淡化到  fadeTo()
            $("#btn6").click(function () {
                $("#div1").fadeTo("slow",0);
            });
            //淡化切换  fadeToggle()
            $("#btn7").click(function () {
                $("#div1").fadeToggle();
            });
        })
    </script>

</head>
<body>
<table style="float: left;">
    <tr>
        <td>
            <button id="btn1">显示show()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn2">隐藏hide()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn3">显示/隐藏切换 toggle()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn4">淡入fadeIn()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn5">淡出fadeOut()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn6">淡化到fadeTo()</button>
        </td>
    </tr>
    <tr>
        <td>
            <button id="btn7">淡化切换fadeToggle()</button>
        </td>
    </tr>
</table>

<div id="div1" style="float:left;border: 1px solid;background-color: blue;width: 300px;height: 200px;">
    jquery动画定义了很多种动画效果，可以很方便的使用这些动画效果
</div>
</body>
</html>
```



