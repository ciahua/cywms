package com.chenyang.cywms.data.model

import com.google.gson.annotations.SerializedName

/** 生产领料主单 shengchanlingliao */
data class Shengchanlingliao(
    val id: String? = null,
    val lingliaodanhao: String? = null,
    val lingliaobumen: String? = null,
    val shengchanzhiling: String? = null,
    /** 0=未完成 1=已完成（以后端为准） */
    val status: String? = null,
    val creator: String? = null,
    val createdt: String? = null,
    val issuer: String? = null,
    val issuedt: String? = null,
    val receiver: String? = null,
    val receivedt: String? = null,
    val remark: String? = null
)

/** 生产领料明细 shengchanlingliao_detail */
data class ShengchanlingliaoDetail(
    val id: String? = null,
    val matcode: String? = null,
    val matname: String? = null,
    val matspec: String? = null,
    val unit: String? = null,
    val reqqty: String? = null,
    val actqty: String? = null,
    val status: String? = null,
    val warehouse: String? = null,
    val srcorderno: String? = null,
    val manuorderno: String? = null,
    val fkShengchanlingliao: String? = null,
    val auxprop: String? = null,
    val remark: String? = null
)

/**
 * 领料扫码提交体 —— 字段名必须与后端一致（含 fk_outsourcedetail）。
 */
data class ProduceMaterialBarcodePda(
    @SerializedName("fk_outsourcedetail")
    val fkOutsourcedetail: String,
    val deliveryno: String,
    val scanner: String,
    val needqty: String,
    val barcodelist: List<String>,
    val scanqtyList: List<String>,
    val restqtyList: List<String>
)

/** 本地已扫条码行 */
data class ScannedBarcodeLine(
    val barcode: String,
    val scanQty: String,
    val restQty: String,
    val period: String,
    val matcodeFromBarcode: String
)

object BarcodeParse {
    /**
     * 物料条码：`matcode*qty**date*lot*…`
     * qty 取第 2 段；FIFO period 优先取日期段（`**` 后第一段）。
     */
    fun parse(raw: String): ParsedBarcode? {
        val code = raw.trim()
        if (code.isEmpty()) return null
        val parts = code.split('*')
        if (parts.isEmpty() || parts[0].isBlank()) return null
        val matcode = parts[0].trim()
        val qty = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "1" }
        // a*b**date → ["a","b","","date",...]
        val period = when {
            parts.size >= 4 && parts[2].isBlank() -> parts[3].trim()
            parts.size >= 3 && parts[2].isNotBlank() -> parts[2].trim()
            else -> ""
        }
        return ParsedBarcode(
            raw = code,
            matcode = matcode,
            qty = qty,
            period = period
        )
    }

    data class ParsedBarcode(
        val raw: String,
        val matcode: String,
        val qty: String,
        val period: String
    )
}

fun String?.asQty(): Double =
    this?.trim()?.toDoubleOrNull() ?: 0.0

fun formatQty(value: Double): String {
    return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
}

/** Jeecg 分页 */
data class JeecgPage<T>(
    val records: List<T> = emptyList(),
    val total: Long = 0,
    val size: Long = 0,
    val current: Long = 0
)

/** 库存台账简要字段（余量回退查询用） */
data class WarehouseLogRow(
    val id: String? = null,
    val barcode: String? = null,
    val matcode: String? = null,
    val restqty: String? = null,
    val sku: String? = null
)
