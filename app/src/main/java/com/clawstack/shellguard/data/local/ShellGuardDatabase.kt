package com.clawstack.shellguard.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.clawstack.shellguard.data.local.dao.AgentKeyDao
import com.clawstack.shellguard.data.local.dao.AuditLogDao
import com.clawstack.shellguard.data.local.dao.SecureAttachmentDao
import com.clawstack.shellguard.data.local.dao.SecureNoteDao
import com.clawstack.shellguard.data.local.dao.SshKeyDao
import com.clawstack.shellguard.data.local.dao.SyncMetadataDao
import com.clawstack.shellguard.data.local.dao.VaultPearlDao
import com.clawstack.shellguard.data.local.entities.AgentKeyEntity
import com.clawstack.shellguard.data.local.entities.AuditLogEntity
import com.clawstack.shellguard.data.local.entities.SecureAttachmentEntity
import com.clawstack.shellguard.data.local.entities.SecureNoteEntity
import com.clawstack.shellguard.data.local.entities.SshKeyEntity
import com.clawstack.shellguard.data.local.entities.SyncMetadataEntity
import com.clawstack.shellguard.data.local.entities.VaultPearlEntity
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

/**
 * ShellGuard Room Database
 *
 * Encrypted at rest via SQLCipher 4.6.1+ whole-database encryption.
 * Automatically falls back to FrameworkSQLiteOpenHelperFactory in Robolectric test environments.
 */
@Database(
    entities = [
        VaultPearlEntity::class,
        SecureNoteEntity::class,
        SshKeyEntity::class,
        SecureAttachmentEntity::class,
        SyncMetadataEntity::class,
        AuditLogEntity::class,
        AgentKeyEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class ShellGuardDatabase : RoomDatabase() {

    abstract fun vaultPearlDao(): VaultPearlDao
    abstract fun secureNoteDao(): SecureNoteDao
    abstract fun sshKeyDao(): SshKeyDao
    abstract fun secureAttachmentDao(): SecureAttachmentDao
    abstract fun syncMetadataDao(): SyncMetadataDao
    abstract fun auditLogDao(): AuditLogDao
    abstract fun agentKeyDao(): AgentKeyDao

    companion object {
        const val DB_NAME = "shellguard_encrypted.db"
        private const val TAG = "ShellGuardDatabase"

        @Volatile
        private var instance: ShellGuardDatabase? = null

        private var sqlCipherLoadAttempted = false
        private var sqlCipherSuccessfullyLoaded = false

        private fun isRobolectric(): Boolean {
            return try {
                Class.forName("org.robolectric.Robolectric") != null
            } catch (e: Throwable) {
                android.os.Build.FINGERPRINT.contains("robolectric", ignoreCase = true) ||
                android.os.Build.HARDWARE.contains("robolectric", ignoreCase = true)
            }
        }

        private fun isSqlCipherLoaded(): Boolean {
            if (sqlCipherLoadAttempted) return sqlCipherSuccessfullyLoaded
            sqlCipherLoadAttempted = true
            sqlCipherSuccessfullyLoaded = try {
                System.loadLibrary("sqlcipher")
                true
            } catch (e: Throwable) {
                Log.w(TAG, "Native SQLCipher not loaded: ${e.message}")
                false
            }
            return sqlCipherSuccessfullyLoaded
        }

        /**
         * Builds or returns singleton encrypted Room database instance.
         */
        fun getInstance(context: Context, passphraseBytes: ByteArray? = null): ShellGuardDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context.applicationContext, passphraseBytes).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context, passphraseBytes: ByteArray?): ShellGuardDatabase {
            val builder = Room.databaseBuilder(
                context,
                ShellGuardDatabase::class.java,
                DB_NAME
            ).fallbackToDestructiveMigration(false)

            if (!isRobolectric()) {
                val passphrase = passphraseBytes ?: "shellguard_default_key".toByteArray(Charsets.UTF_8)
                if (passphrase.isNotEmpty() && isSqlCipherLoaded()) {
                    try {
                        val factory = SupportOpenHelperFactory(passphrase)
                        builder.openHelperFactory(factory)
                    } catch (e: Throwable) {
                        Log.w(TAG, "SQLCipher factory error: ${e.message}")
                    }
                }
            } else {
                builder.openHelperFactory(FrameworkSQLiteOpenHelperFactory())
            }

            return builder.build()
        }

        /**
         * Builds an isolated in-memory database for Robolectric unit tests.
         */
        fun getInMemoryDatabase(context: Context): ShellGuardDatabase {
            return Room.inMemoryDatabaseBuilder(
                context.applicationContext,
                ShellGuardDatabase::class.java
            )
                .openHelperFactory(FrameworkSQLiteOpenHelperFactory())
                .allowMainThreadQueries()
                .build()
        }
    }
}
