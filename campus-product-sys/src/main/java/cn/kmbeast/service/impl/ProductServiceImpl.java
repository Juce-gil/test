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
 * Product service implementation.
 */
@Service
public class ProductServiceImpl implements ProductService {

    private static final Integer ORDER_STATUS_PENDING_CONFIRM = 1;
    private static final Integer ORDER_STATUS_RESERVED = 2;
    private static final Integer ORDER_STATUS_PARTIAL_CONFIRMED = 3;
    private static final Integer ORDER_STATUS_COMPLETED = 4;
    private static final Integer ORDER_STATUS_CANCELLED = 5;

    private static final String PRODUCT_STATUS_ON_SALE = "ON_SALE";
    private static final String PRODUCT_STATUS_RESERVED = "RESERVED";
    private static final String PRODUCT_STATUS_SOLD = "SOLD";
    private static final String PRODUCT_STATUS_OFFLINE = "OFFLINE";
    private static final String PRODUCT_AUDIT_APPROVED = "APPROVED";

    @Resource
    private ProductMapper productMapper;
    @Resource
    private OrdersMapper ordersMapper;
    @Resource
    private InteractionMapper interactionMapper;

    @Override
    public Result<String> deliverGoods(OrdersDeliverDto ordersDeliverDto) {
        if (ordersDeliverDto == null || ordersDeliverDto.getOrdersId() == null) {
            return ApiResult.error("order id cannot be empty");
        }
        return confirmTradeBySeller(ordersDeliverDto.getOrdersId());
    }

    @Override
    public Result<String> save(Product product) {
        if (!StringUtils.hasText(product.getName())) {
            return ApiResult.error("product name cannot be empty");
        }
        if (!StringUtils.hasText(product.getCoverList())) {
            return ApiResult.error("product cover cannot be empty");
        }
        if (product.getPrice() == null) {
            return ApiResult.error("product price cannot be empty");
        }
        if (product.getCategoryId() == null) {
            return ApiResult.error("product category cannot be empty");
        }
        if (product.getInventory() == null || product.getInventory() <= 0) {
            product.setInventory(1);
        }
        if (product.getIsBargain() == null) {
            product.setIsBargain(false);
        }
        if (!StringUtils.hasText(product.getStatus())) {
            product.setStatus(PRODUCT_STATUS_ON_SALE);
        }
        if (!StringUtils.hasText(product.getAuditStatus())) {
            product.setAuditStatus(PRODUCT_AUDIT_APPROVED);
        }
        product.setUserId(LocalThreadHolder.getUserId());
        product.setCreateTime(LocalDateTime.now());
        productMapper.save(product);
        return ApiResult.success("product published successfully");
    }

    @Override
    public Result<String> update(Product product) {
        if (product.getId() == null) {
            return ApiResult.error("product id cannot be empty");
        }
        if (!StringUtils.hasText(product.getName())) {
            return ApiResult.error("product name cannot be empty");
        }
        if (!StringUtils.hasText(product.getCoverList())) {
            return ApiResult.error("product cover cannot be empty");
        }
        if (product.getPrice() == null) {
            return ApiResult.error("product price cannot be empty");
        }
        if (product.getCategoryId() == null) {
            return ApiResult.error("product category cannot be empty");
        }
        if (product.getInventory() == null || product.getInventory() <= 0) {
            product.setInventory(1);
        }
        if (product.getIsBargain() == null) {
            product.setIsBargain(false);
        }
        productMapper.update(product);
        return ApiResult.success("product updated successfully");
    }

    @Override
    public Result<String> batchDelete(List<Integer> ids) {
        productMapper.batchDelete(ids);
        return ApiResult.success("products deleted successfully");
    }

    @Override
    public Result<List<ProductVO>> query(ProductQueryDto productQueryDto) {
        int totalCount = productMapper.queryCount(productQueryDto);
        List<ProductVO> productVOList = productMapper.query(productQueryDto);
        return ApiResult.success(productVOList, totalCount);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public synchronized Result<String> buyProduct(OrdersDTO ordersDTO) {
        if (ordersDTO == null || ordersDTO.getProductId() == null) {
            return ApiResult.error("product id cannot be empty");
        }
        ProductVO productVO = getProductById(ordersDTO.getProductId());
        if (productVO == null) {
            return ApiResult.error("product not found");
        }
        if (Objects.equals(productVO.getUserId(), LocalThreadHolder.getUserId())) {
            return ApiResult.error("you cannot reserve your own product");
        }
        if (productVO.getInventory() == null || productVO.getInventory() <= 0) {
            return ApiResult.error("product inventory is not available");
        }
        if (!PRODUCT_STATUS_ON_SALE.equals(resolveProductStatus(productVO.getStatus()))) {
            return ApiResult.error("product is not available for reservation");
        }
        Integer buyNumber = ordersDTO.getBuyNumber();
        if (buyNumber == null) {
            buyNumber = 1;
        }
        if (!Objects.equals(buyNumber, 1)) {
            return ApiResult.error("only one item can be reserved at a time");
        }
        if (hasActiveReservation(productVO.getId())) {
            return ApiResult.error("product already has an active reservation");
        }

        createReservationOrder(ordersDTO, productVO);
        ordersMapper.save(ordersDTO);
        return ApiResult.success("reservation request submitted, waiting for seller confirmation");
    }

    private void createReservationOrder(Orders orders, ProductVO productVO) {
        orders.setCode(createOrdersCode());
        orders.setUserId(LocalThreadHolder.getUserId());
        orders.setBuyNumber(1);
        orders.setTradeStatus(ORDER_STATUS_PENDING_CONFIRM);
        orders.setBuyPrice(productVO.getPrice());
        orders.setCreateTime(LocalDateTime.now());
    }

    private String createOrdersCode() {
        long timeMillis = System.currentTimeMillis();
        return String.valueOf(timeMillis);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> placeAnOrder(Integer ordersId) {
        OrdersVO ordersVO = getOrderById(ordersId);
        if (ordersVO == null) {
            return ApiResult.error("order not found");
        }
        if (!Objects.equals(ordersVO.getSellerId(), LocalThreadHolder.getUserId())) {
            return ApiResult.error("only the seller can confirm this reservation");
        }
        if (!Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_PENDING_CONFIRM)) {
            return ApiResult.error("current order status does not allow reservation confirmation");
        }

        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setTradeStatus(ORDER_STATUS_RESERVED);
        LocalDateTime confirmTime = LocalDateTime.now();
        orders.setSellerConfirmTime(confirmTime);
        ordersMapper.update(orders);

        Product product = new Product();
        product.setId(ordersVO.getProductId());
        product.setStatus(PRODUCT_STATUS_RESERVED);
        product.setInventory(0);
        productMapper.update(product);
        return ApiResult.success("seller confirmed reservation, product locked as RESERVED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> refund(Integer ordersId) {
        OrdersVO ordersVO = getOrderById(ordersId);
        if (ordersVO == null) {
            return ApiResult.error("order not found");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_COMPLETED)) {
            return ApiResult.error("completed reservations cannot be cancelled");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_CANCELLED)) {
            return ApiResult.error("reservation has already been cancelled");
        }
        Integer currentUserId = LocalThreadHolder.getUserId();
        boolean buyer = Objects.equals(ordersVO.getUserId(), currentUserId);
        boolean seller = Objects.equals(ordersVO.getSellerId(), currentUserId);
        if (!buyer && !seller) {
            return ApiResult.error("only the buyer or seller can cancel this reservation");
        }

        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setTradeStatus(ORDER_STATUS_CANCELLED);
        orders.setCancelTime(LocalDateTime.now());
        orders.setCancelReason(seller ? "seller cancelled reservation" : "buyer cancelled reservation");
        ordersMapper.update(orders);

        restoreProductToSale(ordersVO.getProductId());
        return ApiResult.success(seller ? "seller cancelled the reservation" : "reservation cancelled successfully");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> getGoods(Integer ordersId) {
        OrdersVO ordersVO = getOrderById(ordersId);
        if (ordersVO == null) {
            return ApiResult.error("order not found");
        }
        if (!Objects.equals(ordersVO.getUserId(), LocalThreadHolder.getUserId())) {
            return ApiResult.error("only the buyer can confirm trade completion");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_CANCELLED)) {
            return ApiResult.error("reservation has already been cancelled");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_COMPLETED)) {
            return ApiResult.error("order has already been completed");
        }
        if (!Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_RESERVED)
                && !Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_PARTIAL_CONFIRMED)) {
            return ApiResult.error("seller has not confirmed the reservation yet");
        }
        if (Boolean.TRUE.equals(ordersVO.getIsConfirm()) || ordersVO.getBuyerConfirmTime() != null) {
            return ApiResult.error("buyer has already confirmed this trade");
        }

        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setIsConfirm(true);
        LocalDateTime confirmTime = LocalDateTime.now();
        orders.setBuyerConfirmTime(confirmTime);

        if (Boolean.TRUE.equals(ordersVO.getIsRefundConfirm()) || ordersVO.getSellerFinishTime() != null) {
            orders.setTradeStatus(ORDER_STATUS_COMPLETED);
            ordersMapper.update(orders);
            markProductAsSold(ordersVO.getProductId());
            return ApiResult.success("both parties confirmed completion, product marked as SOLD");
        }

        orders.setTradeStatus(ORDER_STATUS_PARTIAL_CONFIRMED);
        ordersMapper.update(orders);
        return ApiResult.success("buyer confirmed meetup completion, waiting for seller confirmation");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<String> confirmTradeBySeller(Integer ordersId) {
        OrdersVO ordersVO = getOrderById(ordersId);
        if (ordersVO == null) {
            return ApiResult.error("order not found");
        }
        if (!Objects.equals(ordersVO.getSellerId(), LocalThreadHolder.getUserId())) {
            return ApiResult.error("only the seller can confirm trade completion");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_CANCELLED)) {
            return ApiResult.error("reservation has already been cancelled");
        }
        if (Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_COMPLETED)) {
            return ApiResult.error("order has already been completed");
        }
        if (!Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_RESERVED)
                && !Objects.equals(ordersVO.getTradeStatus(), ORDER_STATUS_PARTIAL_CONFIRMED)) {
            return ApiResult.error("current order is not in the completion confirmation stage");
        }
        if (Boolean.TRUE.equals(ordersVO.getIsRefundConfirm()) || ordersVO.getSellerFinishTime() != null) {
            return ApiResult.error("seller has already confirmed this trade");
        }

        Orders orders = new Orders();
        orders.setId(ordersId);
        orders.setIsRefundConfirm(true);
        LocalDateTime confirmTime = LocalDateTime.now();
        orders.setSellerFinishTime(confirmTime);

        if (Boolean.TRUE.equals(ordersVO.getIsConfirm()) || ordersVO.getBuyerConfirmTime() != null) {
            orders.setTradeStatus(ORDER_STATUS_COMPLETED);
            ordersMapper.update(orders);
            markProductAsSold(ordersVO.getProductId());
            return ApiResult.success("both parties confirmed completion, product marked as SOLD");
        }

        orders.setTradeStatus(ORDER_STATUS_PARTIAL_CONFIRMED);
        ordersMapper.update(orders);
        return ApiResult.success("seller confirmed meetup completion, waiting for buyer confirmation");
    }

    @Override
    public Result<List<ChartVO>> queryProductInfo(ProductQueryDto productQueryDto) {
        List<Integer> productIds = productMapper.queryProductIds(productQueryDto.getUserId());
        if (productIds == null || productIds.isEmpty()) {
            return ApiResult.success(new ArrayList<ChartVO>());
        }
        List<Interaction> interactionList = interactionMapper.queryByProductIds(productIds);
        long viewCount = getProductCount(interactionList, InteractionEnum.VIEW.getType());
        long saveCount = getProductCount(interactionList, InteractionEnum.SAVE.getType());
        long loveCount = getProductCount(interactionList, InteractionEnum.LOVE.getType());
        List<ChartVO> chartVOList = new ArrayList<ChartVO>();
        chartVOList.add(new ChartVO("Product Views", (int) viewCount));
        chartVOList.add(new ChartVO("Product Favorites", (int) saveCount));
        chartVOList.add(new ChartVO("Product Wants", (int) loveCount));
        return ApiResult.success(chartVOList);
    }

    private long getProductCount(List<Interaction> interactionList, Integer type) {
        return interactionList.stream()
                .filter(interaction -> Objects.equals(type, interaction.getType()))
                .count();
    }

    @Override
    public Result<List<ProductVO>> queryProductList(Integer id) {
        ProductVO productVO = productMapper.queryById(id);
        if (productVO == null) {
            return ApiResult.error("product not found");
        }
        ProductQueryDto productQueryDto = new ProductQueryDto();
        productQueryDto.setUserId(productVO.getUserId());
        List<ProductVO> productVOS = productMapper.query(productQueryDto);
        return ApiResult.success(productVOS);
    }

    private ProductVO getProductById(Integer productId) {
        return productMapper.queryById(productId);
    }

    private OrdersVO getOrderById(Integer ordersId) {
        if (ordersId == null) {
            return null;
        }
        OrdersQueryDto queryDto = new OrdersQueryDto();
        queryDto.setId(ordersId);
        List<OrdersVO> ordersVOList = ordersMapper.query(queryDto);
        if (ordersVOList == null || ordersVOList.isEmpty()) {
            return null;
        }
        return ordersVOList.get(0);
    }

    private boolean hasActiveReservation(Integer productId) {
        OrdersQueryDto queryDto = new OrdersQueryDto();
        queryDto.setProductId(productId);
        List<OrdersVO> orders = ordersMapper.query(queryDto);
        if (orders == null || orders.isEmpty()) {
            return false;
        }
        for (OrdersVO order : orders) {
            if (isActiveOrderStatus(order.getTradeStatus())) {
                return true;
            }
        }
        return false;
    }

    private boolean isActiveOrderStatus(Integer tradeStatus) {
        return Objects.equals(tradeStatus, ORDER_STATUS_PENDING_CONFIRM)
                || Objects.equals(tradeStatus, ORDER_STATUS_RESERVED)
                || Objects.equals(tradeStatus, ORDER_STATUS_PARTIAL_CONFIRMED);
    }

    private void restoreProductToSale(Integer productId) {
        if (productId == null || hasActiveReservation(productId)) {
            return;
        }
        ProductVO productVO = getProductById(productId);
        if (productVO == null) {
            return;
        }
        if (PRODUCT_STATUS_SOLD.equals(resolveProductStatus(productVO.getStatus()))) {
            return;
        }
        Product product = new Product();
        product.setId(productId);
        product.setStatus(PRODUCT_STATUS_ON_SALE);
        product.setInventory(1);
        productMapper.update(product);
    }

    private void markProductAsSold(Integer productId) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(PRODUCT_STATUS_SOLD);
        product.setInventory(0);
        productMapper.update(product);
    }

    private String resolveProductStatus(String status) {
        return StringUtils.hasText(status) ? status : PRODUCT_STATUS_ON_SALE;
    }
}

