package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.ShoppingCart;
import com.itheima.reggie.service.ShoppingCartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/shoppingCart")
public class ShoppingCartController {

    @Autowired
    private ShoppingCartService shoppingCartService;

    /**
     * 添加购物车
     *
     * @param shoppingCart
     * @return
     */
    @PostMapping("/add")
    public R add(@RequestBody ShoppingCart shoppingCart) {
        //将当前登录的用户id封装到sopping对象中
        Long userId = BaseContext.getCurrentId();
        shoppingCart.setUserId(userId);

        //先查询对应的菜品或者套餐数据，看是否存在，如果存在number+1，否则新增数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        queryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());

        ShoppingCart shoppingCart1 = shoppingCartService.getOne(queryWrapper);
        //判断是否查询到
        if (shoppingCart1 == null) {
            shoppingCart.setNumber(1);
            shoppingCart1 = shoppingCart;
            //添加数据
            shoppingCartService.save(shoppingCart);
        } else {
            Integer number = shoppingCart1.getNumber();
            shoppingCart1.setNumber(number + 1);
            shoppingCartService.updateById(shoppingCart1);
        }

        return R.success(shoppingCart1);
    }

    /**
     * @return
     */
    @GetMapping("/list")
    public R<List<ShoppingCart>> List() {
        //获取用户id
        Long userId = BaseContext.getCurrentId();
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        wrapper.orderByDesc(ShoppingCart::getCreateTime);
        List<ShoppingCart> list = shoppingCartService.list(wrapper);
        return R.success(list);
    }

    /**
     * 清空购物车
     *
     * @return
     */
    @DeleteMapping("/clean")
    public R clean() {
        //根据用户id删除表中的数据
        //1，获取用户id
        Long userId = BaseContext.getCurrentId();
        //2.删除数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        shoppingCartService.remove(wrapper);
        return R.success("清空购物车成功");
    }

    @PostMapping("/sub")
    public R sub(@RequestBody ShoppingCart shoppingCart) {
        //1.获取用户id
        Long userId = BaseContext.getCurrentId();
        //先查询对应的菜品或者套餐数据，看是否存在，如果存在number-1，否则新增数据
        LambdaQueryWrapper<ShoppingCart> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(ShoppingCart::getUserId, userId);
        queryWrapper.eq(shoppingCart.getDishId() != null, ShoppingCart::getDishId, shoppingCart.getDishId());
        queryWrapper.eq(shoppingCart.getSetmealId() != null, ShoppingCart::getSetmealId, shoppingCart.getSetmealId());

        ShoppingCart shoppingCart1 = shoppingCartService.getOne(queryWrapper);
        Integer number = shoppingCart1.getNumber();

        if ((number - 1) >= 0) {
            shoppingCart1.setNumber(number - 1);
            shoppingCartService.updateById(shoppingCart1);
        }
        if ((number - 1) == 0){
            shoppingCartService.removeById(shoppingCart1.getId());
        }
        return R.success("2");
    }


}
