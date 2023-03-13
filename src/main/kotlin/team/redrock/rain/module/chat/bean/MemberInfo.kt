package team.redrock.rain.module.chat.bean

import com.alibaba.excel.annotation.ExcelProperty

/**
 * StatisticsBot
 * team.redrock.rain.bean
 *
 * @author 寒雨
 * @since 2023/3/2 上午11:12
 */
data class MemberInfo(
    @ExcelProperty("日期", "群名片")
    val nick: String,
    @ExcelProperty("日期", "QQ号")
    val id: Long,
    @ExcelProperty("日期", "发言次数")
    val count: Int
)


