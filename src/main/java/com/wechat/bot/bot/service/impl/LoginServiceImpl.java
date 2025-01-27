package com.wechat.bot.bot.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.wechat.bot.bot.service.LoginService;
import com.wechat.bot.config.SystemConfig;
import com.wechat.bot.gewechat.service.LoginApi;
import com.wechat.bot.util.FileUtil;
import com.wechat.bot.util.IpUtil;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * @author Alex
 * @since 2025/1/27 12:01
 * <p></p>
 */
@Slf4j
@Service
public class LoginServiceImpl implements LoginService {

    @Resource
    SystemConfig systemConfig;

    @Override
    public void login() {

        //检查设施是否在线
        JSONObject checkOnline = LoginApi.checkOnline(systemConfig.getAppId());
        if (checkOnline.getInteger("ret") == 200) {
            log.info("{}", checkOnline);
            log.info("AppId : {} 已在线，无需登录", systemConfig.getAppId());
            return;
        }
        log.info("APPid:{}", "并未在线，开始执行登录流程");
        /**
         * 1.获取token
         */
        int totalCount = 10;
        int retryCount = 0;
        while (retryCount < totalCount) {
            JSONObject response = LoginApi.getToken();
            if (response.getInteger("ret") == 200) {
                String token = response.getString("data");
                if (token != null) {
                    systemConfig.setToken(token);
                    FileUtil.writeFile(systemConfig);
                    break;

                }
            }else {
                retryCount++;
                try {
                    Files.deleteIfExists(FileUtil.configFilePath);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /**
         *3、 获取登录二维码
         * @param appId   设备id 首次登录传空，后续登录传返回的appid
         */
        String appId = "";
        String uuid = "";
        retryCount = 0;
        while (retryCount <= totalCount) {
            appId = "";
            JSONObject response = LoginApi.getQr("");
            if (response.getInteger("ret") == 200) {
                JSONObject data = response.getJSONObject("data");
                appId = data.getString("appId");
                uuid = data.getString("uuid");
                String qrData = data.getString("qrData");
                System.out.println("请访问下面地址：登录也可以");
                System.out.println("https://api.qrserver.com/v1/create-qr-code/?data=" + qrData);
                try {
                    TimeUnit.SECONDS.sleep(20);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                systemConfig.setAppId(appId);
                FileUtil.writeFile(systemConfig);
                break;
            }
            retryCount++;
        }

        /**
         * 4、确认登陆
         * @param appId

         * @param uuid       取码返回的uuid
         * @param captchCode 登录验证码（必须同省登录才能避免此问题，也能使账号更加稳定）
         */
        JSONObject jsonObject = LoginApi.checkQr(appId, uuid, null);
        if (jsonObject.getInteger("ret") != 200) {
            throw new RuntimeException("确认登录失败");

        }
        if (jsonObject.getJSONObject("data").getInteger("status") == 2) {
            log.info("登录成功，appId:{}", appId);
        }


    }

    @Override
    public void setCallbackUrl() {

        String callbackUrl = "http://" + IpUtil.getIp() + ":9919/v2/api/callback/collect";

        // 设置一下回调地址
        //System.out.println(callbackUrl);
        JSONObject setCallback = LoginApi.setCallback(systemConfig.getToken(), callbackUrl);
        if (setCallback.getInteger("ret") != 200) {
            throw new RuntimeException("设置回调地址失败");
        }
        log.info("设置回调地址成功");
    }


}
