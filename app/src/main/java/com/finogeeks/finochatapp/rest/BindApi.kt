package com.finogeeks.finochatapp.rest

import com.finogeeks.finochat.rest.api
import com.finogeeks.finochatapp.rest.model.BindParam
import com.finogeeks.finochatapp.rest.model.BindResult
import io.reactivex.Observable
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

val bindApi get () = api<BindApi>()

interface BindApi {
    /**
     * 绑定或解除绑定
     *
     * @param jwt authorization from HomeServerConfig.Credential.
     */
    @POST("finchat/contact/manager/staff/keycloak")
    fun bind(@Body param: BindParam, @Query("jwt") jwt: String): Observable<BindResult>

    /**
     * 解除绑定或解除绑定
     *
     * @param jwt authorization from HomeServerConfig.Credential.
     */
    @POST("finchat/contact/manager/staff/keycloak/unbind")
    fun unbind(@Query("jwt") jwt: String): Observable<BindResult>
}