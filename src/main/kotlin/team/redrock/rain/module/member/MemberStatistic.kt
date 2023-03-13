package team.redrock.rain.module.member

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.annotation.ExcelProperty
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.getMember
import net.mamoe.mirai.event.events.UserMessageEvent
import okhttp3.*
import org.apache.commons.mail.DefaultAuthenticator
import org.apache.commons.mail.EmailAttachment
import org.apache.commons.mail.MultiPartEmail
import team.redrock.rain.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * StatisticsBot
 * team.redrock.rain.module
 *
 * @author 寒雨
 * @since 2023/3/7 下午1:13
 */
data class StudentInfo(
    @ExcelProperty("姓名")
    val name: String,
    @ExcelProperty("学号")
    val stuId: Long,
    @ExcelProperty("QQ号")
    val qq: Long,
)

data class StudentInfoOutput(
    @ExcelProperty("群名")
    val group: String,
    @ExcelProperty("群号")
    val groupId: Long,
    @ExcelProperty("姓名")
    val name: String,
    @ExcelProperty("学号")
    val stuId: Long,
    @ExcelProperty("QQ号")
    val qq: Long
)

private const val CONFIG_PATH = "https://persecution-1301196908.cos.ap-chongqing.myqcloud.com/member_statistic/member-statistic-config.json"

private lateinit var map: Map<Long, String>
private val okHttpClient = OkHttpClient.Builder().build()
private val gson = Gson()
private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd")

suspend fun Bot.setUpMemberStatistic() {
    // 名单excel文件和对应的群号json我放 cos 上
    // 从网络上拉取配置文件
    map = fetchConfig()
    launchWhenCommand("导出未入群人员名单") {
        val data = exportMemberList()
        val pwd = Paths.get("").toAbsolutePath().toString()

        val file = File(pwd, "未入群人员名单 - ${simpleDateFormat.format(Date())}.xlsx")
        withContext(Dispatchers.IO) {
            file.createNewFile()
            EasyExcel.write(file, StudentInfoOutput::class.java)
                .sheet(1)
                .doWrite(data)
        }
        sendEmail(file)
        println("未入群人员名单已发送")
        // 发完就删
        file.delete()
    }
    launchWhenCommand("修改群名片") {
        editNameCard()
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
        subject = "未入群人员名单"
        addTo(emailSubscriber)
        attach(attachment)
    }.send()
}

suspend fun Bot.editNameCard() {
    map.mapNotNull { (k, v) -> (getGroup(k) ?: return@mapNotNull null) to v }
        .associate { (k, v) -> k to readExcel(fetchFileStream(v)) }
        .flatMap { (group, v) -> v.mapNotNull { stu -> stu to (group.getMember(stu.stuId) ?: return@mapNotNull null) } }
        .forEach { (stu, member) ->
            if (member.nameCard != "${stu.stuId}-${stu.name}") {
                // 两秒改一个, 改太快会触发风控
                delay(2000)
                kotlin.runCatching {
                    member.nameCard = "${stu.stuId}-${stu.name}"
                }.onFailure {
                    println("没有管理员权限，无法修改成员群名片")
                }
            }
        }
    println("群名片修改完成!")
}

suspend fun Bot.exportMemberList(): List<StudentInfoOutput> {
    return map.mapNotNull { (k, v) -> (getGroup(k) ?: return@mapNotNull null) to v }
        .associate { (k, v) -> k to readExcel(fetchFileStream(v)) }
        .mapValues { (group, v) -> v.filterNot { stu -> group.contains(stu.stuId) } }
        .flatMap { (group, v) -> v.map { stu -> StudentInfoOutput(group.name, group.id, stu.name, stu.stuId, stu.qq) } }
}

suspend fun readExcel(inputStream: InputStream) = suspendCoroutine<List<StudentInfo>> {
    EasyExcel.read(inputStream, StudentInfo::class.java, object : ReadListener<StudentInfo> {

        private val cache = mutableListOf<StudentInfo>()

        override fun invoke(data: StudentInfo?, context: AnalysisContext?) {
            data?.let { cache.add(data) }
        }

        override fun doAfterAllAnalysed(context: AnalysisContext?) {
            it.resume(cache)
        }
    }).doReadAll()
}

suspend fun Bot.launchWhenCommand(cmd: String, schedule: suspend () -> Unit) {
    eventChannel.subscribeAlways<UserMessageEvent> {
        if (message.contentToString() == cmd) {
            schedule()
        }
    }
}

suspend fun fetchConfig() = suspendCoroutine {
    okHttpClient.newCall(
        Request.Builder()
            .url(CONFIG_PATH)
            .build()
    ).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            it.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            it.resume(
                response.body!!.string()
                    .let { s -> gson.fromJson<Map<String, String>?>(s, object : TypeToken<Map<String, String>>() {}.type).mapKeys { (k, v) -> k.toLong() } }
            )
        }
    })
}

suspend fun fetchFileStream(url: String) = suspendCoroutine {
    okHttpClient.newCall(
        Request.Builder()
            .url(url)
            .build()
    ).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            it.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            it.resume(response.body!!.byteStream())
        }
    })
}