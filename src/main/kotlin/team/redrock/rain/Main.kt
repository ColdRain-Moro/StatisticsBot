package team.redrock.rain

import com.alibaba.excel.EasyExcel
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.*
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.utils.BotConfiguration
import team.redrock.rain.module.chat.setupChatStatistic
import team.redrock.rain.module.member.setUpMemberStatistic
import java.io.File
import java.nio.file.Paths
import java.util.*
import kotlin.properties.Delegates

var qqNumber by Delegates.notNull<Long>()
lateinit var qqPassword: String
var targetGroup by Delegates.notNull<Long>()
lateinit var smtpServer: String
var smtpPort1 by Delegates.notNull<Int>()
lateinit var email: String
lateinit var emailPassword: String
lateinit var emailSubscriber: String

val bot by lazy {
    BotFactory.newBot(qqNumber, qqPassword) {
        protocol = BotConfiguration.MiraiProtocol.ANDROID_PHONE
        fileBasedDeviceInfo("./device.json")
    }
}

fun configureEnv() {
    val env = Dotenv.configure().ignoreIfMissing().load()
    qqNumber = env["QQ_NUMBER"]!!.toLong()
    qqPassword = env["QQ_PASSWORD"]!!
    targetGroup = env["TARGET_GROUP"]!!.toLong()
    smtpServer = env["SMTP_SERVER"] ?: "smtp.qq.com"
    smtpPort1 = env["SMTP_PORT"]?.toIntOrNull() ?: 465
    email = env["EMAIL"]!!
    emailPassword = env["EMAIL_PASSWORD"]!!
    emailSubscriber = env["EMAIL_SUBSCRIBER"]!!
}

fun main(): Unit = runBlocking {
    configureEnv()
    bot.login()
    val timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    TimeZone.setDefault(timeZone)
    bot.setupChatStatistic()
    bot.setUpMemberStatistic()
}