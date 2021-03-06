package cn.itcast.core.service;

import cn.itcast.core.dao.log.PayLogDao;
import cn.itcast.core.dao.order.OrderDao;
import cn.itcast.core.dao.order.OrderItemDao;
import cn.itcast.core.pojo.entity.BuyerCart;
import cn.itcast.core.pojo.entity.PageResult;
import cn.itcast.core.pojo.log.PayLog;
import cn.itcast.core.pojo.order.Order;
import cn.itcast.core.pojo.order.OrderItem;
import cn.itcast.core.pojo.order.OrderQuery;
import cn.itcast.core.util.Constants;
import cn.itcast.core.util.IdWorker;
import com.alibaba.dubbo.config.annotation.Service;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Transactional
public class OrderServiceImpl implements  OrderService {

    @Autowired
    private PayLogDao payLogDao;

    @Autowired
    private OrderDao orderDao;

    @Autowired
    private OrderItemDao orderItemDao;

    @Autowired
    private RedisTemplate redisTemplate;

    @Autowired
    private IdWorker idWorker;

    private long aLong=0;

    private Map<String,String> orderMap;

    @Override
    public void add(Order order) {
        aLong=System.currentTimeMillis();
        //1. 从订单对象中获取当前登录用户用户名
        String userId = order.getUserId();
        //2. 根据用户名获取购物车集合
        List<BuyerCart> cartList = (List<BuyerCart>)redisTemplate.boundHashOps(Constants.CART_LIST_REDIS).get(userId);
        List<String> orderIdList=new ArrayList();//订单ID列表
        double total_money=0;//总金额 （元）
         toDate();
        //3. 遍历购物车集合
        if (cartList != null) {
            for (BuyerCart cart : cartList) {
                //TODO 4. 根据购物车对象保存订单数据
                long orderId = idWorker.nextId();
               // System.out.println("sellerId:"+cart.getSellerId());
                Order tborder=new Order();//新创建订单对象
                tborder.setOrderId(orderId);//订单ID
                tborder.setUserId(order.getUserId());//用户名
                tborder.setPaymentType(order.getPaymentType());//支付类型
                tborder.setStatus("1");//状态：未付款
                tborder.setCreateTime(new Date());//订单创建日期
                tborder.setUpdateTime(new Date());//订单更新日期
                tborder.setReceiverAreaName(order.getReceiverAreaName());//地址
                tborder.setReceiverMobile(order.getReceiverMobile());//手机号
                tborder.setReceiver(order.getReceiver());//收货人
                tborder.setSourceType(order.getSourceType());//订单来源
                tborder.setSellerId(cart.getSellerId());//商家ID
                //循环购物车明细
                double money=0;
                orderMap=new HashMap<>();
                Date date = new Date();
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");
                String format = simpleDateFormat.format(date);

                orderMap.put("orderId",String.valueOf(orderId));
                orderMap.put("time",format);
                orderMap.put("type",order.getPaymentType());

                //5. 从购物车中获取购物项集合
                List<OrderItem> orderItemList = cart.getOrderItemList();
                //6. 遍历购物项集合
                if (orderItemList != null) {
                    for (OrderItem orderItem : orderItemList) {
                        //TODO 7. 根据购物项对象保存订单详情数据
                        orderItem.setId(idWorker.nextId());
                        orderItem.setOrderId( orderId  );//订单ID
                        orderItem.setSellerId(cart.getSellerId());
                        money+=orderItem.getTotalFee().doubleValue();//金额累加
                        orderItemDao.insertSelective(orderItem);

                    }
                }
                tborder.setPayment(new BigDecimal(money));
                orderDao.insertSelective(tborder);
                orderIdList.add(orderId+"");//添加到订单列表
                total_money+=money;//累加到总金额

            }
        }
        //TODO 8. 计算总价钱保存支付日志数据
        if("1".equals(order.getPaymentType())){//如果是微信支付
            PayLog payLog=new PayLog();
            String outTradeNo=  idWorker.nextId()+"";//支付订单号
            payLog.setOutTradeNo(outTradeNo);//支付订单号
            payLog.setCreateTime(new Date());//创建时间
            //订单号列表，逗号分隔
            String ids=orderIdList.toString().replace("[", "").replace("]", "").replace(" ", "");
            payLog.setOrderList(ids);//订单号列表，逗号分隔
            payLog.setPayType("1");//支付类型
            payLog.setTotalFee( (long)(total_money*100 ) );//总金额(分)
            payLog.setTradeState("0");//支付状态
            payLog.setUserId(order.getUserId());//用户ID
            payLogDao.insertSelective(payLog);//插入到支付日志表
            //TODO 9. 使用当前登录用户的用户名作为key, 支付日志对象作为value存入redis中供支付使用
            redisTemplate.boundHashOps("payLog").put(order.getUserId(), payLog);//放入缓存
        }
        //TODO 10. 根据当前登录用户的用户名删除购物车
        redisTemplate.boundHashOps(Constants.CART_LIST_REDIS).delete(order.getUserId());

    }
    //计算支付超时
    public long toDate(){
         //计算超时时间
        return aLong;

    }

    //获取订单编号,下订单时间
    public Map<String,String> timeAndOrderId(){
        return orderMap;
    }

    @Override
    public PayLog getPayLogByUserName(String userName) {

        PayLog payLog = (PayLog)redisTemplate.boundHashOps("payLog").get(userName);
        return payLog;
    }

    @Override
    public void updatePayStatus(String userName) {
        //1. 根据登录用户的用户名, 获取redis中的支付日志对象
        PayLog payLog = (PayLog)redisTemplate.boundHashOps("payLog").get(userName);

        //2. 根据支付日志对象修改数据库中的支付状态
        payLog.setTradeState("1");
        payLog.setPayTime(new Date());
        payLogDao.updateByPrimaryKeySelective(payLog);

        //3. 根据订单id修改订单表的支付状态
        String orderListStr = payLog.getOrderList();
        String[] split = orderListStr.split(",");
        if (split != null) {
            for (String orderId : split) {
                Order order = new Order();
                order.setOrderId(Long.parseLong(orderId));
                order.setStatus("2");
                orderDao.updateByPrimaryKeySelective(order);
            }
        }

        //4. 删除redis中这个用户的支付日志对象
        redisTemplate.boundHashOps("payLog").delete(userName);
    }

    @Override
    public List<Order> findAll() {
        List<Order> orderList = orderDao.selectByExample(null);
        return orderList;
    }


    /**
     * 条件查询加分页,如果没有条件,查询所有订单
     * @param order
     * @param page
     * @param rows
     * @return
     */
    @Override
    public PageResult findPage(Order order, Integer page, Integer rows) {
        //利用分页助手
        PageHelper.startPage(page,rows);
        OrderQuery query = new OrderQuery();
        OrderQuery.Criteria criteria = query.createCriteria();
        if (order != null){
            if (order.getOrderId() != null && !"".equals(order.getOrderId())){
                criteria.andOrderIdEqualTo(order.getOrderId());
            }
        }
        Page<Order> orderList =(Page<Order>) orderDao.selectByExample(query);
        return new PageResult(orderList.getTotal(), orderList.getResult());
    }

    @Override
    public Order findById(Long orderId) {
        Order order = orderDao.selectByPrimaryKey(orderId);
        return order;
    }

    @Override
    public Map<String ,Object> findTotalMoney() {
        //建立数组存放日期和总金额
        List<String> dateList = new ArrayList<>();
        List<BigDecimal> moneyList = new ArrayList<>();
        Map<String ,Object> map = new HashMap<>();

        //获取要统计金额的时间
        //1.定义时间转换格式,用来去掉时分秒
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        //2.获取日历对象
        Calendar calendar = Calendar.getInstance();

        for (int i = -7; i < 0; i++) {
            Date startDate = new Date();
            try {
                startDate = sdf.parse(sdf.format(startDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH,i);
            startDate = calendar.getTime();
            //3.获取一天开始时间
            String s = sdf.format(startDate).toString();
            dateList.add(s);
//            System.out.println(startDate.toString());
            //4.获取一天的结束时间
            calendar.setTime(startDate);
            calendar.add(Calendar.DAY_OF_MONTH,1);
            Date endDate = calendar.getTime();
            try {
                endDate = sdf.parse(sdf.format(endDate));
            } catch (ParseException e) {
                e.printStackTrace();
            }
//            System.out.println(endDate);

            //查询一天的总销售额
            OrderQuery query = new OrderQuery();
            OrderQuery.Criteria criteria = query.createCriteria();
            criteria.andCreateTimeBetween(startDate,endDate);
            List<Order> orderList = orderDao.selectByExample(query);

            //初始化金额统计对象
            int b = 0;
            BigDecimal totalMoney = new BigDecimal(b);

            //遍历数组,求总金额
            if (orderList != null && orderList.size() >0){
                for (Order order : orderList) {
                    BigDecimal payment = order.getPayment();
                    totalMoney = totalMoney.add(payment);
                }
            }
            moneyList.add(totalMoney);
        }
        map.put("moneyList",moneyList);
        map.put("dateList",dateList);
        return map;
    }
}
