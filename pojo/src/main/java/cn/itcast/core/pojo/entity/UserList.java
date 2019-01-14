package cn.itcast.core.pojo.entity;

import cn.itcast.core.pojo.user.User;

import java.io.Serializable;
import java.util.List;

public class UserList implements Serializable {

    //活跃用户集合
    private List<User> userListhuoyue;
    //非活跃用户集合
    private List<User> userListNOhuoyue;
    //用户总数
    private Long userALL;
    //活跃用户个数
    private  Long userActice;
    //非用户活跃个数
    private  Long userNoActice;

    public List<User> getUserListhuoyue() {
        return userListhuoyue;
    }

    public void setUserListhuoyue(List<User> userListhuoyue) {
        this.userListhuoyue = userListhuoyue;
    }

    public List<User> getUserListNOhuoyue() {
        return userListNOhuoyue;
    }

    public void setUserListNOhuoyue(List<User> userListNOhuoyue) {
        this.userListNOhuoyue = userListNOhuoyue;
    }

    public Long getUserALL() {
        return userALL;
    }

    public void setUserALL(Long userALL) {
        this.userALL = userALL;
    }

    public Long getUserActice() {
        return userActice;
    }

    public void setUserActice(Long userActice) {
        this.userActice = userActice;
    }

    public Long getUserNoActice() {
        return userNoActice;
    }

    public void setUserNoActice(Long userNoActice) {
        this.userNoActice = userNoActice;
    }
}
