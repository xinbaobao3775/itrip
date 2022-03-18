package cn.itrip.controller;

import cn.itrip.common.*;
import cn.itrip.dao.itripUser.ItripUserMapper;
import cn.itrip.dao.itripUserLinkUser.ItripUserLinkUserMapper;
import cn.itrip.pojo.ItripUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;

import javax.servlet.http.HttpServletRequest;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Random;


@RequestMapping("api")
@RestController
public class AuthController {
    ObjectMapper o=new ObjectMapper();
    TokenBiz t=new TokenBiz();
    @Autowired
    ItripUserMapper um;
    @Autowired
    ItripUserMapper mp;
    @Autowired
    redisUtil ru;
    @Autowired
    SDKTestSendTemplateSMS sms;
    @RequestMapping("dologin")
    public Dto login(HttpServletRequest req, @RequestParam("name") String name, @RequestParam("password") String password)
    throws Exception{
        val m=new HashMap();
        m.put("userName",name);
        m.put("userPassword",password);

        ItripUser user=null;
        try {
            user = (ItripUser) um.getItripUserListByMap(m).get(0);
        } catch (Exception e) {
            return DtoUtil.returnFail("登录失败","1000");
        }
            String token = t.generateToken(req.getHeader("User-Agent"), user);
            Jedis j=ru.getJedis();
            j.set(token, this.o.writeValueAsString(user));
            ItripTokenVO vo=new ItripTokenVO(token,
                    Calendar.getInstance().getTimeInMillis()*3600*2,
                    Calendar.getInstance().getTimeInMillis());
            j.close();
            return DtoUtil.returnDataSuccess(this.o.writeValueAsString(vo));
    }
    @PostMapping("registerbyphone")
    public Dto Register(@RequestBody ItripUserVO vo) throws Exception {
        if (mp.insertItripUser(vo)>0){
            String authCode=""+new Random().nextInt(9999);
            sms.SMS(vo.getUserCode(),authCode);
            Jedis j = ru.getJedis();
            j.set(vo.getUserCode(),authCode);
            j.close();
            return DtoUtil.returnSuccess();
        }
        return DtoUtil.returnFail("失败","100421");
    }
    @PutMapping("validatephone")
    public Dto validate(String user ,String code) throws Exception {
        Jedis j = ru.getJedis();
        if (j.get(user).equals(code)){
            j.close();
            return DtoUtil.returnSuccess();
        }
        return DtoUtil.returnFail("失败","111");
    }
}
