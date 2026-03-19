<template>
  <div class="orders-page">
    <UserPageSection
      eyebrow="Orders"
      title="我的订单"
      description="集中查看你的下单记录、支付状态与退款进度。"
    >
      <div class="toolbar">
        <div class="status-tabs">
          <button
            v-for="item in tradeStatusList"
            :key="item.name"
            type="button"
            class="status-tab"
            :class="{ active: tradeStatusSelectedItem.name === item.name }"
            @click="tradeStatusSelected(item)"
          >
            {{ item.name }}
          </button>
        </div>

        <el-input
          v-model="ordersQueryDto.code"
          class="order-search"
          placeholder="搜索订单号"
          clearable
          @clear="handleFilterClear"
        >
          <el-button
            slot="append"
            icon="el-icon-search"
            @click="fetchOrders"
          ></el-button>
        </el-input>
      </div>

      <div v-if="!ordersList.length" class="empty-wrap">
        <el-empty description="暂无符合条件的订单"></el-empty>
      </div>

      <div v-else class="order-list">
        <article
          v-for="orderInfo in ordersList"
          :key="orderInfo.id"
          class="order-card"
        >
          <div class="order-top">
            <div>
              <div class="order-code">订单号：{{ orderInfo.code }}</div>
              <div class="order-time">创建时间：{{ orderInfo.createTime }}</div>
            </div>
            <div class="order-status-group">
              <span
                class="status-badge"
                :class="{ success: orderInfo.tradeStatus }"
              >
                {{ orderInfo.tradeStatus ? "已支付" : "待支付" }}
              </span>
              <span v-if="orderInfo.refundStatus" class="status-badge warning">
                {{ orderInfo.isRefundConfirm ? "已退款" : "退款处理中" }}
              </span>
            </div>
          </div>

          <div class="order-main">
            <img :src="orderInfo.cover" alt="cover" class="order-cover" />

            <div class="order-content">
              <div class="title-row">
                <div class="order-title">{{ orderInfo.productTitle }}</div>
                <span class="order-count">x{{ orderInfo.buyNumber }}</span>
              </div>

              <div class="order-detail">
                备注：{{ orderInfo.detail || "暂无备注" }}
              </div>

              <div class="order-price-row">
                <span class="price-symbol">¥</span>
                <span class="price-value">{{ totalPrice(orderInfo) }}</span>
              </div>

              <div class="order-sub-info">
                <span v-if="orderInfo.tradeStatus">
                  支付时间：{{ orderInfo.tradeTime || "暂无记录" }}
                </span>
                <span v-if="orderInfo.isRefundConfirm">
                  退款时间：{{ orderInfo.refundTime || "暂无记录" }}
                </span>
              </div>

              <div class="action-row">
                <template v-if="!orderInfo.refundStatus">
                  <button
                    v-if="!orderInfo.tradeStatus"
                    type="button"
                    class="action-btn primary"
                    @click="placeAnOrder(orderInfo)"
                  >
                    立即支付
                  </button>
                  <button
                    v-else
                    type="button"
                    class="action-btn primary"
                    @click="refund(orderInfo)"
                  >
                    申请退款
                  </button>
                  <button
                    v-if="orderInfo.tradeStatus"
                    type="button"
                    class="action-btn secondary"
                    @click="del(orderInfo)"
                  >
                    删除订单
                  </button>
                </template>
                <span v-else class="refund-tip">
                  {{
                    orderInfo.isRefundConfirm
                      ? "退款已原路退回"
                      : "退款申请已提交，等待卖家确认"
                  }}
                </span>
              </div>
            </div>
          </div>
        </article>
      </div>
    </UserPageSection>
  </div>
</template>

<script>
import UserPageSection from "@/components/user/UserPageSection.vue";
import { toFullImageUrl } from "@/utils/imageUrl";

export default {
  name: "Orders",
  components: {
    UserPageSection
  },
  data() {
    return {
      ordersList: [],
      ordersQueryDto: {
        code: "",
        tradeStatus: null,
        current: 1,
        size: 50
      },
      tradeStatusSelectedItem: {},
      tradeStatusList: [
        { tradeStatus: null, name: "全部" },
        { tradeStatus: true, name: "已支付" },
        { tradeStatus: false, name: "待支付" }
      ]
    };
  },
  created() {
    this.tradeStatusSelected(this.tradeStatusList[0]);
  },
  methods: {
    tradeStatusSelected(item) {
      this.tradeStatusSelectedItem = item;
      this.ordersQueryDto.tradeStatus = item.tradeStatus;
      this.fetchOrders();
    },
    handleFilterClear() {
      this.ordersQueryDto.code = "";
      this.fetchOrders();
    },
    totalPrice(orderInfo) {
      const total = (orderInfo.buyNumber || 0) * (orderInfo.buyPrice || 0);
      return parseFloat(total).toFixed(2);
    },
    refund(order) {
      this.$axios
        .put(`/orders/refund/${order.id}`)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.$notify({
              duration: 1000,
              title: "退款申请已提交",
              message: data.msg || "请等待卖家确认",
              type: "success"
            });
            this.fetchOrders();
          } else {
            this.$message.info((data && data.msg) || "退款失败");
          }
        })
        .catch(error => {
          console.error("退款异常：", error);
          this.$message.error("退款异常");
        });
    },
    placeAnOrder(order) {
      this.$axios
        .put(`/orders/pay/${order.id}`)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.$notify({
              duration: 1000,
              title: "支付成功",
              message: data.msg || "订单已完成支付",
              type: "success"
            });
            this.fetchOrders();
          } else {
            this.$message.info((data && data.msg) || "支付失败");
          }
        })
        .catch(error => {
          console.error("支付异常：", error);
          this.$message.error("支付异常");
        });
    },
    async del(order) {
      const confirmed = await this.$swalConfirm({
        title: "删除订单",
        text: "删除后不可恢复，确定继续吗？",
        icon: "warning"
      });
      if (!confirmed) return;

      try {
        const res = await this.$axios.post("/orders/batchDelete", [order.id]);
        const { data } = res;
        if (data && data.code === 200) {
          this.$notify({
            duration: 1000,
            title: "删除成功",
            message: data.msg || "订单已删除",
            type: "success"
          });
          this.fetchOrders();
        } else {
          this.$message.info((data && data.msg) || "删除失败");
        }
      } catch (error) {
        console.error("订单删除异常：", error);
        this.$message.error("订单删除异常");
      }
    },
    coverParse(order) {
      if (!order.productCover) return "";
      const first = order.productCover.split(",")[0];
      return toFullImageUrl(first);
    },
    fetchOrders() {
      this.$axios
        .post("/orders/queryUser", this.ordersQueryDto)
        .then(res => {
          const { data } = res;
          if (data && data.code === 200) {
            this.ordersList = (data.data || []).map(order => ({
              id: order.id,
              code: order.code,
              productTitle: order.productTitle,
              detail: order.detail,
              buyPrice: order.buyPrice,
              buyNumber: order.buyNumber,
              tradeStatus: order.tradeStatus,
              tradeTime: order.tradeTime,
              refundStatus: order.refundStatus,
              refundTime: order.refundTime,
              isRefundConfirm: order.isRefundConfirm,
              createTime: order.createTime,
              cover: this.coverParse(order)
            }));
          } else {
            this.ordersList = [];
          }
        })
        .catch(error => {
          console.error("订单查询异常：", error);
          this.ordersList = [];
        });
    }
  }
};
</script>

<style scoped lang="scss">
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  margin-bottom: 22px;
  flex-wrap: wrap;
}

.status-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-tab {
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #dbeafe;
  border-radius: 999px;
  background: #ffffff;
  color: #475569;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
  transition: all 0.2s ease;
}

.status-tab.active,
.status-tab:hover {
  background: linear-gradient(135deg, #eff6ff, #dcfce7);
  color: #0f172a;
}

.order-search {
  width: 320px;
}

.order-list {
  display: grid;
  gap: 18px;
}

.order-card {
  padding: 20px;
  border-radius: 24px;
  border: 1px solid rgba(226, 232, 240, 0.82);
  background: linear-gradient(135deg, #ffffff, #f8fbff);
}

.order-top {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 16px;
}

.order-code {
  color: #0f172a;
  font-size: 16px;
  font-weight: 700;
}

.order-time {
  margin-top: 6px;
  color: #64748b;
  font-size: 13px;
}

.order-status-group {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.status-badge {
  display: inline-flex;
  align-items: center;
  min-height: 32px;
  padding: 0 12px;
  border-radius: 999px;
  background: #f8fafc;
  color: #475569;
  font-size: 12px;
  font-weight: 700;
}

.status-badge.success {
  background: #dcfce7;
  color: #15803d;
}

.status-badge.warning {
  background: #fef3c7;
  color: #b45309;
}

.order-main {
  display: flex;
  gap: 18px;
}

.order-cover {
  width: 132px;
  height: 132px;
  border-radius: 18px;
  object-fit: cover;
  background: #f1f5f9;
}

.order-content {
  flex: 1;
}

.title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.order-title {
  color: #0f172a;
  font-size: 20px;
  font-weight: 700;
}

.order-count {
  color: #64748b;
  font-size: 13px;
  font-weight: 700;
}

.order-detail,
.order-sub-info {
  margin-top: 10px;
  color: #64748b;
  font-size: 14px;
  line-height: 1.75;
}

.order-sub-info {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
}

.order-price-row {
  display: flex;
  align-items: baseline;
  gap: 4px;
  margin-top: 14px;
}

.price-symbol {
  color: #f97316;
  font-size: 14px;
  font-weight: 800;
}

.price-value {
  color: #f97316;
  font-size: 28px;
  font-weight: 800;
}

.action-row {
  display: flex;
  gap: 10px;
  align-items: center;
  flex-wrap: wrap;
  margin-top: 18px;
}

.action-btn {
  min-height: 40px;
  padding: 0 16px;
  border: none;
  border-radius: 12px;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.action-btn.primary {
  background: linear-gradient(135deg, #22c55e, #16a34a);
  color: #ffffff;
}

.action-btn.secondary {
  background: #eff6ff;
  color: #2563eb;
}

.refund-tip {
  color: #2563eb;
  font-size: 13px;
  font-weight: 600;
}

.empty-wrap {
  padding: 12px 0;
}

@media (max-width: 768px) {
  .order-search {
    width: 100%;
  }

  .order-main {
    flex-direction: column;
  }

  .order-cover {
    width: 100%;
    height: 220px;
  }

  .order-top {
    flex-direction: column;
  }
}
</style>
