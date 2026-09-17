package expo.modules.tailscalecheck

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.net.NetworkCapabilities
import java.net.Inet4Address
import expo.modules.kotlin.records.Field
import expo.modules.kotlin.records.Record

enum class TailscaleState {
  DISCONNECTED,
  SPLIT_TUNNEL,
  EXIT_NODE_ACTIVE
}

data class TailscaleInterfaceInfo(@Field val name: String, @Field val ip: String) : Record

fun getTailscaleStateValue(state: TailscaleState?): String {
  return when (state) {
    TailscaleState.DISCONNECTED -> "NONE"
    TailscaleState.SPLIT_TUNNEL -> "SPLIT_TUNNEL"
    TailscaleState.EXIT_NODE_ACTIVE -> "EXIT_NODE"
    null -> "UNKNOWN"
  }
}

class TailscaleDetector(
  context: Context,
  private val onStateChanged: (TailscaleState) -> Unit,
) {
  private val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
  private var isListening = false
  private var lastReportedState: TailscaleState? = null

  private fun handleNetworkUpdate(network: Network) {
    val newState = evaluateNetwork(network)
    if (newState != lastReportedState) {
      lastReportedState = newState
      onStateChanged(newState)
    }
  }

  private val callback = object : ConnectivityManager.NetworkCallback() {
    override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
      super.onCapabilitiesChanged(network, networkCapabilities)
      handleNetworkUpdate(network)
    }

    override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
      super.onLinkPropertiesChanged(network, linkProperties)
      handleNetworkUpdate(network)
    }

    override fun onLost(network: Network) {
      super.onLost(network)
      if (lastReportedState != TailscaleState.DISCONNECTED) {
        lastReportedState = TailscaleState.DISCONNECTED
        onStateChanged(TailscaleState.DISCONNECTED)
      }
    }
  }

  @Synchronized
  fun start() {
    if (isListening) return
    try {
      cm.registerDefaultNetworkCallback(callback)
      isListening = true
    } catch (_: Exception) {}
  }

  @Synchronized
  fun stop() {
    if (!isListening) return
    try {
      cm.unregisterNetworkCallback(callback)
    } catch (_: Exception) {} finally {
      isListening = false
    }
  }

  fun getCurrentState(): TailscaleState {
    val activeNetwork = cm.activeNetwork ?: return TailscaleState.DISCONNECTED
    return evaluateNetwork(activeNetwork)
  }

  private fun evaluateNetwork(network: Network): TailscaleState {
    val caps = cm.getNetworkCapabilities(network) ?: return TailscaleState.DISCONNECTED
    val lp = cm.getLinkProperties(network) ?: return TailscaleState.DISCONNECTED

    if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) return TailscaleState.DISCONNECTED

    val hasTailscaleDNS = lp.dnsServers.any {
      it?.hostAddress == "100.100.100.100" || it?.hostAddress == "fd7a:115c:a1e0::53"
    }
    val hasTailnetDomain = lp.domains?.contains(".ts.net") == true
    val hasTailscaleIP = lp.linkAddresses.any { linkAddress ->
      val address = linkAddress.address
      if (address is Inet4Address) {
        val bytes = address.address
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF
        b0 == 100 && b1 in 64..127
      } else false
    }

    val isTailscale = hasTailscaleIP || hasTailnetDomain || hasTailscaleDNS
    if (!isTailscale) return TailscaleState.DISCONNECTED

    val hasDefaultV4Route = lp.routes.any { routeInfo ->
      routeInfo.isDefaultRoute && routeInfo.destination?.address is Inet4Address
    }

    if (hasDefaultV4Route) {
      return TailscaleState.EXIT_NODE_ACTIVE
    } else {
      return TailscaleState.SPLIT_TUNNEL
    }
  }

  fun tailscaleInterface(): TailscaleInterfaceInfo? {
    val activeNetwork = cm.activeNetwork ?: return null
    val lp = cm.getLinkProperties(activeNetwork) ?: return null

    val interfaceName = lp.interfaceName ?: return null

    for (linkAddress in lp.linkAddresses) {
      val address = linkAddress.address
      if (address is Inet4Address) {
        val bytes = address.address
        val b0 = bytes[0].toInt() and 0xFF
        val b1 = bytes[1].toInt() and 0xFF

        if (b0 == 100 && b1 in 64..127) {
          return TailscaleInterfaceInfo(
            name = interfaceName,
            ip = address.hostAddress ?: ""
          )
        }
      }
    }

    return null
  }
}