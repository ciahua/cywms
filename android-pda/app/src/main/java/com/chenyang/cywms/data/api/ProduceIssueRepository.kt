package com.chenyang.cywms.data.api

import com.chenyang.cywms.data.model.ApiResult
import com.chenyang.cywms.data.model.ProduceMaterialBarcodePda
import com.chenyang.cywms.data.model.Shengchanlingliao
import com.chenyang.cywms.data.model.ShengchanlingliaoDetail
import com.chenyang.cywms.data.model.asQty
import com.chenyang.cywms.data.model.formatQty
import com.chenyang.cywms.data.prefs.SessionPrefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ProduceIssueRepository(
    private val apiClient: ApiClient,
    private val prefs: SessionPrefs
) {
    private suspend fun api(): ProduceIssueApi = apiClient.produceIssueApi()

    private fun <T> ApiResult<T>.requireSuccess(defaultMsg: String): T {
        if (!success) error(message?.ifBlank { null } ?: defaultMsg)
        return result ?: error(message?.ifBlank { null } ?: defaultMsg)
    }

    private fun <T> ApiResult<T>.requireOk(defaultMsg: String) {
        if (!success) error(message?.ifBlank { null } ?: defaultMsg)
    }

    suspend fun listOrders(search: String?): Result<List<Shengchanlingliao>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user = prefs.current().username
                val resp = if (search.isNullOrBlank()) {
                    api().list2(user)
                } else {
                    api().list3(user, search.trim())
                }
                resp.requireSuccess("加载领料单失败")
            }
        }

    /**
     * 优先 MainId1（按用户）；若无明细则回退 MainId，避免管理员账号看不到行。
     */
    suspend fun listDetails(mainId: String): Result<List<ShengchanlingliaoDetail>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user = prefs.current().username
                val filtered = api().detailByMainId1(mainId, user)
                filtered.requireOk("加载明细失败")
                val rows = filtered.result.orEmpty()
                if (rows.isNotEmpty()) rows
                else api().detailByMainId(mainId).requireSuccess("加载明细失败")
            }
        }

    suspend fun getScanQty(detailId: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api().getScanQty(detailId)
            resp.requireOk("查询已扫数量失败")
            resp.result?.takeIf { it.isNotBlank() }
                ?: resp.message?.takeIf { it.isNotBlank() }
                ?: "0"
        }
    }

    suspend fun verify(barcode: String, detailId: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api().verify(barcode, detailId)
                resp.requireOk("条码校验失败")
                resp.result == true
            }
        }

    /**
     * 领料余量：优先 produce getrestqty。
     * 若为 0，回退台账 list（同条码多行时取最大正余量，避免 500+0 被接口合成 0）。
     */
    suspend fun getRestQty(barcode: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val resp = api().getRestQty(barcode)
            if (resp.success) {
                val direct = resp.result?.takeIf { it.isNotBlank() }
                    ?: resp.message?.takeIf { it.isNotBlank() }
                    ?: "0"
                if (direct.asQty() > 0) return@runCatching direct
            }
            val page = api().warehouseListByBarcode(barcode)
            if (!page.success) {
                if (resp.success) return@runCatching "0"
                error(page.message?.ifBlank { null } ?: "查询余量失败")
            }
            val maxRest = page.result?.records
                ?.map { it.restqty.asQty() }
                ?.filter { it > 0 }
                ?.maxOrNull()
                ?: 0.0
            formatQty(maxRest)
        }
    }

    suspend fun checkFifo(
        barcode: String,
        matcode: String,
        period: String,
        requestQty: String
    ): Result<Boolean> = withContext(Dispatchers.IO) {
        runCatching {
            val p = period.ifBlank { "0" }
            // checkinfifo1 在部分条码上会误返 false；以 checkinfifo 为准，1 仅作增强
            val basic = api().checkInFifo(barcode, matcode, p)
            if (!basic.success) return@runCatching true
            if (basic.result == false) return@runCatching false
            if (requestQty.isNotBlank()) {
                val withQty = api().checkInFifo1(barcode, matcode, p, requestQty)
                // 1 失败或 false 时不覆盖已通过的 basic
                if (withQty.success && withQty.result == true) return@runCatching true
            }
            true
        }
    }

    suspend fun submitPda(body: ProduceMaterialBarcodePda): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api().addPda(body)
                resp.requireOk("提交失败")
                resp.result ?: resp.message ?: "OK"
            }
        }

    suspend fun changeLineStatus(detailId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val user = prefs.current().username
                val resp = api().changeStatus(detailId, user)
                resp.requireOk("更新明细状态失败")
                resp.result ?: resp.message ?: "OK"
            }
        }

    suspend fun changeMainStatus(mainId: String): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api().changeMainStatus(mainId)
                resp.requireOk("更新主单状态失败")
                resp.result ?: resp.message ?: "OK"
            }
        }

    suspend fun currentUsername(): String = prefs.current().username
}
