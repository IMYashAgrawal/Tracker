package com.yash.tracker.data.remote

import com.yash.tracker.BuildConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

/**
 * Supabase client configured with Postgrest only — no Auth module.
 * The anon key grants access; the single RLS policy is `true`.
 */
object SupabaseManager {
    val client: SupabaseClient by lazy {
        val url = BuildConfig.SUPABASE_URL.trim()
        val key = BuildConfig.SUPABASE_KEY.trim()

        require(url.isNotBlank() && (url.startsWith("http://") || url.startsWith("https://"))) {
            "Supabase URL is not configured. Please add 'supabase.url=https://YOUR_PROJECT.supabase.co' to local.properties and rebuild the app."
        }
        require(key.isNotBlank()) {
            "Supabase Key is not configured. Please add 'supabase.key=YOUR_ANON_KEY' to local.properties and rebuild the app."
        }

        createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = key
        ) {
            install(Postgrest)
        }
    }
}
