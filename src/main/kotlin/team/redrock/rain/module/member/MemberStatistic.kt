package team.redrock.rain.module.member

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.annotation.ExcelProperty
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
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import team.redrock.rain.*
import java.io.File
import java.io.IOException
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
class StudentInfo {
    @ExcelProperty("学号")
    var stuId: Long? = null
    @ExcelProperty("姓名")
    var name: String? = null
    @ExcelProperty("性别")
    var gender: String? = null
    @ExcelProperty("学院")
    var department: String? = null
    @ExcelProperty("专业")
    var major: String? = null
    @ExcelProperty("年级")
    var grade: Int? = null
    @ExcelProperty("行政班班号")
    var classId: Int? = null
    @ExcelProperty("QQ号")
    var qq: Long? = null

    override fun toString(): String {
        return "$name $stuId $qq"
    }

    companion object {
        fun fromEntity(entity: StudentEntity): StudentInfo {
            return StudentInfo().apply {
                stuId = entity.stuId
                name = entity.name
                gender = entity.gender
                department = entity.department
                major = entity.major
                grade = entity.grade
                classId = entity.classId
                qq = entity.qqNumber
            }
        }
    }
}

data class StudentInfoOutput(
    @ExcelProperty("群名")
    val group: String,
    @ExcelProperty("学院")
    val depart: String,
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

private lateinit var map: Map<String, Long>
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
        println(data)
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
    map.mapNotNull { (k, v) -> k to (getGroup(v) ?: return@mapNotNull null) }
        .onEach { println(it) }
        .associate { (k, v) -> v to selectDepartmentMembers(k) }
        .onEach { println(it) }
        .flatMap { (group, v) -> v.mapNotNull { stu -> stu to (group.getMember(stu.qq!!) ?: return@mapNotNull null) } }
        .onEach { println(it) }
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
    return map.mapNotNull { (k, v) -> (getGroup(v) ?: return@mapNotNull null) to k }
        .associate { (k, v) -> k to selectDepartmentMembers(v) }
        .mapValues { (group, v) -> v.filterNot { stu -> group.contains(stu.qq!!) } }
        .flatMap { (group, v) -> v.map { stu -> StudentInfoOutput(group.name, stu.department!!, group.id, stu.name!!, stu.stuId!!, stu.qq!!) } }
}

suspend fun selectDepartmentMembers(depart: String): List<StudentInfo> {
    return newSuspendedTransaction {
        StudentEntity.find { TableStudents.department eq depart }
            .map { StudentInfo.fromEntity(it) }
    }
}

suspend fun Bot.launchWhenCommand(cmd: String, schedule: suspend () -> Unit) {
    eventChannel.subscribeAlways<UserMessageEvent> {
        if (message.contentToString() == cmd) {
            schedule()
        }
    }
}

suspend fun fetchConfig(): Map<String, Long> = suspendCoroutine {
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
                    .let { s -> gson.fromJson(s, object : TypeToken<Map<String, Long>>() {}.type) }
            )
        }
    })
}