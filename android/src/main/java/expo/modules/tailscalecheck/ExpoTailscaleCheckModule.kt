package expo.modules.tailscalecheck

import expo.modules.kotlin.modules.Module
import expo.modules.kotlin.modules.ModuleDefinition

class ExpoTailscaleCheckModule : Module() {
  private var detector: TailscaleDetector? = null

  override fun definition() = ModuleDefinition {
    Name("ExpoTailscaleCheck")

    OnCreate {
      val context = appContext.reactContext ?: return@OnCreate

      detector = TailscaleDetector(context) { state ->
        sendEvent(
          "onStateChange",
          mapOf(
            "state" to getTailscaleStateValue(state)
          )
        )
      }
      detector?.start()
    }

    OnDestroy {
      detector?.stop()
      detector = null
    }

    Events("onStateChange")

    Function("getState") {
      val state = detector?.getCurrentState()
      getTailscaleStateValue(state)
    }

    Function("tailscaleInterface") {
      val tsInterface = detector?.tailscaleInterface()
      tsInterface
    }

    Function("openTailscaleApp") { packageName: String? ->
      val context = appContext.reactContext

      if (context != null) openTailscaleApp(context, packageName)
    }

    Function("connectVPN") { packageName: String? ->
      val context = appContext.reactContext

      if (context != null) connectVPN(context, packageName)
    }

    Function("disconnectVPN") { packageName: String? ->
      val context = appContext.reactContext

      if (context != null) disconnectVPN(context, packageName)
    }
  }
}
