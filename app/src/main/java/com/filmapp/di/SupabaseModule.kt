package com.filmapp.di

import com.filmapp.BuildConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.realtime
import javax.inject.Singleton

/**
 * Supabase DI Module.
 *
 * PostgreSQL Schema Design (to be created in Supabase Dashboard → SQL Editor):
 *
 * ── profiles ──────────────────────────────────────
 * CREATE TABLE public.profiles (
 *     id          UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
 *     username    TEXT UNIQUE,
 *     display_name TEXT,
 *     avatar_url  TEXT,
 *     created_at  TIMESTAMPTZ DEFAULT now(),
 *     updated_at  TIMESTAMPTZ DEFAULT now()
 * );
 * ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY "Users can view own profile" ON public.profiles FOR SELECT USING (auth.uid() = id);
 * CREATE POLICY "Users can update own profile" ON public.profiles FOR UPDATE USING (auth.uid() = id);
 *
 * ── watchlist ─────────────────────────────────────
 * CREATE TABLE public.watchlist (
 *     id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
 *     user_id     UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
 *     movie_id    INTEGER NOT NULL,
 *     title       TEXT NOT NULL,
 *     poster_path TEXT,
 *     backdrop_path TEXT,
 *     vote_average DOUBLE PRECISION,
 *     overview    TEXT,
 *     added_at    TIMESTAMPTZ DEFAULT now(),
 *     UNIQUE(user_id, movie_id)
 * );
 * ALTER TABLE public.watchlist ENABLE ROW LEVEL SECURITY;
 * CREATE POLICY "Users can view own watchlist" ON public.watchlist FOR SELECT USING (auth.uid() = user_id);
 * CREATE POLICY "Users can insert own watchlist" ON public.watchlist FOR INSERT WITH CHECK (auth.uid() = user_id);
 * CREATE POLICY "Users can delete own watchlist" ON public.watchlist FOR DELETE USING (auth.uid() = user_id);
 *
 * ── Auto-create profile on signup (trigger) ──────
 * CREATE OR REPLACE FUNCTION public.handle_new_user()
 * RETURNS TRIGGER AS $$
 * BEGIN
 *   INSERT INTO public.profiles (id, username, display_name)
 *   VALUES (NEW.id, NEW.raw_user_meta_data->>'username', NEW.raw_user_meta_data->>'display_name');
 *   RETURN NEW;
 * END;
 * $$ LANGUAGE plpgsql SECURITY DEFINER;
 *
 * CREATE TRIGGER on_auth_user_created
 *   AFTER INSERT ON auth.users
 *   FOR EACH ROW EXECUTE FUNCTION public.handle_new_user();
 *
 * Enable Realtime on the watchlist table via Supabase Dashboard for live sync.
 */
@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = BuildConfig.SUPABASE_URL,
            supabaseKey = BuildConfig.SUPABASE_ANON_KEY
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideSupabaseAuth(client: SupabaseClient): Auth {
        return client.auth
    }

    @Provides
    @Singleton
    fun provideSupabasePostgrest(client: SupabaseClient): Postgrest {
        return client.postgrest
    }

    @Provides
    @Singleton
    fun provideSupabaseRealtime(client: SupabaseClient): Realtime {
        return client.realtime
    }
}
