package hu.zenith.gamebox.app

import android.content.Context

/** Helyi mentés: legjobb pontszámok és játékonkénti beállítások (az eredeti zenith_<játék>_hs / _<kulcs> localStorage-kulcsok megfelelői). */
class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("zenith_prefs", Context.MODE_PRIVATE)

    fun best(id: String): Int = sp.getInt("zenith_${id}_hs", 0)

    fun setBest(id: String, value: Int) {
        sp.edit().putInt("zenith_${id}_hs", value).apply()
    }

    fun opt(id: String, key: String): String? = sp.getString("zenith_${id}_$key", null)

    fun setOpt(id: String, key: String, value: String) {
        sp.edit().putString("zenith_${id}_$key", value).apply()
    }
}
