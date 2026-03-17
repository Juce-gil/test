<template>
  <div class="user-product">
    <!-- 查询栏 -->
    <el-card shadow="never" class="query-card">
      <el-row :gutter="12" class="query-row">
        <el-col :span="5">
          <el-select
            size="small"
            v-model="query.isBargain"
            placeholder="全部"
            clearable
            style="width: 100%;"
          >
            <el-option label="全部" :value="null"></el-option>
            <el-option label="支持砍价" :value="true"></el-option>
            <el-option label="不支持砍价" :value="false"></el-option>
          </el-select>
        </el-col>
        <el-col :span="5">
          <el-select
            size="small"
            v-model="query.categoryId"
            placeholder="商品类别"
            clearable
            style="width: 100%;"
          >
            <el-option
              v-for="item in categoryList"
              :key="item.id"
              :label="item.name"
              :value="item.id"
            />
          </el-select>
        </el-col>
        <el-col :span="6">
          <el-input
            size="small"
            v-model="query.name"
            placeholder="商品名称"
            clearable
            style="width: 100%;"
            @keyup.enter.native="handleFilter"
          />
        </el-col>
        <el-col :span="4">
          <el-button size="small" type="primary" icon="el-icon-search" @click="handleFilter">查询</el-button>
          <el-button size="small" @click="handleFilterClear">重置</el-button>
        </el-col>
      </el-row>
    </el-card>

    <!-- 商品列表 -->
    <el-card shadow="never" class="list-card">
      <div v-if="loading" class="loading-wrap">
        <i class="el-icon-loading"></i> 加载中...
      </div>
      <div v-else-if="!productList.length" class="empty-wrap">
        暂无商品
      </div>
      <el-row v-else :gutter="16" class="product-grid">
        <el-col
          v-for="item in productList"
          :key="item.id"
          :xs="12"
          :sm="8"
          :md="6"
          class="product-col"
        >
          <div class="product-card" @click="goDetail(item)">
            <div class="card-cover">
              <el-image
                v-if="item.coverList"
                :src="getImageUrl(item.coverList)"
                fit="cover"
                class="cover-img"
              >
                <div slot="error" class="cover-error">暂无图片</div>
              </el-image>
              <div v-else class="cover-placeholder">暂无图片</div>
              <span v-if="item.isBargain === true || item.isBargain === 1" class="tag-bargain">支持砍价</span>
            </div>
            <div class="card-body">
              <div class="card-name" :title="item.name">{{ item.name }}</div>
              <div class="card-meta">
                <span class="card-category">{{ getCategoryName(item.categoryId) }}</span>
                <span class="card-old">新旧 {{ item.oldLevel || '-' }}/9</span>
              </div>
              <div class="card-price">¥ {{ item.price != null ? item.price : '-' }}</div>
              <div class="card-publisher">
                <el-avatar
                  :size="24"
                  :src="getPublisherAvatar(item)"
                  class="publisher-avatar"
                >
                  <i class="el-icon-user-solid"></i>
                </el-avatar>
                <span class="publisher-name">{{ getPublisherName(item) }}</span>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>

      <el-pagination
        v-if="total > 0"
        class="pagination"
        :current-page="currentPage"
        :page-sizes="[12, 24, 48]"
        :page-size="pageSize"
        :total="total"
        layout="total, sizes, prev, pager, next"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </el-card>
  </div>
</template>

<script>
import { toFullImageUrl } from '@/utils/imageUrl'

export default {
  name: 'Product',
  data() {
    return {
      query: {
        name: '',
        categoryId: null,
        isBargain: null
      },
      productList: [],
      categoryList: [],
      currentPage: 1,
      pageSize: 12,
      total: 0,
      loading: false
    };
  },
  created() {
    this.fetchCategoryList();
    this.fetchProductList();
  },
  methods: {
    getImageUrl(url) {
      return toFullImageUrl(url)
    },
    getCategoryName(categoryId) {
      if (categoryId == null) return '-';
      const item = this.categoryList.find(c => c.id === categoryId);
      return item ? item.name : String(categoryId);
    },
    getPublisherName(item) {
      if (!item) return '-';
      if (item.userName) return item.userName;
      if (item.userNickName) return item.userNickName;
      if (item.userId != null) return String(item.userId);
      return '-';
    },
    getPublisherAvatar(item) {
      if (!item) return '';
      if (item.userAvatar) {
        return this.getImageUrl(item.userAvatar);
      }
      return '';
    },
    fetchCategoryList() {
      this.$axios
        .post('/category/query', { size: 200 })
        .then(res => {
          const { data } = res;
          if (data && data.code === 200 && Array.isArray(data.data)) {
            this.categoryList = (data.data || []).filter(
              item => item.isUse === true || item.isUse === 1
            );
          } else {
            this.categoryList = [];
          }
        })
        .catch(() => {
          this.categoryList = [];
        });
    },
    fetchProductList() {
      this.loading = true;
      const body = {
        name: this.query.name || undefined,
        current: this.currentPage,
        size: this.pageSize
      };
      if (this.query.categoryId != null && this.query.categoryId !== '') {
        body.categoryId = Number(this.query.categoryId);
      }
      if (this.query.isBargain != null && this.query.isBargain !== '') {
        body.isBargain = this.query.isBargain === true || this.query.isBargain === 1 ? 1 : 0;
      }
      this.$axios
        .post('/product/query', body)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.productList = data.data || [];
            this.total = data.total != null ? data.total : 0;
          } else {
            this.productList = [];
            this.total = 0;
          }
        })
        .catch(() => {
          this.productList = [];
          this.total = 0;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    handleFilter() {
      this.currentPage = 1;
      this.fetchProductList();
    },
    handleFilterClear() {
      this.query.name = '';
      this.query.categoryId = null;
      this.query.isBargain = null;
      this.currentPage = 1;
      this.fetchProductList();
    },
    handleSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
      this.fetchProductList();
    },
    handleCurrentChange(val) {
      this.currentPage = val;
      this.fetchProductList();
    },
    goDetail(item) {
      if (item && item.id != null) {
        this.$router.push({ path: '/product/detail', query: { id: item.id } });
      }
    }
  }
};
</script>

<style scoped lang="scss">
.user-product {
  padding: 0 0 20px;
}
.query-card {
  margin-bottom: 16px;
}
.query-row {
  align-items: center;
}
.list-card {
  min-height: 200px;
}
.loading-wrap,
.empty-wrap {
  text-align: center;
  padding: 40px;
  color: #909399;
}
.product-grid {
  margin-bottom: 20px;
}
.product-col {
  margin-bottom: 16px;
}
.product-card {
  border: 1px solid #ebeef5;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
  &:hover {
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  }
}
.card-cover {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f5f7fa;
  overflow: hidden;
}
.cover-img {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.cover-img ::v-deep .el-image__inner {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.cover-error,
.cover-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 12px;
}
.tag-bargain {
  position: absolute;
  top: 8px;
  right: 8px;
  padding: 2px 8px;
  background: #e6a23c;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}
.card-body {
  padding: 12px;
}
.card-name {
  font-size: 14px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
}
.card-meta {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}
.card-category {
  margin-right: 8px;
}
.card-price {
  font-size: 16px;
  color: #f56c6c;
  font-weight: 500;
  margin-bottom: 8px;
}
.card-publisher {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 4px;
}
.publisher-avatar {
  flex-shrink: 0;
}
.publisher-name {
  font-size: 12px;
  color: #909399;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  flex: 1;
}
.pagination {
  margin-top: 16px;
  text-align: right;
}
</style>
