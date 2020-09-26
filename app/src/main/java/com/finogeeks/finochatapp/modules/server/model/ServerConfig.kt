package com.finogeeks.finochatapp.modules.server.model

import com.google.gson.annotations.SerializedName


open class ServerConfig(@SerializedName("server_name") var name: String,
                        @SerializedName("server_address") var url: String,
                        @Transient val viewType: Int = VIEW_TYPE_SERVER) {

    companion object {
        const val VIEW_TYPE_SERVER = 0
        const val VIEW_TYPE_LOCAL = 1
    }
}