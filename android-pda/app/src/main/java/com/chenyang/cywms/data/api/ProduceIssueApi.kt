package com.chenyang.cywms.data.api

import com.chenyang.cywms.data.model.ApiResult
import com.chenyang.cywms.data.model.ProduceMaterialBarcodePda
import com.chenyang.cywms.data.model.Shengchanlingliao
import com.chenyang.cywms.data.model.ShengchanlingliaoDetail
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface ProduceIssueApi {
    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/list2")
    suspend fun list2(
        @Query("userName") userName: String
    ): ApiResult<List<Shengchanlingliao>>

    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/list3")
    suspend fun list3(
        @Query("userName") userName: String,
        @Query("searchValue") searchValue: String?
    ): ApiResult<List<Shengchanlingliao>>

    /** PDA 用户过滤明细 */
    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/queryShengchanlingliaoDetailByMainId1")
    suspend fun detailByMainId1(
        @Query("id") id: String,
        @Query("userName") userName: String
    ): ApiResult<List<ShengchanlingliaoDetail>>

    /** 无用户过滤（MainId1 为空时回退） */
    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/queryShengchanlingliaoDetailByMainId")
    suspend fun detailByMainId(
        @Query("id") id: String
    ): ApiResult<List<ShengchanlingliaoDetail>>

    @GET("jeecg-boot/produce_material_barcode/produceMaterialBarcode/verify")
    suspend fun verify(
        @Query("barcode") barcode: String,
        @Query("id") detailId: String
    ): ApiResult<Boolean>

    @GET("jeecg-boot/produce_material_barcode/produceMaterialBarcode/getrestqty")
    suspend fun getRestQty(
        @Query("barcode") barcode: String
    ): ApiResult<String>

    @GET("jeecg-boot/produce_material_barcode/produceMaterialBarcode/getscanqty")
    suspend fun getScanQty(
        @Query("id") detailId: String
    ): ApiResult<String>

    @POST("jeecg-boot/produce_material_barcode/produceMaterialBarcode/addpda")
    suspend fun addPda(
        @Body body: ProduceMaterialBarcodePda
    ): ApiResult<String>

    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/changestatus")
    suspend fun changeStatus(
        @Query("id") detailId: String,
        @Query("username") username: String
    ): ApiResult<String>

    @GET("jeecg-boot/shengchanlingliao/shengchanlingliao/changemainstatus")
    suspend fun changeMainStatus(
        @Query("id") mainId: String
    ): ApiResult<String>

    @GET("jeecg-boot/warehouselog/warehouselog/checkinfifo")
    suspend fun checkInFifo(
        @Query("barcode") barcode: String,
        @Query("matcode") matcode: String,
        @Query("period") period: String
    ): ApiResult<Boolean>

    @GET("jeecg-boot/warehouselog/warehouselog/checkinfifo1")
    suspend fun checkInFifo1(
        @Query("barcode") barcode: String,
        @Query("matcode") matcode: String,
        @Query("period") period: String,
        @Query("requestqty") requestQty: String
    ): ApiResult<Boolean>
}
