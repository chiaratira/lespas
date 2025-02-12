package site.leos.apps.lespas

import android.accounts.AccountManager
import android.app.ActivityManager
import android.app.Application
import android.app.admin.DevicePolicyManager
import android.content.BroadcastReceiver
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.preference.PreferenceManager
import site.leos.apps.lespas.photo.PhotoRepository
import site.leos.apps.lespas.sync.SyncAdapter

class SystemBroadcastReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val accounts = AccountManager.get(context).getAccountsByType(context.getString(R.string.account_type_nc))

        when(intent.action) {
            AccountManager.ACTION_ACCOUNT_REMOVED -> {
                // When our account has been removed, delete all user data
                // TODO API level 26 required
                intent.extras?.apply {
                    // TODO supporting multiple NC account by checking KEY_ACCOUNT_NAME
                    if (getString(AccountManager.KEY_ACCOUNT_TYPE, "") == context.getString(R.string.account_type_nc))
                        (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }
            }
            DevicePolicyManager.ACTION_DEVICE_OWNER_CHANGED-> {
                // When phone owner changed, delete all user data, remove accounts
                if (accounts.isNotEmpty()) {
                    AccountManager.get(context).removeAccount(accounts[0], null, null, null)
                    (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData()
                }
            }
            Intent.ACTION_BOOT_COMPLETED -> {
                // Turn on periodic sync after bootup
                if (accounts.isNotEmpty()) {
                    if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(context.getString(R.string.sync_pref_key), false)) {
                        ContentResolver.setSyncAutomatically(accounts[0], context.getString(R.string.sync_authority), true)
                        ContentResolver.addPeriodicSync(
                            accounts[0], context.getString(R.string.sync_authority),
                            Bundle().apply { putInt(SyncAdapter.ACTION, SyncAdapter.SYNC_REMOTE_CHANGES) },
                            6 * 3600L
                        )
                    }

                    // Clear cache
                    //context.cacheDir.deleteRecursively()
                }
            }
            Intent.ACTION_LOCALE_CHANGED -> if (accounts.isNotEmpty()) Thread { PhotoRepository(context.applicationContext as Application).clearLocality() }.start()
        }
    }
}