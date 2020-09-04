package com.finogeeks.finochatapp.modules.server.model

import com.google.gson.annotations.SerializedName

class ServerData(@SerializedName("data") var data: ArrayList<ServerConfig>?)