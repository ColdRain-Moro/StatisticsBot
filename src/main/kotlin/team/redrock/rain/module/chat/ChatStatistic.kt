package team.redrock.rain.module.chat

import com.alibaba.excel.EasyExcel
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.events.GroupMessageEvent
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailAttachment
import org.apache.commons.mail.MultiPartEmail
import team.redrock.rain.*
import team.redrock.rain.module.chat.bean.MemberInfo
import java.io.File
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * StatisticsBot
 * team.redrock.rain.module
 *
 * @author 寒雨
 * @since 2023/3/7 下午1:05
 */

private val statistics = hashMapOf<Long, Int>()

private var lastDay = -1

private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

suspend fun Bot.setupChatStatistic() {
    val lock = Mutex()
    bot.eventChannel.subscribeAlways<GroupMessageEvent> {
        if (group.id == targetGroup) {
            // 上个锁免得产生竞态
            lock.withLock {
                statistics[sender.id] = (statistics[sender.id] ?: 0) + 1
            }
        }
    }

    launchWhen(0) {
        val file = export()

        println(statistics)

        sendEmail(file)

        file.delete()

        // 清除前一天的记录
        statistics.clear()
    }
}

private fun sendEmail(file: File) {
    val multipart = MultiPartEmail()
    val attachment = EmailAttachment()
    attachment.apply {
        path = file.path
        name = String(file.name.toByteArray(), Charsets.UTF_8)
        disposition = EmailAttachment.ATTACHMENT
    }
    multipart.apply {
        hostName = smtpServer
        setSmtpPort(smtpPort1)
        setCharset("utf-8")
        setAuthenticator(DefaultAuthenticator(email, emailPassword))
        isSSLOnConnect = true
        setFrom(email)
        subject = "今日群聊发言统计"
        addTo(emailSubscriber)
        attach(attachment)
    }.send()
}

// 整点唤醒
private suspend fun launchWhen(hour: Int, func: suspend CoroutineScope.() -> Unit) = coroutineScope {
    while (true) {
        val calendar = Calendar.getInstance()
        val date = calendar.time
        if (date.hours == hour && date.date != lastDay) {
            func()
            lastDay = date.date
        }
        delay(TimeUnit.MINUTES.toMillis(1))
    }
}

private fun getDateString(today: Boolean): String {
    return simpleDateFormat.format(Date().apply {
        if (!today) {
            date -= 1
        }
    })
}

private suspend fun export(today: Boolean = false): File {
    // 制成excel表 并发邮件
    val data = statistics.mapNotNull { (id, count) ->
        bot.getGroup(targetGroup)?.getMember(id)?.let {
            MemberInfo(it.nameCard, it.id, count)
        }
    }
    val pwd = Paths.get("").toAbsolutePath().toString()

    val file = File(pwd, "群聊发言统计 - ${simpleDateFormat.format(Date())}.xlsx")
    withContext(Dispatchers.IO) {
        file.createNewFile()
        val dateString = getDateString(today) + " 群聊发言统计"
        EasyExcel.write(file, MemberInfo::class.java)
            .sheet(1)
            .head(listOf(listOf(dateString, "群名片"), listOf(dateString, "QQ号"), listOf(dateString, "发言次数")))
            .doWrite(data)
    }
    return file
}