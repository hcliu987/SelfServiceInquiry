
## 使用步骤：
1. 请注意，本demo为简化代码编译时加入了lombok支持
1. 另外，新手遇到问题，请务必先阅读[【开发文档首页】](https://github.com/Wechat-Group/WxJava/wiki)的常见问题部分，可以少走很多弯路，节省不少时间。
1. 配置：复制 `/src/main/resources/application.yml.template` 或修改其扩展名生成 `application.yml` 文件，根据自己需要填写相关配置（需要注意的是：yml文件内的属性冒号后面的文字之前需要加空格，可参考已有配置，否则属性会设置不成功）；
2. 主要配置说明如下：
```
wx:
  mp:
    useRedis: false
    redisConfig:
      host: 127.0.0.1
      port: 6379
      timeout: 2000
    configs:
      - appId: 1111 # 第一个公众号的appid
        secret: 1111 # 公众号的appsecret
        token: 111 # 接口配置里的Token值
        aesKey: 111 # 接口配置里的EncodingAESKey值
      - appId: 2222 # 第二个公众号的appid，以下同上
        secret: 1111
        token: 111
        aesKey: 111

```
3. 运行Java程序：`WxMpDemoApplication`；
4. 配置微信公众号中的接口地址：http://公网可访问域名/wx/portal/xxxxx （注意，xxxxx为对应公众号的appid值）；
5. 根据自己需要修改各个handler的实现，加入自己的业务逻辑。
	
