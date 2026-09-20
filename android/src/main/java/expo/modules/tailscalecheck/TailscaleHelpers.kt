package expo.modules.tailscalecheck

import android.content.Context
import android.content.Intent
import androidx.annotation.StringDef

const val TAILSCALE_PACKAGE_NAME = "com.tailscale.ipn"

object ActionType {
  const val CONNECT = "CONNECT_VPN"
  const val DISCONNECT = "DISCONNECT_VPN"
}

@Retention(AnnotationRetention.SOURCE)
@StringDef(ActionType.CONNECT, ActionType.DISCONNECT)
annotation class Action


fun openTailscaleApp(ctx: Context, packageName: String?) {
  val pm = ctx.packageManager

  val launchIntent = pm.getLaunchIntentForPackage(packageName ?: TAILSCALE_PACKAGE_NAME)
  launchIntent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
  if (launchIntent != null) ctx.startActivity(launchIntent)
}

private fun tailscaleReceiverAction(ctx: Context, packageName: String?, @Action actionType: String) {
  val targetPackage = packageName ?: TAILSCALE_PACKAGE_NAME
  val action = "${targetPackage}.${actionType}"

  val intent = Intent(action).apply {
    `package` = targetPackage
  }

  ctx.sendBroadcast(intent)
}

fun connectVPN(ctx: Context, packageName: String?) {
  tailscaleReceiverAction(ctx, packageName, ActionType.CONNECT)
}

fun disconnectVPN(ctx: Context, packageName: String?) {
  tailscaleReceiverAction(ctx, packageName, ActionType.DISCONNECT)
}
