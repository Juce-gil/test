<template>
  <div class="product-detail">
    <el-card v-loading="loading" shadow="never" class="detail-card">
      <div v-if="!loading && !product.id" class="empty">商品不存在或已下架</div>
      <template v-else-if="product.id">
        <el-row :gutter="32" class="detail-row">
          <!-- 左侧：商品图片 -->
          <el-col :span="12">
            <div class="image-area">
              <el-image
                v-if="coverUrl"
                :src="coverUrl"
                fit="contain"
                class="main-image"
              >
                <div slot="error" class="image-error">加载失败</div>
              </el-image>
              <div v-else class="image-placeholder">暂无图片</div>
              <span v-if="isBargain" class="tag-bargain">支持砍价</span>
            </div>
          </el-col>
          <!-- 右侧：价格与信息 + 收藏按钮 -->
          <el-col :span="12">
            <div class="info-area">
              <div class="info-header">
                <h1 class="product-name">{{ product.name }}</h1>
                <el-button
                  :type="collected ? 'warning' : 'default'"
                  :icon="collected ? 'el-icon-star-on' : 'el-icon-star-off'"
                  class="btn-favorite"
                  @click="toggleFavorite"
                >
                  {{ collected ? "取消收藏" : "收藏" }}
                </el-button>
              </div>
              <div class="block block-publisher">
                <span class="section-label">发布者</span>
                <span class="publisher-name">{{ publisherName }}</span>
              </div>
              <div class="block block-price">
                <span class="section-label section-label-lg">价格</span>
                <span class="price"
                  >¥ {{ product.price != null ? product.price : "-" }}</span
                >
              </div>
              <div class="block block-info">
                <span class="section-label section-label-lg">基础信息</span>
                <div class="info-grid">
                  <div class="info-item">
                    <span class="info-item-label">分类</span>
                    <span>{{ categoryName }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-item-label">新旧程度</span>
                    <span>{{
                      product.oldLevel != null ? product.oldLevel + " / 9" : "-"
                    }}</span>
                  </div>
                  <div class="info-item">
                    <span class="info-item-label">库存</span>
                    <span>{{
                      product.inventory != null ? product.inventory : "-"
                    }}</span>
                  </div>
                  <div class="info-item info-item-bargain">
                    <span class="info-item-label">是否支持砍价</span>
                    <span :class="{ 'bargain-yellow': isBargain }">{{
                      isBargain ? "支持" : "不支持"
                    }}</span>
                  </div>
                </div>
              </div>
              <div v-if="product.detail" class="block block-desc">
                <span class="section-label section-label-lg">商品描述</span>
                <p class="detail-text">{{ product.detail }}</p>
              </div>
              <div class="actions">
                <el-button
                  type="primary"
                  size="medium"
                  icon="el-icon-chat-dot-round"
                  @click="handleIWant"
                  >我想要</el-button
                >
                <el-button
                  type="danger"
                  size="medium"
                  icon="el-icon-goods"
                  @click="openOrderDialog"
                  >立即购买</el-button
                >
              </div>
            </div>
          </el-col>
        </el-row>
      </template>
    </el-card>

    <!-- 下单弹窗 -->
    <el-dialog
      title="商品下单"
      :visible.sync="orderDialogVisible"
      width="480px"
      :close-on-click-modal="false"
    >
      <div class="order-dialog-body">
        <div class="order-summary">
          <div class="order-title">{{ product.name }}</div>
          <div class="order-meta">
            <span class="order-price-label">单价：</span>
            <span class="order-price"
              >¥ {{ product.price != null ? product.price : "-" }}</span
            >
            <span class="order-stock"
              >库存：{{
                product.inventory != null ? product.inventory : "-"
              }}</span
            >
          </div>
        </div>
        <div class="order-field">
          <span class="order-label">下单数量</span>
          <el-input-number
            v-model="orderQuantity"
            :min="1"
            :max="product.inventory || 1"
            label="请选择数量"
          />
        </div>
        <div class="order-field">
          <span class="order-label">备注信息</span>
          <el-input
            type="textarea"
            :rows="3"
            v-model="orderRemark"
            placeholder="补充一些与卖家沟通的信息（可选）"
          />
        </div>
      </div>
      <span slot="footer" class="dialog-footer">
        <el-button @click="orderDialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="submitOrder">确认下单</el-button>
      </span>
    </el-dialog>
  </div>
</template>

<script>
import { toFullImageUrl } from "@/utils/imageUrl";
import { getUserInfo } from "@/utils/storage";

export default {
  name: "ProductDetail",
  data() {
    return {
      product: {},
      categoryList: [],
      loading: false,
      collected: false,
      orderDialogVisible: false,
      orderQuantity: 1,
      orderRemark: ""
    };
  },
  computed: {
    productId() {
      const id =
        this.$route.query.id != null && this.$route.query.id !== ""
          ? this.$route.query.id
          : this.$route.query.productId;
      return id != null && id !== "" ? Number(id) : null;
    },
    coverUrl() {
      const url = this.product.coverList;
      const firstUrl = url ? String(url).split(",")[0] : "";
      return toFullImageUrl(firstUrl || "");
    },
    categoryName() {
      const id = this.product.categoryId;
      if (id == null) return "-";
      const item = this.categoryList.find(c => c.id === id);
      return item ? item.name : String(id);
    },
    publisherName() {
      return (
        this.product.userName ||
        this.product.userNickName ||
        (this.product.userId != null ? String(this.product.userId) : "-")
      );
    },
    isBargain() {
      return this.product.isBargain === true || this.product.isBargain === 1;
    }
  },
  watch: {
    productId() {
      this.fetchDetail();
      this.loadFavoriteStatus();
    }
  },
  created() {
    this.fetchCategoryList();
    this.fetchDetail();
    this.loadFavoriteStatus();
  },
  methods: {
    getImageUrl(url) {
      return toFullImageUrl(url || "");
    },
    fetchCategoryList() {
      this.$axios
        .post("/category/query", { size: 200 })
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
    fetchDetail() {
      if (this.productId == null || isNaN(this.productId)) {
        this.product = {};
        this.collected = false;
        return;
      }
      this.loading = true;
      this.$axios
        .post("/product/query", { id: this.productId, current: 1, size: 1 })
        .then(res => {
          const { data } = res;
          if (
            data &&
            data.code === 200 &&
            Array.isArray(data.data) &&
            data.data.length
          ) {
            this.product = data.data[0];
            // 商品加载完成后，检查收藏状态
            this.loadFavoriteStatus();
          } else {
            this.product = {};
            this.collected = false;
          }
        })
        .catch(() => {
          this.product = {};
          this.collected = false;
        })
        .finally(() => {
          this.loading = false;
        });
    },
    loadFavoriteStatus() {
      if (this.productId == null) {
        this.collected = false;
        return;
      }
      const userInfo = getUserInfo();
      if (!userInfo) {
        this.collected = false;
        return;
      }
      this.$axios
        .post("/interaction/query", {
          userId: userInfo.id,
          productId: this.productId,
          type: 1
        })
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.collected = data.total > 0;
          }
        })
        .catch(() => {
          this.collected = false;
        });
    },
    toggleFavorite() {
      if (this.productId == null) {
        this.$message.warning("商品ID不存在");
        return;
      }
      this.$axios
        .post(`/interaction/saveOperation/${this.productId}`)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.collected = data.data;
            this.$notify({
              duration: 1000,
              title: "收藏操作",
              message: data.msg || (this.collected ? "已收藏" : "已取消收藏"),
              type: "success"
            });
          }
        })
        .catch(error => {
          console.error("收藏操作异常:", error);
          this.$message.error("收藏操作异常");
        });
    },
    handleIWant() {
      if (!this.product || !this.product.id) {
        this.$message.warning("商品信息不完整");
        return;
      }
      this.$axios
        .post(`/interaction/likeProduct/${this.product.id}`)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.$notify({
              duration: 1200,
              title: "想要操作通知",
              message: data.msg || "已通知发布者",
              type: "success"
            });
          } else {
            this.$notify({
              duration: 2000,
              title: "想要操作通知",
              message: (data && data.msg) || "操作失败",
              type: "info"
            });
          }
        })
        .catch(error => {
          console.error("商品---想要---异常：", error);
          this.$message.error("想要操作异常");
        });
    },
    openOrderDialog() {
      if (!this.product || !this.product.id) {
        this.$message.warning("商品信息不完整");
        return;
      }
      this.orderQuantity = 1;
      this.orderRemark = "";
      this.orderDialogVisible = true;
    },
    submitOrder() {
      if (!this.product || !this.product.id) {
        this.$message.warning("商品信息不完整");
        return;
      }
      if (!this.orderQuantity || this.orderQuantity <= 0) {
        this.$message.warning("请选择正确的下单数量");
        return;
      }
      const payload = {
        productId: this.product.id,
        buyNumber: this.orderQuantity,
        detail: this.orderRemark
      };
      this.$axios
        .post("/product/buyProduct", payload)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.$notify({
              duration: 1200,
              title: "下单成功",
              message: data.msg || "下单成功",
              type: "success"
            });
            this.orderDialogVisible = false;
            // 下单成功后刷新商品信息，更新库存
            this.fetchDetail();
          } else {
            this.$notify({
              duration: 2000,
              title: "下单失败",
              message: (data && data.msg) || "下单失败",
              type: "error"
            });
          }
        })
        .catch(error => {
          console.error("商品下单异常：", error);
          this.$message.error("下单异常");
        });
    }
  }
};
</script>

<style scoped lang="scss">
.product-detail {
  padding: 0 0 24px;
}
.detail-card {
  border-radius: 12px;
  min-height: 360px;
}
.empty {
  text-align: center;
  padding: 60px 20px;
  color: #909399;
}
.detail-row {
  align-items: flex-start;
}
.image-area {
  position: relative;
  width: 100%;
  padding-top: 100%;
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}
.main-image {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
}
.main-image ::v-deep .el-image__inner {
  width: 100%;
  height: 100%;
  object-fit: contain;
}
.image-error,
.image-placeholder {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}
.tag-bargain {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 4px 10px;
  background: #e6a23c;
  color: #fff;
  font-size: 12px;
  border-radius: 4px;
}
.info-area {
  padding: 0 0 0 8px;
}
.info-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
}
.product-name {
  flex: 1;
  font-size: 22px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 8px 0;
  line-height: 1.4;
}
.btn-favorite {
  flex-shrink: 0;
  white-space: nowrap;
}
.block {
  margin-bottom: 20px;
  padding: 14px 16px;
  background: #fafbfc;
  border-radius: 10px;
  border: 1px solid #ebeef5;
}
.block-publisher {
  margin-bottom: 16px;
  padding: 10px 14px;
}
.section-label {
  display: block;
  font-size: 13px;
  color: #909399;
  margin-bottom: 6px;
}
.section-label-lg {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 10px;
}
.publisher-name {
  font-size: 15px;
  color: #303133;
  font-weight: 500;
}
.block-price {
  padding: 18px 16px;
}
.block-price .price {
  font-size: 28px;
  color: #f56c6c;
  font-weight: 600;
}
.block-info .info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 16px 24px;
}
.info-item {
  font-size: 14px;
  color: #606266;
}
.info-item-label {
  color: #909399;
  margin-right: 8px;
}
.info-item-bargain span:last-child {
  font-weight: 500;
}
.bargain-yellow {
  background: linear-gradient(to bottom, #fdf6ec, #f5dab1);
  color: #b88230;
  padding: 2px 10px;
  border-radius: 6px;
  font-weight: 600;
}
.block-desc {
  padding-bottom: 18px;
}
.detail-text {
  margin: 0;
  color: #606266;
  font-size: 14px;
  line-height: 1.7;
  white-space: pre-wrap;
}
.actions {
  margin-top: 28px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
  display: flex;
  gap: 12px;
}

.order-dialog-body {
  padding: 4px 4px 0;
}
.order-summary {
  margin-bottom: 16px;
}
.order-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #303133;
}
.order-meta {
  font-size: 13px;
  color: #606266;
}
.order-price-label {
  margin-right: 4px;
}
.order-price {
  font-size: 16px;
  font-weight: 600;
  color: #f56c6c;
  margin-right: 12px;
}
.order-stock {
  color: #909399;
}
.order-field {
  margin-bottom: 12px;
}
.order-label {
  display: block;
  margin-bottom: 4px;
  font-size: 13px;
  color: #606266;
}
</style>
