package com.example.livebroadcast.utility

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

object IpAddressTool {

    /**
     * 在 Flow 中接收 IP 地址
     *
     * @param context Context
     * @return IPv4 中的 IP 地址
     * */

    fun collectIpAddress(context: Context) = callbackFlow {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE)
            ?.let { it as ConnectivityManager }

        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)
                val linkProperties = connectivityManager?.getLinkProperties(network)
                // 查找 IPv4地址
                val address = linkProperties?.linkAddresses
                    // 我们学校校园网的默认 ip分配以 10 开头，考虑是否根据实际使用添加自定义设置页
                    ?.find { it.address?.hostAddress?.toString()?.startsWith("10") == true }
                    ?.address?.hostAddress
                if (address != null) {
                    trySend(address)
                }
            }
        }
        connectivityManager?.registerNetworkCallback(request, networkCallback)

        // awaitClose块确保当Flow关闭时，会执行解除注册操作
        // 即调用connectivityManager.unregisterNetworkCallback来移除之前注册的网络回调，避免内存泄漏。
        awaitClose {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        }
    }
}