package team.redrock.rain.database

import com.alibaba.excel.EasyExcel
import com.alibaba.excel.context.AnalysisContext
import com.alibaba.excel.read.listener.ReadListener
import io.github.cdimascio.dotenv.Dotenv
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import team.redrock.rain.module.member.StudentEntity
import team.redrock.rain.module.member.StudentInfo
import team.redrock.rain.module.member.TableStudents
import java.io.File
import java.io.InputStream
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * StatisticsBot
 * team.redrock.rain.database
 *
 * @author 寒雨
 * @since 2023/3/21 下午6:51
 */
object DatabaseDriver {
    fun init(url: String, user: String, password: String) {
        Database.connect(
            url,
            "com.mysql.cj.jdbc.Driver",
            user,
            password
        )
        transaction {
            addLogger(StdOutSqlLogger)
        }
    }
}

// 建表并上传本地Excel表中的数据
private suspend fun main() {
    val env = Dotenv.configure().ignoreIfMissing().load()
    DatabaseDriver.init(env["DATABASE_URL"], env["DATABASE_USER"], env["DATABASE_PASSWORD"])
    // 建表
    newSuspendedTransaction {
        SchemaUtils.create(TableStudents)
    }
    readExcel(File("./info.xlsx").inputStream()).forEach { info ->
        newSuspendedTransaction {
            StudentEntity.new {
                stuId = info.stuId!!
                name = info.name!!
                gender = info.gender!!
                department = info.department!!
                major = info.major!!
                grade = info.grade!!
                classId = info.classId!!
                qqNumber = info.qq!!
            }
        }
    }
}

private suspend fun readExcel(inputStream: InputStream) = suspendCoroutine<List<StudentInfo>> {
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