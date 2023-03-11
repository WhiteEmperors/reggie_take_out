package com.itheima.reggie.controller;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;


    /**
     * 员工登录
     * @param request
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login (HttpServletRequest request, @RequestBody Employee employee){
        //1.将页面传递过来的数据进行md5加密
        String password = employee.getPassword();
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        //2.进行数据库的比对，查看页面传过来的数据是否和数据库一致
        LambdaQueryWrapper<Employee> queryChainWrapper = new LambdaQueryWrapper<>();
        queryChainWrapper.eq(Employee::getUsername,employee.getUsername());
        Employee emp = employeeService.getOne(queryChainWrapper);//拿到比对结果
        //3. 如果没有查询到则返回登录失败的结果
        if(emp == null){
            return R.error("登录失败,用户名或密码错误");
        }
        //4.密码错误返回登录失败的结果
        if (!emp.getPassword().equals(password)){
            return R.error("登录失败,密码错误");
        }
        //5. 查看员工状态，如果为金庸状态，则返回员工已禁用结果
        if (emp.getStatus() == 0){
            return R.error("账号以禁用");
        }
        //6.登录成功，员工id存入Session并返回成功结果
        request.getSession().setAttribute("employee",emp.getId());
        return R.success(emp);
    }

    /**
     * 退出功能
     * @param request
     * @return
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){    //清除Session中保存的当前员工信息的id
        request.getSession().removeAttribute("employee");
        return R.success("退出成功");
    }

    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping
    public R<String> save(HttpServletRequest request,@RequestBody Employee employee){
        log.info("新增员工，员工信息：{}",employee.toString());

        //设置初始密码123456，需要进行md5加密处理
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));

        /*employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());

        //获得当前登录用户的id
        Long empId = (Long) request.getSession().getAttribute("employee");

        employee.setCreateUser(empId);
        employee.setUpdateUser(empId);*/

        employeeService.save(employee);
        return R.success("新增员工成功");
    }

    /**
     * 分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> Page(Integer page, Integer pageSize, String name){
        log.info("page = {},pageSize = {},name = {}",page,pageSize,name);
        //构造分页器
        Page pageInfo = new Page(page,pageSize);

        //构造分页条件
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        //添加过滤条件  不为空的时候走该条件
        queryWrapper.like(StringUtils.isNotEmpty(name), Employee::getName, name);
        //添加排除条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        //执行查询
        employeeService.page(pageInfo,queryWrapper);
        return R.success(pageInfo);
    }

    /**
     * 修改
     * @param request
     * @param employee
     * @return
     */
    @PutMapping
    public R update(HttpServletRequest request, @RequestBody Employee employee){

        /*Long empId = (Long) request.getSession().getAttribute("employee");
        //设置修改时间
        employee.setUpdateTime(LocalDateTime.now());
        //修改前端传过来的Id
        employee.setUpdateUser(empId);*/
        //将数据封装到employeeService
        employeeService.updateById(employee);
        return R.success("员工修改信息成功");//返回成功信息
    }

    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息...");
        Employee employee = employeeService.getById(id);
        if (employee !=null){
            return R.success(employee);
        }
        return R.error("没有查询到对应员工的信息");
    }

}
