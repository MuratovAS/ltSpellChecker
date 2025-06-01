package app.muratovAS.ltSpellChecker.app

import android.app.Application
import android.content.SharedPreferences
import androidx.preference.PreferenceManager
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

class App : Application() {

    companion object {
        lateinit var instance: App
            private set

        val prefs: SharedPreferences by lazy {
            PreferenceManager.getDefaultSharedPreferences(instance.applicationContext)
        }

        private const val CONNECTION_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 30_000

        lateinit var httpClient: OkHttpClient
    }

    private fun provideHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
        return builder
            .connectTimeout(CONNECTION_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .readTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .writeTimeout(READ_TIMEOUT_MS.toLong(), TimeUnit.MILLISECONDS)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        httpClient = provideHttpClient()
    }
}