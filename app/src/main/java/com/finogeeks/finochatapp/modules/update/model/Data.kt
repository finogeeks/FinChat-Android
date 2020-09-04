package com.finogeeks.finochatapp.modules.update.model

import com.google.gson.annotations.SerializedName

class Data(@SerializedName("updateType") var updateType: String?,
           @SerializedName("url") var url: String?,
           @SerializedName("version") var version: String?,
           @SerializedName("remarks") var remarks: String?,
           @SerializedName("forceUpdate") var forceUpdate: Boolean?,
           @SerializedName("forceUpdateVersion") var forceUpdateVersion: String?)