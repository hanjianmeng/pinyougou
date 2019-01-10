package cn.itcast.core.controller;

import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.service.OrderService;
import com.alibaba.dubbo.config.annotation.Reference;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/orderManager")
public class OrderManagerController {

    @Reference
    private OrderService orderService;

    @RequestMapping("/findAll")
    public List<Order> findAll(){
        List<Order> list = orderService.findAll();
        return list;
    }

    /**
     * 分页查询
     * @param page 当前页
     * @param rows 每页展示数据条数
     * @return
     */
    @RequestMapping("/findPage")
    public PageResult findPage(Integer page, Integer rows) {
        PageResult result = orderService.findPage(null, page, rows);
        return result;
    }


    /**
     * 条件查询
     * @param order
     * @param page
     * @param rows
     * @return
     */
    @RequestMapping("/search")
    public PageResult search(@RequestBody Order order, Integer page, Integer rows) {
        PageResult result = orderService.findPage(order, page, rows);
        return result;
    }

    /**
     * 根据id查询某一个订单数据
     * @param orderId
     * @return
     */
    @RequestMapping("/findOne")
    public Order findById(Long orderId){
        Order order = orderService.findById(orderId);
        return order;
    }

    /**
     * 查询某一天的销售总金额
     * @param time
     * @return
     */
    @RequestMapping("/findTotalMoney")
    public String findTotalMoney(Date time){
        String totalMoney = orderService.findTotalMoney(time);
        return totalMoney;
    }
}
