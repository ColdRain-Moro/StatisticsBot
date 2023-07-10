package team.redrock.rain.module.notify

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.nameCardOrNick
import net.mamoe.mirai.message.data.At
import net.mamoe.mirai.message.data.PlainText
import net.mamoe.mirai.message.data.buildMessageChain
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import java.util.*

/**
 * Created by rain on 2023/7/8
 **/

private const val group = 740171764L

suspend fun setupNotify(bot: Bot) {
    bot.launch {
        while (true) {
            // 等待到8点50
            delay(getDelay())
            bot.notify()
        }
    }
}

private suspend fun Bot.notify() {
    val now = Calendar.getInstance()
    val month = now.get(Calendar.MONTH) + 1
    val date = now.get(Calendar.DATE)

    val notify = newSuspendedTransaction {
        NotifyEntity.find { TableNotify.date eq "$month.$date" }
            .firstOrNull()
    } ?: return

    val group = bot.getGroup(group) ?: return
    val member = group.members
        .firstOrNull { it.nameCardOrNick.contains(notify.student) } ?: return

    group.sendMessage(buildMessageChain {
        +At(member)
        +PlainText("晚上9点钟记得报值班情况哦~")
    })
}

// 获取当前时间到8点50的间隔
private fun getDelay(): Long {
    val now = Calendar.getInstance()
    val noon = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 20)
        set(Calendar.MINUTE, 50)
        set(Calendar.SECOND, 0)
    }

    return if (now.after(noon)) {
        noon.add(Calendar.DATE, 1)
        noon.timeInMillis - now.timeInMillis
    } else {
        noon.timeInMillis - now.timeInMillis
    }
}

object TableNotify : IntIdTable("students_notify") {
    val date = varchar("date", 16)
    val student = varchar("student", 16)
}

class NotifyEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<NotifyEntity>(TableNotify)

    var date by TableNotify.date
    var student by TableNotify.student
}