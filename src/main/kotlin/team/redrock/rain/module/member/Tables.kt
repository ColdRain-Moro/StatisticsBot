package team.redrock.rain.module.member

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable

/**
 * StatisticsBot
 * team.redrock.rain.module.member
 *
 * @author 寒雨
 * @since 2023/3/21 下午8:11
 */
object TableStudents : IntIdTable("students_table") {
    val stuId = long("stu_id")
    val name = varchar("name", 16)
    val gender = varchar("gender", 1)
    val department = varchar("department", 16)
    val major = varchar("major", 32)
    val grade = integer("grade")
    val classId = integer("class_id")
    val qqNumber = long("qq_number")
}

class StudentEntity(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<StudentEntity>(TableStudents)

    var stuId by TableStudents.stuId
    var name by TableStudents.name
    var gender by TableStudents.gender
    var department by TableStudents.department
    var major by TableStudents.major
    var grade by TableStudents.grade
    var classId by TableStudents.classId
    var qqNumber by TableStudents.qqNumber
}