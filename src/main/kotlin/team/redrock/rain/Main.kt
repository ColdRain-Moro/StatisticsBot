package team.redrock.rain

import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.*
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.auth.BotAuthorization
import net.mamoe.mirai.utils.BotConfiguration
import team.redrock.rain.database.DatabaseDriver
import team.redrock.rain.module.chat.setupChatStatistic
import team.redrock.rain.module.member.setUpMemberStatistic
import team.redrock.rain.module.notify.setupNotify
import xyz.cssxsh.mirai.tool.FixProtocolVersion
import xyz.cssxsh.mirai.tool.KFCFactory
import xyz.cssxsh.mirai.tool.TLV544Provider
import xyz.cssxsh.mirai.tool.ViVo50
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
lateinit var databaseUrl: String
lateinit var databaseUser: String
lateinit var databasePassword: String

val bot by lazy {
    BotFactory.newBot(qqNumber, qqPassword) {
        protocol = BotConfiguration.MiraiProtocol.ANDROID_PAD
        fileBasedDeviceInfo("device.json")
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
    databaseUrl = env["DATABASE_URL"]!!
    databaseUser = env["DATABASE_USER"]!!
    databasePassword = env["DATABASE_PASSWORD"]!!
}

fun main(): Unit = runBlocking {
    configureEnv()
    DatabaseDriver.init(databaseUrl, databaseUser, databasePassword)
    FixProtocolVersion.update()
    KFCFactory.install()

    println(FixProtocolVersion.info())
    bot.login()
    val timeZone = TimeZone.getTimeZone("Asia/Shanghai")
    TimeZone.setDefault(timeZone)
    setupNotify(bot)
    bot.setupChatStatistic()
    bot.setUpMemberStatistic()
}