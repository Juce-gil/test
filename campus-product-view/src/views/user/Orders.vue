<template>
  <div class="orders-page">
    <div class="condition">
      <span class="trade">
        <span
          v-for="item in tradeStatusList"
          :key="item.name"
          :style="{
            color: tradeStatusSelectedItem.name === item.name ? 'rgb(51,51,51)' : '',
            backgroundColor: tradeStatusSelectedItem.name === item.name ? 'rgb(255,255,255)' : ''
          }"
          @click="tradeStatusSelected(item)"
        >{{ item.name }}</span>
      </span>
      <el-input
        size="small"
        style="width: 180px;"
        v-model="ordersQueryDto.code"
        placeholder="订单号"
        clearable
        @clear="handleFilterClear"
      >
        <el-button slot="append" icon="el-icon-search" @click="fetchOrders"></el-button>
      </el-input>
    </div>

    <div v-if="!ordersList.length">
      <el-empty description="未找到符合条件的订单数据"></el-empty>
    </div>
    <div v-else>
      <div class="item-order" v-for="orderInfo in ordersList" :key="orderInfo.id">
        <div class="orders-base-info">
          <div class="code">订单号：{{ orderInfo.code }}</div>
          <div class="code" v-if="orderInfo.tradeStatus">支付时间：{{ orderInfo.tradeTime }}</div>
          <div class="code" v-if="orderInfo.isRefundConfirm">退款时间：{{ orderInfo.refundTime }}</div>
        </div>
        <div class="info">
          <div>
            <img :src="orderInfo.cover" alt="">
          </div>
          <div>
            <div>
              <span class="title">{{ orderInfo.productTitle }}</span>
              <span class="number">x{{ orderInfo.buyNumber }}</span>
            </div>
            <div class="detail">
              备注：{{ orderInfo.detail || '无' }}
            </div>
            <div>
              <span class="symbol">￥</span>
              <span class="price">{{ totalPrice(orderInfo) }}</span>
            </div>
            <div class="orders-base-info">创建时间：{{ orderInfo.createTime }}</div>
            <div style="margin-block: 10px;">
              <span v-if="orderInfo.refundStatus" style="color: rgb(0, 121, 186);">
                {{ orderInfo.isRefundConfirm ? '钱款已原路返回' : '申请退款中，待卖家确认...' }}
              </span>
              <span v-else>
                <span
                  v-if="!orderInfo.tradeStatus"
                  class="edit-button"
                  @click="placeAnOrder(orderInfo)"
                >下单</span>
                <span
                  v-else
                  class="edit-button"
                  @click="refund(orderInfo)"
                >申请退款</span>
                <span
                  v-if="orderInfo.tradeStatus"
                  class="channel-button"
                  style="margin-left: 6px;"
                  @click="del(orderInfo)"
                >删除</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { toFullImageUrl } from '@/utils/imageUrl'

export default {
  name: 'Orders',
  data() {
    return {
      ordersList: [],
      ordersQueryDto: {
        code: '',
        tradeStatus: null,
        current: 1,
        size: 50
      },
      tradeStatusSelectedItem: {},
      tradeStatusList: [
        { tradeStatus: null, name: '全部' },
        { tradeStatus: true, name: '已支付' },
        { tradeStatus: false, name: '未支付' }
      ]
    }
  },
  created() {
    this.tradeStatusSelected(this.tradeStatusList[0])
  },
  methods: {
    tradeStatusSelected(item) {
      this.tradeStatusSelectedItem = item
      this.ordersQueryDto.tradeStatus = item.tradeStatus
      this.fetchOrders()
    },
    handleFilterClear() {
      this.ordersQueryDto.code = ''
      this.fetchOrders()
    },
    totalPrice(orderInfo) {
      const total = (orderInfo.buyNumber || 0) * (orderInfo.buyPrice || 0)
      return parseFloat(total).toFixed(2)
    },
    refund(order) {
      this.$axios
        .put(`/orders/refund/${order.id}`)
        .then(res => {
          const { data } = res
          if (data && data.code === 200) {
            this.$notify({
              duration: 1000,
              title: '退款操作',
              message: data.msg || '退款成功',
              type: 'success'
            })
            this.fetchOrders()
          } else {
            this.$message.info((data && data.msg) || '退款失败')
          }
        })
        .catch(error => {
          console.error('退款异常：', error)
          this.$message.error('退款异常')
        })
    },
    placeAnOrder(order) {
      this.$axios
        .put(`/orders/pay/${order.id}`)
        .then(res => {
          const { data } = res
          if (data && data.code === 200) {
            this.$notify({
              duration: 1000,
              title: '支付',
              message: data.msg || '支付成功',
              type: 'success'
            })
            this.fetchOrders()
          } else {
            this.$message.info((data && data.msg) || '支付失败')
          }
        })
        .catch(error => {
          console.error('支付异常：', error)
          this.$message.error('支付异常')
        })
    },
    async del(order) {
      const confirmed = await this.$swalConfirm({
        title: '删除订单',
        text: '删除后不可恢复，是否继续？',
        icon: 'warning'
      })
      if (!confirmed) return
      try {
        const ids = [order.id]
        const res = await this.$axios.post('/orders/batchDelete', ids)
        const { data } = res
        if (data && data.code === 200) {
          this.$notify({
            duration: 1000,
            title: '删除成功',
            message: data.msg || '订单已删除',
            type: 'success'
          })
          this.fetchOrders()
        } else {
          this.$message.info((data && data.msg) || '删除失败')
        }
      } catch (error) {
        console.error('订单删除异常：', error)
        this.$message.error('订单删除异常')
      }
    },
    coverParse(order) {
      if (!order.productCover) return ''
      const first = order.productCover.split(',')[0]
      return toFullImageUrl(first)
    },
    fetchOrders() {
      this.$axios
        .post('/orders/queryUser', this.ordersQueryDto)
        .then(res => {
          const { data } = res
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
            }))
          } else {
            this.ordersList = []
          }
        })
        .catch(error => {
          console.error('订单数据查询异常：', error)
          this.ordersList = []
        })
    }
  }
}
</script>

<style scoped lang="scss">
.orders-page {
  .condition {
    display: flex;
    justify-content: flex-end;
    margin-bottom: 10px;
    .trade {
      display: inline-block;
      font-size: 12px;
      background-color: rgb(246, 246, 246);
      line-height: 24px;
      padding-inline: 10px;
      padding-block: 4px;
      margin-right: 5px;
      border-radius: 5px;
      cursor: pointer;
      span {
        display: inline-block;
        padding-inline: 10px;
        border-radius: 5px;
      }
    }
  }
  .item-order {
    font-size: 14px;
    margin-block: 10px;
    padding-bottom: 10px;
    border-bottom: 1px solid rgb(246, 246, 246);
    .orders-base-info {
      font-size: 14px;
      margin-block: 6px;
      color: rgb(117, 117, 117);
      .code {
        margin-block: 4px;
      }
    }
    .info {
      display: flex;
      justify-content: flex-start;
      gap: 20px;
      img {
        width: 140px;
      }
      .detail {
        margin-block: 4px;
        font-size: 14px;
      }
      .symbol {
        font-size: 12px;
        margin-right: 4px;
        color: rgb(255, 79, 36);
      }
      .price {
        font-size: 18px;
        font-weight: 800;
        color: rgb(255, 79, 36);
      }
      .title {
        font-size: 20px;
        font-weight: 800;
      }
      .number {
        margin-left: 6px;
      }
    }
  }
}
</style>