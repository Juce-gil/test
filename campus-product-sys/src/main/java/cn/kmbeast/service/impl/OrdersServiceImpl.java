package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.OrdersMapper;
import cn.kmbeast.mapper.ProductMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.OrdersQueryDto;
import cn.kmbeast.pojo.entity.Orders;
import cn.kmbeast.pojo.vo.OrdersVO;
import cn.kmbeast.service.OrdersService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 订单业务逻辑接口实现类
 */
@Service
public class OrdersServiceImpl implements OrdersService {

    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private ProductMapper productMapper;

    /**
     * 新增
     *
     * @param orders 参数
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> save(Orders orders) {
        orders.setUserId(LocalThreadHolder.getUserId());
        orders.setTradeStatus(false); // 未付款状态
        orders.setCreateTime(LocalDateTime.now());
        ordersMapper.save(orders);
        return ApiResult.success("订单新增成功");
    }

    /**
     * 修改
     *
     * @param orders 参数
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> update(Orders orders) {
        ordersMapper.update(orders);
        return ApiResult.success("订单修改成功");
    }

    /**
     * 删除
     *
     * @param ids 待删除ID集合
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        ordersMapper.batchDelete(ids);
        return ApiResult.success("订单删除成功");
    }

    /**
     * 查询
     *
     * @param ordersQueryDto 查询参数
     * @return Result<List < OrdersVO>> 后台通用返回封装类
     */
    @Override
    public Result<List<OrdersVO>> query(OrdersQueryDto ordersQueryDto) {
        int totalCount = ordersMapper.queryCount(ordersQueryDto);
        List<OrdersVO> ordersVOList = ordersMapper.query(ordersQueryDto);
        return ApiResult.success(ordersVOList, totalCount);
    }

    /**
     * 查询用户发布的商品产生的订单数据
     *
     * @param ordersQueryDto 查询参数
     * @return Result<List < OrdersVO>> 响应结果
     */
    @Override
    public Result<List<OrdersVO>> queryOrdersList(OrdersQueryDto ordersQueryDto) {
        List<Integer> productIds = productMapper.queryProductIds(LocalThreadHolder.getUserId());
        if (productIds == null || productIds.isEmpty()) {
            return ApiResult.success(new ArrayList<>());
        }
        ordersQueryDto.setProductIds(productIds);
        List<OrdersVO> ordersVOList = ordersMapper.queryByProductIds(ordersQueryDto);
        return ApiResult.success(ordersVOList);
    }

    /**
     * 订单确定退款
     *
     * @param ordersId 订单ID
     * @return Result<String> 响应结果
     */
    @Override
    public Result<String> returnMoney(Integer ordersId) {
        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setRefundStatus(true);
        orders.setIsRefundConfirm(true);
        orders.setRefundTime(LocalDateTime.now());
        ordersMapper.update(orders);
        return ApiResult.success("退款成功");
    }
}