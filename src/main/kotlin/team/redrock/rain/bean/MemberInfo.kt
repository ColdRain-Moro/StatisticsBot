package team.redrock.rain.bean

import com.alibaba.excel.annotation.ExcelProperty

/**
 * StatisticsBot
 * team.redrock.rain.bean
 *
 * @author 寒雨
 * @since 2023/3/2 上午11:12
 */
data class MemberInfo(
    @ExcelProperty("群名片")
    val nick: String,
    @ExcelProperty("QQ号")
    val id: Long,
    @ExcelProperty("发言次数")
    val count: Int
)
