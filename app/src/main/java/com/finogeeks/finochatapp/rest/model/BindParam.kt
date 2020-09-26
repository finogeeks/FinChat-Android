package com.finogeeks.finochatapp.rest.model

import com.google.gson.annotations.SerializedName

class BindParam(@SerializedName("type") val type: String,
                @SerializedName("token") val token: String)