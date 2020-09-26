package com.finogeeks.finochatapp.modules.update.model

import com.google.gson.annotations.SerializedName

class VersionResp(@SerializedName("data") var data: ArrayList<Data>?,
                  @SerializedName("error") var error: String?,
                  @SerializedName("errcode") var errcode: String?,
                  @SerializedName("service") var service: String?)