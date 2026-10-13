package io.github.supermonster003.autojs6.plugin.three.block.compose.designer.parse

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.RemoteException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.autojs.plugin.typescript.parser.ITypeScriptParserPlugin
import java.io.Closeable
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import org.autojs.plugin.typescript.parser.TypeScriptParserContract as Contract

/**
 * Client of the TypeScript Engine parser service (TypeScript parser contract v1, roadmap P9.2 / D42). The
 * binding is made on first use and kept until [close], because the service keeps its parser page only while
 * it is bound. Trees travel through a file in the plugin's cache directory that the service writes through a
 * descriptor; its length and SHA-256 are checked against the result before the JSON is read.
 */
internal class TsxParserClient(context: Context) : Closeable {
    private val context = context.applicationContext
    private val lock = Mutex()
    private var connection: ServiceConnection? = null
    private var parser: ITypeScriptParserPlugin? = null

    /** Why a TSX document cannot be parsed right now; the editor then opens it read-only. */
    enum class Reason {
        /** The TypeScript Engine plugin is not installed. */
        NOT_INSTALLED,
        /** It is installed but has no parser service, or a different contract: older than [MIN_VERSION_NAME]. */
        TOO_OLD,
        /** It has the service but would not bind it (no permission, a disabled or quarantined process, a crash). */
        UNAVAILABLE,
    }

    sealed class Outcome {
        data class Parsed(val tree: TsTree) : Outcome()
        /** The service answered with a contract error such as a timeout or a busy parser. */
        data class Failed(val code: String, val message: String) : Outcome()
        data class Unavailable(val reason: Reason, val installedVersion: String?) : Outcome()
    }

    suspend fun parse(fileName: String, sourceText: String): Outcome = lock.withLock {
        withContext(Dispatchers.IO) {
            val bound = when (val connected = connect()) {
                is Connected.Ready -> connected.parser
                is Connected.Missing -> return@withContext Outcome.Unavailable(connected.reason, installedVersion())
            }
            try {
                request(bound, fileName, sourceText)
            } catch (error: RemoteException) {
                // The service process died or the binder broke: bind again on the next request.
                disconnect()
                Outcome.Unavailable(Reason.UNAVAILABLE, installedVersion())
            } catch (error: SecurityException) {
                disconnect()
                Outcome.Unavailable(Reason.UNAVAILABLE, installedVersion())
            }
        }
    }

    override fun close() {
        disconnect()
    }

    private sealed class Connected {
        class Ready(val parser: ITypeScriptParserPlugin) : Connected()
        class Missing(val reason: Reason) : Connected()
    }

    private suspend fun connect(): Connected {
        parser?.takeIf { it.asBinder().isBinderAlive }?.let { return Connected.Ready(it) }
        disconnect()
        val intent = Intent(Contract.ACTION).addCategory(CATEGORY).setPackage(PACKAGE_NAME)
        @Suppress("DEPRECATION")
        val service = context.packageManager.queryIntentServices(intent, 0).firstOrNull()?.serviceInfo
            ?: return Connected.Missing(if (installedVersion() == null) Reason.NOT_INSTALLED else Reason.TOO_OLD)
        val ready = CompletableDeferred<IBinder?>()
        val callbacks = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName, binder: IBinder) { ready.complete(binder) }
            override fun onServiceDisconnected(name: ComponentName) { parser = null }
            override fun onBindingDied(name: ComponentName) { parser = null }
            override fun onNullBinding(name: ComponentName) { ready.complete(null) }
        }
        val bound = try {
            context.bindService(Intent(intent).setComponent(ComponentName(service.packageName, service.name)), callbacks, Context.BIND_AUTO_CREATE)
        } catch (error: SecurityException) {
            false
        }
        if (!bound) {
            runCatching { context.unbindService(callbacks) }
            return Connected.Missing(Reason.UNAVAILABLE)
        }
        connection = callbacks
        val binder = try {
            withTimeout(BIND_TIMEOUT_MS) { ready.await() }
        } catch (error: TimeoutCancellationException) {
            null
        }
        if (binder == null) {
            disconnect()
            return Connected.Missing(Reason.UNAVAILABLE)
        }
        val remote = ITypeScriptParserPlugin.Stub.asInterface(binder)
        val compatible = try {
            remote.contractVersion == Contract.VERSION && remote.contractHash == Contract.HASH
        } catch (error: RemoteException) {
            disconnect()
            return Connected.Missing(Reason.UNAVAILABLE)
        }
        if (!compatible) {
            disconnect()
            return Connected.Missing(Reason.TOO_OLD)
        }
        parser = remote
        return Connected.Ready(remote)
    }

    private fun disconnect() {
        parser = null
        connection?.let { runCatching { context.unbindService(it) } }
        connection = null
    }

    private fun request(parser: ITypeScriptParserPlugin, fileName: String, sourceText: String): Outcome {
        val directory = File(context.cacheDir, TREE_DIRECTORY).apply { mkdirs() }
        val file = File(directory, UUID.randomUUID().toString() + ".json")
        try {
            val request = Bundle().apply {
                putInt(Contract.KEY_CONTRACT_VERSION, Contract.VERSION)
                putString(Contract.KEY_CONTRACT_HASH, Contract.HASH)
                putInt(Contract.KEY_TREE_SCHEMA_REVISION, Contract.TREE_SCHEMA_REVISION)
                putString(Contract.KEY_REQUEST_ID, UUID.randomUUID().toString())
                putString(Contract.KEY_FILE_NAME, fileName)
                putString(Contract.KEY_SOURCE_TEXT, sourceText)
                putLong(Contract.KEY_TIMEOUT_MS, Contract.DEFAULT_TIMEOUT_MS)
            }
            val descriptor = ParcelFileDescriptor.open(
                file,
                ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_WRITE_ONLY,
            )
            val result = descriptor.use { parser.parse(request, it) }
            if (result.getString(Contract.KEY_OUTCOME) != Contract.OUTCOME_PARSED) {
                return Outcome.Failed(
                    result.getString(Contract.KEY_ERROR_CODE) ?: Contract.ERROR_UNAVAILABLE,
                    result.getString(Contract.KEY_ERROR_MESSAGE) ?: "",
                )
            }
            val bytes = file.readBytes()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
            if (bytes.size.toLong() != result.getLong(Contract.KEY_TREE_BYTE_LENGTH) || digest != result.getString(Contract.KEY_TREE_SHA256)) {
                return Outcome.Failed(Contract.ERROR_OUTPUT_TRANSPORT, "The syntax tree arrived incomplete")
            }
            return try {
                Outcome.Parsed(TsTreeJson.parse(bytes.toString(Charsets.UTF_8)))
            } catch (error: TsTreeFormatException) {
                Outcome.Failed(Contract.ERROR_OUTPUT_TRANSPORT, error.message ?: "Unreadable syntax tree")
            }
        } finally {
            file.delete()
        }
    }

    /** The installed TypeScript Engine version name, or null when it is not installed. */
    fun installedVersion(): String? = try {
        context.packageManager.getPackageInfo(PACKAGE_NAME, 0).versionName ?: ""
    } catch (error: PackageManager.NameNotFoundException) {
        null
    }

    companion object {
        /** The TypeScript Engine plugin, the provider of the parser service. */
        const val PACKAGE_NAME = "io.github.supermonster003.autojs6.plugin.typescript"
        /** The first TypeScript Engine release with the parser service. */
        const val MIN_VERSION_NAME = "0.7.0"
        private const val CATEGORY = "typescript"
        private const val TREE_DIRECTORY = "tsx-trees"
        private const val BIND_TIMEOUT_MS = 10_000L

        /** True when an installed TypeScript Engine offers the parser service; nothing is bound. */
        fun hasParserService(context: Context): Boolean {
            val intent = Intent(Contract.ACTION).addCategory(CATEGORY).setPackage(PACKAGE_NAME)
            @Suppress("DEPRECATION")
            return context.packageManager.queryIntentServices(intent, 0).isNotEmpty()
        }
    }
}
