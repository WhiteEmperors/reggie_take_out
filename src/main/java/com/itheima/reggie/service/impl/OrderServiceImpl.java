package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {

    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;

    /**
     * 用户下单
     *
     * @param orders
     */
    @Override
    public void subimt(Orders orders) {
        //获取当前用户id
        Long userId = BaseContext.getCurrentId();

        //查询当前用户的购物车  用户进行比对，然后shoppingCartService中list查出
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId,userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        //判断shoppingCarts
        if (shoppingCarts == null || shoppingCarts.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }
        //查询用户数据
        User user = userService.getById(userId);
        //查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        //地址为空的时候抛出异常
        if (addressBook == null){
            throw new  CustomException("用户地址信息有无，不能下单");
        }
        long orderId = IdWorker.getId();

        /**
         * 这段代码创建了一个AtomicInteger对象，名为amount，并初始化为0。
         * AtomicInteger是一个Java原子类，它提供了一种线程安全的方式来进行自增、自减和其他数学计算。
         *      与普通的整数类型不同，AtomicInteger可以保证在多线程环境中进行操作时，不会出现竞态条件（race condition）和数据不一致的问题。
         * 因此，当多个线程需要对同一个计数器进行操作时，使用AtomicInteger可以保证线程安全性和正确性。
         */
        AtomicInteger amount = new AtomicInteger(0);

        //组装订单明细信息
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            /**
             * 具体来说，它使用了Java中的AtomicInteger类的addAndGet方法，对计数器进行了原子性操作，保证了线程安全性。
             * 在具体实现中，它首先通过item.getAmount()获取商品的单价，再通过item.getNumber()获取商品的数量，
             *      将二者相乘得到该商品的总价，然后将总价转换为int类型，并将其加入到计数器中。
             * 简而言之，这段代码的作用是计算一个商品的总价，并将其加入到计数器中。
             *
             * amount：一个计数器，表示某种商品的总数量。
             * addAndGet()：这是一个原子性操作，用于对amount进行加操作，并返回加操作后的值。
             * item.getAmount()：表示获取某个商品的单价。
             * item.getNumber()：表示获取某个商品的数量。
             * multiply()：表示将单价乘以数量，得到该商品的总价。
             * new BigDecimal()：表示创建一个BigDecimal类型的对象，用于存储计算结果。
             * intValue()：表示将BigDecimal类型的对象转换成int类型，用于加到计数器中。
             * 综上所述，这段代码的作用是将一个订单中所有商品的总价加到一个计数器中，以便统计订单的总价值。
             */
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        //组装订单数据
        orders.setId(orderId);
        orders.setOrderTime(LocalDateTime.now());
        orders.setCheckoutTime(LocalDateTime.now());
        orders.setStatus(2);
        orders.setAmount(new BigDecimal(amount.get()));//总金额
        orders.setUserId(userId);
        orders.setNumber(String.valueOf(orderId));
        orders.setUserName(user.getName());
        orders.setConsignee(addressBook.getConsignee());
        orders.setPhone(addressBook.getPhone());
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + (addressBook.getDistrictName() == null ? "" : addressBook.getDistrictName())
                + (addressBook.getDetail() == null ? "" : addressBook.getDetail()));
        //向订单表插入数据，一条数据
        this.save(orders);

        //向订单明细表插入数据，多条数据
        orderDetailService.saveBatch(orderDetails);

        //清空购物车数据
        shoppingCartService.remove(wrapper);

    }
}