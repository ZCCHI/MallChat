package com.abin.mallchat.custom.user.controller;


import com.abin.mallchat.common.common.domain.vo.response.ApiResult;
import com.abin.mallchat.common.common.utils.AssertUtil;
import com.abin.mallchat.common.common.utils.RequestHolder;
import com.abin.mallchat.common.user.dao.UserDao;
import com.abin.mallchat.common.user.domain.dto.ItemInfoDTO;
import com.abin.mallchat.common.user.domain.dto.SummeryInfoDTO;
import com.abin.mallchat.common.user.domain.entity.User;
import com.abin.mallchat.common.user.domain.enums.RoleEnum;
import com.abin.mallchat.common.user.service.IRoleService;
import com.abin.mallchat.custom.user.domain.vo.request.user.*;
import com.abin.mallchat.custom.user.domain.vo.response.user.BadgeResp;
import com.abin.mallchat.custom.user.domain.vo.response.user.UserInfoResp;
import com.abin.mallchat.custom.user.service.LoginService;
import com.abin.mallchat.custom.user.service.UserService;
import com.abin.mallchat.custom.user.service.WebSocketService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Objects;

/**
 * <p>
 * 用户表 前端控制器
 * </p>
 *
 * @author <a href="https://github.com/zongzibinbin">abin</a>
 * @since 2023-03-19
 */
@RestController
@RequestMapping("/capi/user")
@Api(tags = "用户管理相关接口")
public class UserController {
    @Autowired
    private UserService userService;

    @Autowired
    private IRoleService iRoleService;

    @GetMapping("/userInfo")
    @ApiOperation("用户详情")
    public ApiResult<UserInfoResp> getUserInfo() {
        return ApiResult.success(userService.getUserInfo(RequestHolder.get().getUid()));
    }

    @PostMapping("/public/summary/userInfo/batch")
    @ApiOperation("用户聚合信息-返回的代表需要刷新的")
    public ApiResult<List<SummeryInfoDTO>> getSummeryUserInfo(@Valid @RequestBody SummeryInfoReq req) {
        return ApiResult.success(userService.getSummeryUserInfo(req));
    }

    @PostMapping("/public/badges/batch")
    @ApiOperation("徽章聚合信息-返回的代表需要刷新的")
    public ApiResult<List<ItemInfoDTO>> getItemInfo(@Valid @RequestBody ItemInfoReq req) {
        return ApiResult.success(userService.getItemInfo(req));
    }

    @PutMapping("/name")
    @ApiOperation("修改用户名")
    public ApiResult<Void> modifyName(@Valid @RequestBody ModifyNameReq req) {
        userService.modifyName(RequestHolder.get().getUid(), req);
        return ApiResult.success();
    }

    @GetMapping("/badges")
    @ApiOperation("可选徽章预览")
    public ApiResult<List<BadgeResp>> badges() {
        Long uid = 10003L;
        return ApiResult.success(userService.badges(RequestHolder.get().getUid() == null?uid:RequestHolder.get().getUid()));
    }

    @PutMapping("/badge")
    @ApiOperation("佩戴徽章")
    public ApiResult<Void> wearingBadge(@Valid @RequestBody WearingBadgeReq req) {
        userService.wearingBadge(RequestHolder.get().getUid(), req);
        return ApiResult.success();
    }

    @PutMapping("/black")
    @ApiOperation("拉黑用户")
    public ApiResult<Void> black(@Valid @RequestBody BlackReq req) {
        Long uid = RequestHolder.get().getUid();
        boolean hasPower = iRoleService.hasPower(uid, RoleEnum.ADMIN);
        AssertUtil.isTrue(hasPower, "没有权限");
        userService.black(req);
        return ApiResult.success();
    }

    @Autowired
    private UserDao userDao;
    @Autowired
    private LoginService loginService;

    @Autowired
    @Lazy
    private WebSocketService webSocketService;

    @GetMapping("scan/{code}/{id}")
    public void  scaLogin(@PathVariable("code") Integer code,@PathVariable("id")Long id){
       // User user = userDao.getByOpenId(fromUser);
        UserInfoResp user = userService.getUserInfo(id);

        if (Objects.nonNull(user) && StringUtils.isNotEmpty(user.getAvatar())) {
            //注册且已经授权的用户，直接登录成功
            login(user.getId(), code);
        }
    }

    private void login(Long uid, Integer eventKey) {
        User user = userDao.getById(uid);
        //调用用户登录模块
        String token = loginService.login(uid);
        //推送前端登录成功
        webSocketService.scanLoginSuccess(eventKey, user, token);
    }

}

