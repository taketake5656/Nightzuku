package moe.shizuku.manager.utils

import android.os.Parcel
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuApiConstants

/**
 * NightDog toggle over the Nightzuku server's custom binder transactions.
 * Transaction codes must match rikka.shizuku.server.ServerConstants.
 */
object NightDogApis {

    private const val TRANSACTION_SET_ENABLED = 10002
    private const val TRANSACTION_GET_ENABLED = 10003

    fun isEnabled(): Boolean {
        val binder = Shizuku.getBinder() ?: return false
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        return try {
            data.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR)
            binder.transact(TRANSACTION_GET_ENABLED, data, reply, 0)
            reply.readException()
            reply.readInt() != 0
        } finally {
            data.recycle()
            reply.recycle()
        }
    }

    fun setEnabled(enabled: Boolean) {
        val binder = Shizuku.getBinder() ?: throw IllegalStateException("Nightzuku service is not running.")
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken(ShizukuApiConstants.BINDER_DESCRIPTOR)
            data.writeInt(if (enabled) 1 else 0)
            binder.transact(TRANSACTION_SET_ENABLED, data, reply, 0)
            reply.readException()
        } finally {
            data.recycle()
            reply.recycle()
        }
    }
}
