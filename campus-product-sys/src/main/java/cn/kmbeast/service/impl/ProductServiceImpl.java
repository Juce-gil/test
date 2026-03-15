package cn.kmbeast.service.impl;

import cn.kmbeast.context.LocalThreadHolder;
import cn.kmbeast.mapper.InteractionMapper;
import cn.kmbeast.mapper.OrdersMapper;
import cn.kmbeast.mapper.ProductMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.OrdersQueryDto;
import cn.kmbeast.pojo.dto.query.extend.ProductQueryDto;
import cn.kmbeast.pojo.dto.update.OrdersDTO;
import cn.kmbeast.pojo.em.InteractionEnum;
import cn.kmbeast.pojo.entity.Interaction;
import cn.kmbeast.pojo.entity.Orders;
import cn.kmbeast.pojo.entity.Product;
import cn.kmbeast.pojo.vo.ChartVO;
import cn.kmbeast.pojo.vo.OrdersDeliverDto;
import cn.kmbeast.pojo.vo.OrdersVO;
import cn.kmbeast.pojo.vo.ProductVO;
import cn.kmbeast.service.ProductService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 商品类别业务逻辑接口实现类
 */
@Service
public class ProductServiceImpl implements ProductService {

    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private InteractionMapper interactionMapper;

    private final static Integer AWAITING_PAY = 1; // 待支付状态
    private final static Integer OK_PAY = 2; // 已支付状态
    private final static Integer REPLY_REFUND = 2; // 申请退款

    /**
     * 商品发货
     *
     * @param ordersDeliverDto 发货参数Dto
     * @return Result<String> 响应结果
     */
    @Override
    public Result<String> deliverGoods(OrdersDeliverDto ordersDeliverDto) {
        if (ordersDeliverDto.getOrdersId() == null) {
            return ApiResult.error("订单ID不能为空");
        }
        if (ordersDeliverDto.getDeliverAdrId() == null) {
            return ApiResult.error("请设置商家发货地址");
        }
        Orders orders = new Orders();
        orders.setId(ordersDeliverDto.getOrdersId()); // 设置订单ID
        orders.setDeliverAdrId(ordersDeliverDto.getDeliverAdrId()); // 设置商家发货地址ID
        orders.setDeliverTime(LocalDateTime.now()); // 设置发货时间
        orders.setIsDeliver(true); // 发货状态设置成已发货状态
        ordersMapper.update(orders);
        return ApiResult.success("商品发货成功");
    }


    /**
     * 新增
     *
     * @param product 参数
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> save(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            return ApiResult.error("商品名不能为空");
        }
        if (!StringUtils.hasText(product.getCoverList())) {
            return ApiResult.error("请上传商品封面");
        }
        if (product.getPrice() == null) {
            return ApiResult.error("请填写价格");
        }
        if (product.getInventory() == null) {
            return ApiResult.error("库存不能为空");
        }
        if (product.getCategoryId() == null) {
            return ApiResult.error("请选择商品分类");
        }
        if (product.getIsBargain() == null) {
            product.setIsBargain(false);
        }
        product.setUserId(LocalThreadHolder.getUserId());
        product.setCreateTime(LocalDateTime.now());
        productMapper.save(product);
        return ApiResult.success("商品新增成功");
    }

    /**
     * 修改
     *
     * @param product 参数
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> update(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            return ApiResult.error("商品名不能为空");
        }
        if (!StringUtils.hasText(product.getCoverList())) {
            return ApiResult.error("请上传商品封面");
        }
        if (product.getPrice() == null) {
            return ApiResult.error("请填写价格");
        }
        if (product.getInventory() == null) {
            return ApiResult.error("库存不能为空");
        }
        if (product.getCategoryId() == null) {
            return ApiResult.error("请选择商品分类");
        }
        if (product.getIsBargain() == null) {
            product.setIsBargain(false);
        }
        productMapper.update(product);
        return ApiResult.success("商品修改成功");
    }

    /**
     * 删除
     *
     * @param ids 待删除ID集合
     * @return Result<String> 后台通用返回封装类
     */
    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        productMapper.batchDelete(ids);
        return ApiResult.success("商品删除成功");
    }

    /**
     * 查询
     *
     * @param productQueryDto 查询参数
     * @return Result<List < ProductVO>> 后台通用返回封装类
     */
    @Override
    public Result<List<ProductVO>> query(ProductQueryDto productQueryDto) {
        int totalCount = productMapper.queryCount(productQueryDto);
        List<ProductVO> productVOList = productMapper.query(productQueryDto);
        return ApiResult.success(productVOList, totalCount);
    }

    /**
     * 商品下单
     *
     * @param ordersDTO 订单
     * @return Result<String>
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> buyProduct(OrdersDTO ordersDTO) {
        if (ordersDTO.getProductId() == null) {
            return ApiResult.error("商品ID不为空");
        }
        ProductQueryDto productQueryDto = new ProductQueryDto();
        productQueryDto.setId(ordersDTO.getProductId());
        List<ProductVO> productVOS = productMapper.query(productQueryDto);
        if (productVOS.isEmpty()) {
            return ApiResult.error("商品信息异常");
        }
        // 有且仅有一条商品信息
        ProductVO productVO = productVOS.get(0);
        // 判断库存情况
        if (productVO.getInventory() <= 0
                || (productVO.getInventory() - ordersDTO.getBuyNumber()) < 0) {
            return ApiResult.error("商品库存不足");
        }
        createOrders(ordersDTO, productVO);
        ordersMapper.save(ordersDTO);
        // 扣库存
        Product product = new Product();
        product.setId(productVO.getId());
        product.setInventory(productVO.getInventory() - ordersDTO.getBuyNumber());
        productMapper.update(product);

        return ApiResult.success("下单成功");
    }

    /**
     * 设置订单所需参数
     *
     * @param orders    订单
     * @param productVO 商品信息
     */
    private void createOrders(Orders orders, ProductVO productVO) {
        orders.setCode(createOrdersCode());
        orders.setUserId(LocalThreadHolder.getUserId());
        orders.setTradeStatus(AWAITING_PAY); // 初始时，未交易成功
        orders.setBuyPrice(productVO.getPrice());
        orders.setCreateTime(LocalDateTime.now());
    }

    /**
     * 生成订单号
     *
     * @return String
     */
    private String createOrdersCode() {
        // UUID
        //String ordersCode = UUID.randomUUID().toString().toLowerCase();
        // 时间戳
        long timeMillis = System.currentTimeMillis();
        return String.valueOf(timeMillis);
    }

    /**
     * 商品下单
     *
     * @param ordersId 订单ID
     * @return Result<String> 通用返回封装类
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> placeAnOrder(Integer ordersId) {
        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setTradeStatus(OK_PAY); // 已支付状态
        orders.setIsConfirm(false);// 未收货
        orders.setTradeTime(LocalDateTime.now());
        ordersMapper.update(orders);
        return ApiResult.success("下单成功");
    }

    /**
     * 申请退款
     *
     * @param ordersId 订单ID
     * @return Result<String> 响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> refund(Integer ordersId) {
        OrdersQueryDto ordersQueryDto = new OrdersQueryDto();
        ordersQueryDto.setId(ordersId);
        ordersQueryDto.setRefundStatus(1);
        ordersQueryDto.setUserId(LocalThreadHolder.getUserId());
        int queryCount = ordersMapper.queryCount(ordersQueryDto);
        if (queryCount > 0) { // 存在未审核退款记录
            return ApiResult.error("您已申请退款，待卖家审核，请勿重复申请");
        }
        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setRefundStatus(REPLY_REFUND);
        ordersMapper.update(orders);
        return ApiResult.success("申请退款成功，请等待卖家审核");
    }

    /**
     * 确定收货
     *
     * @param ordersId 订单ID
     * @return Result<String> 响应结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> getGoods(Integer ordersId) {
        OrdersQueryDto ordersQueryDto = new OrdersQueryDto();
        ordersQueryDto.setId(ordersId);
        ordersQueryDto.setUserId(LocalThreadHolder.getUserId());
        List<OrdersVO> ordersVOList = ordersMapper.query(ordersQueryDto);
        if (ordersVOList.isEmpty()) {
            return ApiResult.error("订单异常");
        }
        OrdersVO ordersVO = ordersVOList.get(0);
        if (ordersVO.getIsDeliver() == null) { // 未发货
            if (ordersVO.getIsRefundConfirm() != null && ordersVO.getIsRefundConfirm()) {
                Orders orders = new Orders();
                orders.setId(ordersId);
                orders.setIsConfirm(true);
                orders.setIsConfirmTime(LocalDateTime.now());
                ordersMapper.update(orders);
            } else {
                return ApiResult.error("卖家未发货哦");
            }
        }
        Orders orders = new Orders();
        orders.setId(ordersVO.getId()); // 设置订单ID
        orders.setIsConfirm(true); // 已确认收货
        orders.setIsConfirmTime(LocalDateTime.now()); // 确认收货时间
        ordersMapper.update(orders);
        return ApiResult.success("确定收货成功");
    }

    /**
     * 查询用户商品指标情况
     *
     * @param productQueryDto 查询参数
     * @return Result<List < ChartVO>> 响应结果
     */
    @Override
    public Result<List<ChartVO>> queryProductInfo(ProductQueryDto productQueryDto) {
        List<Integer> productIds = productMapper.queryProductIds(productQueryDto.getUserId());
        if (productIds.isEmpty()) {
            return ApiResult.success(new ArrayList<>());
        }
        List<Interaction> interactionList = interactionMapper.queryByProductIds(productIds);
        // 浏览、收藏、想要
        long viewCount = getProductCount(interactionList, InteractionEnum.VIEW.getType());
        long saveCount = getProductCount(interactionList, InteractionEnum.SAVE.getType());
        long loveCount = getProductCount(interactionList, InteractionEnum.LOVE.getType());
        List<ChartVO> chartVOList = new ArrayList<>();
        ChartVO chartVOView = new ChartVO("商品被浏览", (int) viewCount);
        ChartVO chartVOSave = new ChartVO("商品被收藏", (int) saveCount);
        ChartVO chartVOLove = new ChartVO("商品被想要", (int) loveCount);
        chartVOList.add(chartVOView);
        chartVOList.add(chartVOSave);
        chartVOList.add(chartVOLove);
        return ApiResult.success(chartVOList);
    }

    /**
     * 过滤指定的商品指标数据
     *
     * @param interactionList 互动数据源
     * @param type            互动类型
     * @return long
     */
    private long getProductCount(List<Interaction> interactionList, Integer type) {
        return interactionList.stream()
                .filter(interaction -> Objects.equals(type, interaction.getType()))
                .count();
    }

    @Override
    public Result<List<ProductVO>> queryProductList(Integer id) {
        ProductVO productVO = productMapper.queryById(id);
        if (productVO == null) {
            return ApiResult.error("商品不存在");
        }
        Integer userId = productVO.getUserId();
        ProductQueryDto productQueryDto = new ProductQueryDto();
        productQueryDto.setUserId(userId);
        List<ProductVO> productVOS = productMapper.query(productQueryDto);
        return ApiResult.success(productVOS);
    }


}
