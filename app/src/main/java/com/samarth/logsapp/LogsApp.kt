package com.samarth.logsapp

import android.app.Application
import com.samarth.logsapp.data.local.AppDatabase
import com.samarth.logsapp.data.local.LogFileStore
import com.samarth.logsapp.data.remote.LogsRepository
import com.samarth.logsapp.sync.ConnectivityObserver
import com.samarth.logsapp.sync.SyncManager
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Holds the app's shared singletons for its whole lifetime: the Supabase
 * client, the local per-day .md file store, the Room sync ledger, and the
 * sync manager that ties them together.
 *
 * SUPABASE_URL / SUPABASE_ANON_KEY come from BuildConfig, populated at
 * build time from gradle properties (local) or CI secrets (release
 * builds). The anon key is safe to ship — access control is enforced by
 * Postgres RLS on the `daily_logs` table, not by hiding this key.
 */
class LogsApp : Application() {

    lateinit var supabase: SupabaseClient
        private set

    lateinit var logFileStore: LogFileStore
        private set

    lateinit var syncManager: SyncManager
        private set

    lateinit var connectivityObserver: ConnectivityObserver
        private set

    override fun onCreate() {
        super.onCreate()

        supabase = createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
        }

        logFileStore = LogFileStore(this)
        connectivityObserver = ConnectivityObserver(this)

        val database = AppDatabase.getInstance(this)
        val logsRepository = LogsRepository(supabase)
        syncManager = SyncManager(
            fileStore = logFileStore,
            syncedDayDao = database.syncedDayDao(),
            logsRepository = logsRepository
        )
    }
}
