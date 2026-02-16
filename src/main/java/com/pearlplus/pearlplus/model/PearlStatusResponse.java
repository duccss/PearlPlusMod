package com.pearlplus.pearlplus.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public record PearlStatusResponse(
    List<String> pearls,
    List<String> output,
    @SerializedName(value = "botName", alternate = {"bot_name", "bot", "botUsername", "bot_username"})
    String botName,
    @SerializedName(value = "serverIp", alternate = {"server_ip", "server", "minecraftServer", "minecraft_server"})
    String serverIp,
    @SerializedName(value = "botConnected", alternate = {"bot_connected", "connected"})
    Boolean botConnected
) {
}
