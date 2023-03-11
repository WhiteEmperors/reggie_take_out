package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/setmeal")
public class SetmealController {

    @Autowired
    private SetmealDishService setmealDishService;

    @Autowired
    private SetmealService setmealService;

    @Autowired
    private CategoryService categoryService;


    /**
     *  保存套餐
     *  这段代码是一个基于SpringMVC开发的Controller处理POST请求的注解方法。
     *  它使用@PostMapping注解标注，表示处理HTTP POST请求。该方法通过使用@RequestBody注解来接收请求体中的JSON数据并将其转换为SetmealDto对象。
     *  SetmealDto包含有保存套餐相关信息的属性，例如名称、描述、价格等。
     *  最后，该方法返回一个R对象，该对象包含一个String类型的message属性，表示请求处理的结果消息。
     * @param setmealDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody SetmealDto setmealDto){
        /**
         * 这段代码使用了log4j2的日志输出功能，将一个包含套餐信息的DTO对象输出到日志中。
         * 具体解释如下：
         *      - log：是一个Logger对象，用来记录日志。
         *      - .info()：是Logger对象的一个方法，表示输出“普通”级别的日志。
         *      - "套餐信息：{}"：是输出日志的格式字符串，其中的{}表示需要插入一个参数。
         *      - ,setmealDto：是需要插入到日志格式字符串中的参数，即一个包含套餐信息的DTO对象。
         *  综合起来，这段代码的作用是将一个包含套餐信息的DTO对象以指定的格式输出到日志中，便于开发者在调试和排查问题时查看相关信息
         */
        log.info("套餐信息：{}",setmealDto);
        setmealService.saveWithDish(setmealDto);
        return R.success("新增套餐成功");
    }

    /**
     * 套餐分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page,int pageSize,String name){
        //分页构造器对象
        Page<Setmeal> pageInfo = new Page<>(page,pageSize);
        Page<SetmealDto> dtoPage = new Page<>();

        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        //添加查询条件，根据name进行like模糊查询
        queryWrapper.like(name != null,Setmeal::getName,name);
        //添加排序条件，根据更新时间降序排列
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        setmealService.page(pageInfo,queryWrapper);

        //5. 对象拷贝  将pageInfo中的数据拷贝到dtoPage中， records属性被忽略
        /**
         * 对拷贝进行解释
         * 这段代码是使用BeanUtils工具将PageInfo对象中的属性复制到dtoPage对象中。
         * 其中，“records”属性被忽略，不会被复制到dtoPage对象中。
         * 具体来说，BeanUtils.copyProperties()方法使用Java反射机制将一个JavaBean对象中的属性值复制到另一个JavaBean对象中。
         * 第一个参数指定源对象，第二个参数指定目标对象，第三个参数是要忽略复制的属性列表。
         * 在这个例子中，我们指定了“records”属性作为忽略属性。这意味着在复制完成后，目标对象中将不会有“records”属性。
         */
        BeanUtils.copyProperties(pageInfo,dtoPage,"records");
        /**
         * 对pageInfo.getRecords()解析
         * 这段代码的含义是，从一个分页信息对象 pageInfo 中获取当前页面的数据记录列表，将其赋值给一个类型为 Setmeal 的列表 records。
         * List 是 Java 中的集合类型，它可以存储多个元素，并且支持对元素进行添加、删除、查询等操作。
         * Setmeal 则是一个自定义的数据类型，用于表示某个套餐项目。
         * pageInfo 可能是一个分页插件的返回值，它包含了分页查询的相关信息，比如当前页的页码、每页显示的记录数、总记录数等。
         * 通过调用 getRecords() 方法，可以获取当前页的数据记录列表。
         *      这个列表中的每个元素都是一个 Setmeal 对象，代表当前页的一个套餐项目。
         *      最后将这些项目存储到一个名为 records 的列表中，以便后续进行处理或展示。
         */
        List<Setmeal> records = pageInfo.getRecords();

        List<SetmealDto> list = records.stream().map((item) -> {
            SetmealDto setmealDto = new SetmealDto();
            //对象拷贝
            BeanUtils.copyProperties(item,setmealDto);
            //分类id
            Long categoryId = item.getCategoryId();
            //根据分类id查询分类对象
            Category category = categoryService.getById(categoryId);
            if(category != null){
                //分类名称
                String categoryName = category.getName();
                setmealDto.setCategoryName(categoryName);
            }
            return setmealDto;
        }).collect(Collectors.toList());
        dtoPage.setRecords(list);
        return R.success(dtoPage);
    }

    /**
     * 删除套餐
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(@RequestParam List<Long> ids){
        log.info("ids:{}",ids);
        setmealService.removeWithDish(ids);
        return R.success("套餐数据删除成功");
    }

    /**
     * 根据条件查询套餐数据
     * @param setmeal
     * @return
     */
    @GetMapping("/list")
    public R<List<Setmeal>> list(Setmeal setmeal){
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(setmeal.getCategoryId() != null,Setmeal::getCategoryId,setmeal.getCategoryId());
        queryWrapper.eq(setmeal.getStatus() != null,Setmeal::getStatus,setmeal.getStatus());
        queryWrapper.orderByDesc(Setmeal::getUpdateTime);

        List<Setmeal> list = setmealService.list(queryWrapper);

        return R.success(list);
    }

}



























