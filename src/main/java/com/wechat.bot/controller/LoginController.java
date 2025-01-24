package com.wechat.bot.controller;

import com.alibaba.fastjson2.JSONObject;


import com.wechat.bot.service.LoginApi;
import com.wechat.bot.util.OkhttpUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import static com.wechat.bot.util.QRCodeUtil.generateQRCodeBase64;


/**
 * @author Alex
 * @since 2025/1/24 11:17
 * <p></p>
 */
@Component
public class LoginController {


    @PostConstruct
    public static void login() {

        JSONObject response = LoginApi.getToken();
        String token = response.getString("data");
        System.out.println(token);
        OkhttpUtil.token = token;
        /**
         *3、 获取登录二维码
         * @param appId   设备id 首次登录传空，后续登录传返回的appid
         */
        String appId = "";
        JSONObject qr = LoginApi.getQr(appId);
        JSONObject jsonObject1 = qr.getJSONObject("data");
        appId = jsonObject1.getString("appId");
        String uuid = jsonObject1.getString("uuid");
        String qrData = jsonObject1.getString("qrData");
        System.out.println("请访问下面地址：登录也可以");
        System.out.println("https://api.qrserver.com/v1/create-qr-code/?data=" + qrData);
        //String qrImgBase64 = jsonObject1.getString("qrImgBase64");
        String pngPath = "src/main/resources/static/login.png";
        generateQRCodeBase64(qrData, 10, 15, pngPath);
        String captchCode = "123456";

        /**
         * 4、确认登陆
         * @param appId

         * @param uuid       取码返回的uuid
         * @param captchCode 登录验证码（必须同省登录才能避免此问题，也能使账号更加稳定）
         */
        JSONObject jsonObject = LoginApi.checkQr(appId, uuid, captchCode);
    }



}
