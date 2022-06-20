package com.example.seckilldemo;

import com.example.seckilldemo.controller.TUserController;
import com.example.seckilldemo.entity.TUser;
import com.example.seckilldemo.mapper.TUserMapper;
import com.example.seckilldemo.service.ITUserService;
import com.example.seckilldemo.utils.MD5Util;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import javax.validation.Validator;
import java.util.*;
import java.util.concurrent.TimeUnit;

@SpringBootTest
class SeckillDemoApplicationTests {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RedisScript<Boolean> lockScript;

    /**
     * 问题：如果中间抛出异常，K1没有被删除，会导致死锁
     */
    @Test
    public void testLock01() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //占位，如果key不存在才可以设置成功
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1");
        if (isLock) {
            valueOperations.set("name", "xxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name=" + name);
            //操作结束，删除锁
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程在使用，请稍后再试");
        }
    }

    /**
     * 死锁问题解决：setIfAbsent配置超时时间：
     *
     * 问题：
     *      第一个线程获取到锁然后超时锁不见了，线程还在继续执行（耗时操作）；
     *      第二个线程进入上锁，
     *      此时：加入第一个线程执行结束会删除锁，此时删掉的是第二个线程的锁。乱序问题。
     */
    @Test
    public void testLock2() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        //给锁添加一个过期时间
        Boolean isLock = valueOperations.setIfAbsent("k1", "v1", 5, TimeUnit.SECONDS);
        if (isLock) {
            valueOperations.set("name", "xxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name=" + name);
            //操作结束，删除锁
            redisTemplate.delete("k1");
        } else {
            System.out.println("有线程在使用，请稍后再试");
        }
    }

    /**
     * 解决乱序问题：K1的值采用随机值，删除的时候根据随机值判断是不是自己的锁。
     */
    @Test
    public void testLock3() {
        ValueOperations valueOperations = redisTemplate.opsForValue();
        String value = UUID.randomUUID().toString();
        Boolean isLock = valueOperations.setIfAbsent("k1", value, 5, TimeUnit.SECONDS);
        if (isLock) {
            valueOperations.set("name", "xxx");
            String name = (String) valueOperations.get("name");
            System.out.println("name=" + name);
            //操作结束，删除锁
            System.out.println(valueOperations.get("k1"));
            Boolean result = (Boolean) redisTemplate.execute(lockScript, Collections.singletonList("k1"), value);
            System.out.println(result);
        } else {
            System.out.println("有线程在使用，请稍后再试");
        }
    }
}
