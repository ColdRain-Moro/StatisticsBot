# StatisticsBot

> 统计群成员每天发言次数，并在次日0点整合为excel表发送给订阅者邮箱

## build

> $ ./gradlew assemble

## 环境变量

> 可以在当前路径创建 .env 文件，也可自行设置

~~~.dotenv
# QQ号
QQ_NUMBER="35xxxx0965"
# 密码
QQ_PASSWORD="xxxx"
# QQ群
TARGET_GROUP="46xxxx707"
# SMTP服务器
SMTP_SERVER="smtp.qq.com"
# SMTP端口
SMTP_PORT="465"
# 邮箱帐号
EMAIL="13xxxxxx19@qq.com"
# 邮箱密码
EMAIL_PASSWORD="xxxxxx"
# 订阅者邮箱
EMAIL_SUBSCRIBER="rain@redrock.team"
~~~

## device.json

使用 `ANDROID_PHONE` 协议。使用与jar文件同级目录的 `device.json`, 如找不到则会使用随机设备。

为了避免验证和风控，建议自行提供 `device.json`。可使用 [Aoki](https://github.com/MrXiaoM/Aoki) 进行获取。