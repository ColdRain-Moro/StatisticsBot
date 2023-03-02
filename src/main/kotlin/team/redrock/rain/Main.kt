package team.redrock.rain

import com.alibaba.excel.EasyExcel
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.events.GroupMessageEvent
import net.mamoe.mirai.utils.BotConfiguration
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailAttachment
import org.apache.commons.mail.MultiPartEmail
import team.redrock.rain.bean.MemberInfo
import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
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

val statistics = hashMapOf<Long, Int>()

var lastDay = -1

val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

fun configureEnv() {
    val env = Dotenv.load()
    qqNumber = env["QQ_NUMBER"]!!.toLong()
    qqPassword = env["QQ_PASSWORD"]!!
    targetGroup = env["TARGET_GROUP"]!!.toLong()
    smtpServer = env["SMTP_SERVER"] ?: "smtp.qq.com"
    smtpPort1 = env["SMTP_PORT"]?.toIntOrNull() ?: 465
    email = env["EMAIL"]!!
    emailPassword = env["EMAIL_PASSWORD"]!!
    emailSubscriber = env["EMAIL_SUBSCRIBER"]!!
}

suspend fun export(): File {
    // 制成excel表 并发邮件
    val data = statistics.mapNotNull { (id, count) ->
        bot.getGroup(targetGroup)?.getMember(id)?.let {
            MemberInfo(it.nick, it.id, count)
        }
    }
    val pwd = Paths.get("").toAbsolutePath().toString()
    val file = File(pwd, "群聊发言统计 - ${simpleDateFormat.format(Date())}.xlsx")
    withContext(Dispatchers.IO) {
        file.createNewFile()
        EasyExcel.write(file, MemberInfo::class.java)
            .sheet(1)
            .doWrite(data)
    }
    return file
}

fun main(): Unit = runBlocking {
    configureEnv()
    bot.login()
    val lock = Mutex()
    bot.eventChannel.subscribeAlways<GroupMessageEvent> {
        if (group.id == targetGroup) {
            // 上个锁免得产生竞态
            lock.lock()
            statistics[sender.id] = (statistics[sender.id] ?: 0) + 1
            lock.unlock()
            if (message.contentToString() == "#export#") {
                sendEmail(export())
            }
        }
    }
    launchWhen(0) {
        val file = export()

        sendEmail(file)

        file.delete()

        // 清除前一天的记录
        statistics.clear()
    }
}

fun sendEmail(file: File) {
    val multipart = MultiPartEmail()
    val attachment = EmailAttachment()
    attachment.apply {
        path = file.path
        name = file.name
        disposition = EmailAttachment.ATTACHMENT
    }
    multipart.apply {
        hostName = smtpServer
        setSmtpPort(smtpPort1)
        setCharset("UTF-8")
        setAuthenticator(DefaultAuthenticator(email, emailPassword))
        isSSLOnConnect = true
        setFrom(email)
        subject = "今日群聊发言统计"
        addTo(emailSubscriber)
        attach(attachment)
    }.send()
}
// 整点唤醒
suspend fun launchWhen(hour: Int, func: suspend CoroutineScope.() -> Unit) = coroutineScope {
    while (true) {
        val date = Date()
        if (date.hours == hour && date.date != lastDay) {
            func()
            lastDay = date.date
        }
        delay(TimeUnit.MINUTES.toMillis(1))
    }
}