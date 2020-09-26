package com.finogeeks.finochatapp.rest.model

import com.google.gson.annotations.SerializedName

class BindResult(@SerializedName("id") val id: String?,
                 @SerializedName("errcode") val errCode: String?,
                 @SerializedName("error") val error: String?,
                 @SerializedName("service") val service: String?)