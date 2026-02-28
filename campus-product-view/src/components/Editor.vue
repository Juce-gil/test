<template>
    <div>
        <Toolbar style="border-bottom: 1px solid #eae8e8;" :editor="editor" :defaultConfig="toolbarConfig"
            :mode="mode" />
        <Editor :style="{ height: height, overflowY: 'hidden' }" v-model="content" :defaultConfig="editorConfig"
            :mode="mode" @onCreated="onCreated" />
    </div>
</template>
<script>
import Vue from 'vue';
import { Editor, Toolbar } from '@wangeditor/editor-for-vue';
import { API_BASE_URL } from '@/utils/request';
export default Vue.extend({
    components: { Editor, Toolbar },
    props: {
        receiveContent: {
            type: String,
            default: '',
            required: true
        },
        height: {
            type: String,
            default: 'calc(100vh - 100px)'
        }
    },
    data() {
        return {
            editor: null,
            content: '<p>创作内容</p>',
            toolbarConfig: {},
            editorConfig: {
                placeholder: '请输入内容...',
                MENU_CONF: {
                    uploadImage: {
                        server: API_BASE_URL + '/file/upload',
                        fieldName: 'file',
                        maxFileSize: 10 * 1024 * 1024,
                        maxNumberOfFiles: 10,
                        // allowedFileTypes: ['image/*'],
                        metaWithUrl: false,
                        withCredentials: true,
                        timeout: 10 * 1000,
                        headers: {
                            'token': sessionStorage.getItem('token')
                        },
                        customInsert(res, insertFn) {
                            insertFn(res.data, res.data, res.data);
                        },
                    },
                }
            },
            mode: 'default',
        }
    },
    methods: {
        onCreated(editor) {
            this.editor = Object.seal(editor);
            this.toolbarConfig.excludeKeys = ['group-video','group-image'];
        },
    },
    watch: {
        receiveContent: {
            handler(v1, v2) {
                console.log("接收内容：", v1);
                this.content = v1;
            },
            deep: true, // 启用深度监听  
            immediate: true
        },
        content(newVal, oldVal) {
            this.$emit('on-receive', newVal);
        },
    },
    beforeDestroy() {
        const editor = this.editor;
        if (editor == null) return;
        editor.destroy();
    }
})
</script>
<style src="@wangeditor/editor/dist/css/style.css"></style>
<style scoped>
.line-number {
    display: block;
    margin-right: 10px;
    /* 以下样式确保行号不被选中或复制 */
    pointer-events: none;
    user-select: none;
    -webkit-user-select: none;
    color: #999;
    /* 行号颜色，可自定义 */
    text-align: right;
    /* 行号对齐方式 */
}
</style>
```

<h1 id="rxGCi">后端</h1>
使用下列的新增语句替换ProductMapper.xml里面的商品新增SQL

![](https://cdn.nlark.com/yuque/0/2024/png/32649796/1734090641680-a6241eda-29b2-443a-a5fb-d56c11c6ce24.png)

```xml
<insert id="save">
  INSERT INTO product (name, detail, cover_list, old_level, category_id, user_id, inventory, price, is_bargain,
  create_time)
  VALUES (#{name}, #{detail}, #{coverList}, #{oldLevel}, #{categoryId}, #{userId}, #{inventory}, #{price},
  #{isBargain}, #{createTime})
</insert>