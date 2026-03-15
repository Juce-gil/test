<template>
    <div>
        <div class="nav-category">
            <div class="left">
                <span class="tab"
                    :class="{ active: categorySelectedItem.name === isUseCategory.name }"
                    @click="categorySelected(isUseCategory)" :key="index"
                    v-for="(isUseCategory, index) in isUseCategoryList">
                    {{ isUseCategory.name }}
                </span>
            </div>
            <div class="right">
                <div class="filter-group">
                    <span class="bargain">
                        <span class="pill" :class="{ active: bargainSelectedItem.name === bargain.name }"
                            @click="bargainSelected(bargain)" v-for="(bargain, index) in bargainStatus" :key="index">
                            {{ bargain.name }}
                        </span>
                    </span>
                </div>
                <div class="filter-group">
                    <el-date-picker class="filter-control" style="width: 220px;" @change="fetchFreshData" size="small"
                    v-model="searchTime" type="daterange" range-separator="至" start-placeholder="发布开始"
                    end-placeholder="发布结束">
                    </el-date-picker>
                    <el-select class="filter-control" style="width: 120px;" @change="fetchFreshData" size="small"
                    v-model="productQueryDto.categoryId" placeholder="商品类别">
                    <el-option v-for="item in categoryList" :key="item.id" :label="item.name" :value="item.id">
                    </el-option>
                    </el-select>
                </div>
            </div>
        </div>
        <div class="product-list">
            <el-row v-if="productList.length === 0">
                <el-empty description="暂无商品信息"></el-empty>
            </el-row>
            <el-row v-else :gutter="16">
                <el-col @click.native="route(product)" :span="6" v-for="(product, index) in productList" :key="index">
                    <div class="item-product">
                        <div class="cover">
                            <img :src="coverListParse(product)" alt="" srcset="">
                        </div>
                        <div class="meta-row">
                            <span class="bargain-tag" :class="{ off: !product.isBargain }">{{ product.isBargain ? '支持砍价' : '不支持砍价' }}</span>
                            <span class="love">{{ product.likeNumber }}人想要</span>
                        </div>
                        <div class="title">
                            {{ product.name }}
                        </div>
                        <div class="price-row">
                            <span class="decimel-symbol">¥</span>
                            <span class="price">{{ product.price }}</span>
                        </div>
                        <div class="info">
                            <img :src="product.userAvatar" alt="" srcset="">
                            <span>{{ product.userName }}</span>
                        </div>
                    </div>
                </el-col>
            </el-row>
        </div>
    </div>
</template>
<script>
export default {
    name: 'Product',
    data() {
        return {
            categoryList: [], // 存储的商品类别数组
            isUseCategoryList: [], // 存储的启用的类别数组
            categorySelectedItem: {},
            productQueryDto: {}, // 商品查询条件类
            productList: [],// 存储后端返回的商品数据列表
            bargainSelectedItem: {},
            searchTime: [],
            bargainStatus: [{ isBargain: null, name: '全部' }, { isBargain: true, name: '支持砍价' }, { isBargain: false, name: '不支持砍价' }]

        };
    },
    created() {
        this.fetchFreshData();
        this.fetchCategoryList();
        // 页面加载时，默认不启用砍价查询条件
        this.bargainSelected(this.bargainStatus[0]);
    },
    methods: {
        route(product) {
            // 跳转商品详情
            this.$router.push('/product-detail?productId=' + product.id);
        },
        coverListParse(product) {
            if (product.coverList === null) {
                return;
            }
            const newCoverList = product.coverList.split(',');
            return newCoverList[0];
        },
        /**
         * 商品砍价选中事件
         * @param {*} bargain 
         */
        bargainSelected(bargain) {
            this.bargainSelectedItem = bargain;
            this.productQueryDto.isBargain = bargain.isBargain;
            this.fetchFreshData();
        },
        /**
         * 查询商品数据
         */
        async fetchFreshData() {
            let startTime = null;
            let endTime = null;
            if (this.searchTime != null && this.searchTime.length === 2) {
                const [startDate, endDate] = await Promise.all(this.searchTime.map(date => date.toISOString()));
                startTime = `${startDate.split('T')[0]}T00:00:00`;
                endTime = `${endDate.split('T')[0]}T23:59:59`;
            }
            // this.productQueryDto.current = this.currentPage;
            // this.productQueryDto.size = this.pageSize;
            this.productQueryDto.startTime = startTime;
            this.productQueryDto.endTime = endTime;
            this.$axios.post('/product/query', this.productQueryDto).then(res => {
                const { data } = res; // 解构
                if (data.code === 200) {
                    this.productList = data.data;
                }
            }).catch(error => {
                console.log("商品查询异常：", error);
            })
        },
        /**
         * 商品类别选中事件
         * @param {*} category 
         */
        categorySelected(category) {
            this.categorySelectedItem = category;
            this.productQueryDto.categoryId = category.id;
            // 查询对应的商品分类下面的商品数据
            this.fetchFreshData();
        },
        /**
         * 加载商品类别数据
         */
        fetchCategoryList() {
            this.$axios.post('/category/query', {}).then(res => {
                const { data } = res; // 解构
                if (data.code === 200) {
                    this.categoryList = data.data;
                    this.isUseCategoryList = data.data.filter(category => category.isUse);
                    this.isUseCategoryList.unshift({ id: null, name: '全部' });
                    this.categorySelected(this.isUseCategoryList[0]);
                }
            }).catch(error => {
                console.log("商品类别查询异常：", error);
            })
        },
    }
};
</script>
<style scoped lang="scss">
.cover {
    img {
        width: 100%;
        height: 200px;
        border-radius: 12px;
        object-fit: cover;
    }
}

.meta-row{
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    padding-top: 10px;
}

.title {
    font-size: 16px;
    line-height: 22px;
    color: #1f1f1f;
    font-weight: 700;
    margin-top: 6px;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
}

.decimel-symbol {
    font-size: 14px;
    color: #ff4f24;
    font-weight: 800;
}

.price-row{
    display: flex;
    align-items: baseline;
    gap: 4px;
    padding: 10px 0 12px 0;
}
.price {
    font-size: 24px;
    color: #ff4f24;
    font-weight: 800;
    margin-right: 6px;
}

.love {
    font-size: 14px;
    color: #999;
}

.bargain-tag{
    font-size: 12px;
    font-weight: 700;
    background-color: rgba(245, 194, 66, 0.22);
    color: rgb(123, 88, 10);
    border: 1px solid rgba(245, 194, 66, 0.35);
    border-radius: 999px;
    padding: 2px 10px;
    line-height: 18px;
    white-space: nowrap;
}
.bargain-tag.off{
    background-color: rgba(153, 153, 153, 0.12);
    border: 1px solid rgba(153, 153, 153, 0.18);
    color: rgba(0,0,0,0.55);
}

.info {
    display: flex;
    justify-content: left;
    align-items: center;
    gap: 8px;
    padding-top: 8px;

    img {
        width: 28px;
        height: 28px;
        border-radius: 50%;
        object-fit: cover;
    }

    span {
        font-size: 13px;
        color: rgba(0,0,0,0.65);
        font-weight: 600;
    }
}

.bargain {
    display: inline-block;
    font-size: 12px;
    background-color: rgb(246, 246, 246);
    line-height: 24px;
    padding-inline: 10px;
    padding-block: 4px;
    border-radius: 999px;
    cursor: pointer;

    span {
        display: inline-block;
        padding-inline: 10px;
        border-radius: 999px;
    }
}
.pill{
    color: rgba(0,0,0,0.65);
    user-select: none;
}
.pill.active{
    color: rgb(255, 255, 255);
    background-color: rgb(51, 51, 51);
}

.product-list {
    padding-block: 20px;

    .item-product {
        padding: 12px 12px 14px 12px;
        box-sizing: border-box;
        border-radius: 16px;
        transition: all .25s ease;
        cursor: pointer;
        background: rgb(255,255,255);
        border: 1px solid rgba(0,0,0,0.06);
        box-shadow: 0 1px 2px rgba(0,0,0,0.04);
        height: 100%;
    }

    .item-product:hover {
        transform: translateY(-2px);
        box-shadow: 0 10px 22px rgba(0, 0, 0, 0.10), 0 2px 6px rgba(0, 0, 0, 0.06);
    }
}

.nav-category {
    display: flex;
    justify-content: space-between;
    align-items: flex-start;
    gap: 18px;

    .left {
        display: flex;
        justify-content: left;
        gap: 8px;
        flex-wrap: wrap;

        .tab {
            display: inline-flex;
            align-items: center;
            background-color: rgba(0,0,0,0.04);
            padding: 8px 16px;
            cursor: pointer;
            border-radius: 999px;
            user-select: none;
            font-size: 13px;
            color: rgba(0,0,0,0.65);
        }

        .tab:hover {
            background-color: rgba(0,0,0,0.06);
        }
        .tab.active{
            background-color: rgb(255, 209, 80);
            color: rgb(51, 51, 51);
            font-weight: 800;
        }
    }

    .right{
        display: flex;
        align-items: center;
        justify-content: flex-end;
        gap: 10px;
        flex-wrap: wrap;
    }
    .filter-group{
        display: flex;
        align-items: center;
        gap: 8px;
        padding-left: 10px;
        border-left: 1px solid rgba(0,0,0,0.06);
    }
    .filter-group:first-child{
        border-left: none;
        padding-left: 0;
    }
    .filter-control :deep(.el-input__inner){
        border-radius: 12px;
        height: 34px;
        line-height: 34px;
    }
}
</style>