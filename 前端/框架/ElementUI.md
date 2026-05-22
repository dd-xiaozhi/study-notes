# 使用ElemnetUI的小知识

## 选择器

### 选择框中使用树状数据类型

![image-20240411180241326](D:\学习笔记\img\img01\image-20240411180241326.png) 

这里用到了两个结构：

- 选择器
- 树形控件

**实现**

1. 我们要将数据放入到树状控件中，所以没有对应的 Option 组件中，因此我们可以使用 Select 组件的 empty 插槽，这个是没有子Ootion 的时候就会显示
2. 当树形控件中选择了对应的节点，这个时候我们可以触发它的点击方法
3. 使用 ref 获取 Select 组件的实例，在点击方法(第二点)中触发隐藏方法

```html
<el-select
    popper-class="custom-popper-class"
    ref="selecterRef"
    v-model="deptParams.parentDept.name"
    placeholder="选择上级部门"
    clearable
>
    <template #empty>
      <div class="select-tree-con">
        <el-tree
          class="custom-tree-class"
          :data="data"
          :props="DialogTreeProps"
          default-expand-all
          :expand-on-click-node="false"
          @node-click="xxx"
        />
      </div>
    </template>
</el-select>
<script setup lang="ts">
import { ref, reactive } from 'vue'
const data = [......]
/**
 * 弹窗
 */
let deptParams = reactive<any>({
  parentDept: { id: 1, name: '' },
})
// Tree DOM
let selecterRef = ref()
// Tree的配置项对应的数据名
const DialogTreeProps = { children: 'children', label: 'label' }
// 点击Tree中的节点的回调
const handleDialogNodeClick = (node: any) => {
  deptParams.parentDept.name = node.label
  // 触发 blur 方法让下拉框隐藏 -> 可以翻一下官网，有说明
  selecterRef.value.blur()
}
</script>
<style lang="scss" scoped>
.select-tree-con {
  height: 200px !important;
}
.custom-tree-class {
  max-height: 300px;
  overflow: scroll;
}
</style>
```



