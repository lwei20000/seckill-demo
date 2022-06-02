[github]
#修改hosts文件
sudo su 
输入密码：1212
#进入编辑
vim /etc/hosts
#Note：梁威的token（注意此token只显示一次）
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
#添加：
http://github.com 204.232.175.94 
http://gist.github.com 107.21.116.220 
http://help.github.com 207.97.227.252 
http://nodeload.github.com 199.27.76.130 
http://raw.github.com 107.22.3.110 
http://status.github.com 204.232.175.78 
http://training.github.com 207.97.227.243 
http://www.github.com

[jmeter]
lwdeMacBook-Pro:bin weiliang$ pwd
/Users/weiliang/Documents/tool-for-mac/apache-jmeter-5.4.3/bin
lwdeMacBook-Pro:bin weiliang$ sh jmeter.sh
# 当并发多少的时候，QPS能达到多少/TPS是多少

[mac安装rabbitmq]
# 在终端输入下面指令
brew install rabbitmq
# 进入安装目录
cd  /usr/local/Cellar/rabbitmq/3.7.5
# 以服务方式启动，启动后终端可以关闭，不影响服务运行
brew services start rabbitmq
# 浏览器访问
在浏览器访问 http://localhost:15672/



[mac安装redis]
1.通过brew安装
    brew install redis，如果已经安装过了可以使用brew reinstall redis，进行重新安装。
2.启动redis
    2.1 通过brew：brew services start redis
    2.2 通过redis-server：redis-server，则终端页面不可以关闭，只能重新起一个终端界面
3.关闭redis
    3.1 通过brew: brew services stop redis
    3.2 通过redis-cli: redis-cli shutdown
4.连接redis
    redis-cli -h 127.0.0.1 -p 6379，默认密码为空
5.修改密码
    1.在redis中,使用命令:config set requirepass xxxx，不需要重启服务，一旦退出了redis-cli，再次进入时则需要输入密码才可以进行redis操作
    2.修改配置文件中，requirepass参数，但是需要重启redis服务
      如果采用brew安装的话，配置文件路径为：/usr/local/etc/redis.conf
6.常用命令
    6.1 鉴权: auth xxxx
    6.2 查看所有key: keys *
    6.3 查看特定前缀的key: keys xx*（在前缀后面加上*）
    6.4 新增key-value: 
        1.string: set dd dd
        2.list: LPUSH dd_list dd1（可以添加重复的value值）
        3.set: SADD dd_set dd1(可以添加重复的value值,但是会覆盖前面的一个值）
        4.zset: ZADD dd_zset 1 dd1
        5.hash: HMSET dd_hash dd1 dd1_value dd2 dd2_value
    6.5 获取特定key的值(需要根据类型进行不同的操作): 
        1.string: get dd
        2.list: LRANGE dd_list 0 10
        3.set: SMEMBERS dd_set
        4.zset: ZRANGE dd_zset 0 10
        5.hash: HMGET dd_hash dd1
    6.6 查看特定key的存储类型: type dd