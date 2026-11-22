package com.litetv.launcher

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import android.view.Gravity
import android.view.View
import android.widget.*
import java.util.Date

class MainActivity : Activity() {
 private val handler = Handler(Looper.getMainLooper())
 private lateinit var clock: TextView
 private lateinit var date: TextView
 private val tick = object : Runnable { override fun run() { clock.text = DateFormat.format("HH:mm", Date()); date.text = DateFormat.format("EEEE, dd MMMM yyyy", Date()); handler.postDelayed(this, 15000) } }
 private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
 private fun shape(color: Int, radius: Int) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat() }
 override fun onCreate(savedInstanceState: Bundle?) {
  super.onCreate(savedInstanceState)
  window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
  val root = FrameLayout(this).apply { setBackgroundResource(R.drawable.mountains) }
  val shade = View(this).apply { background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0x99081429.toInt(), 0x10081429, 0xCC081429.toInt())) }
  root.addView(shade, FrameLayout.LayoutParams(-1,-1))
  val panel = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(48),dp(32),dp(48),dp(34)) }
  root.addView(panel, FrameLayout.LayoutParams(-1,-1))
  val heading = TextView(this).apply { text = "LiteTV  •  HOME"; textSize = 15f; setTextColor(0xFFB8E7F4.toInt()); typeface = Typeface.DEFAULT_BOLD }
  panel.addView(heading)
  clock = TextView(this).apply { textSize = 66f; setTextColor(Color.WHITE); typeface = Typeface.create("sans-serif-light", Typeface.NORMAL) }
  panel.addView(clock)
  date = TextView(this).apply { textSize = 17f; setTextColor(0xFFE6EEF5.toInt()) }
  panel.addView(date)
  panel.addView(View(this), LinearLayout.LayoutParams(1,0,1f))
  val label = TextView(this).apply { text = "APLIKASI"; textSize = 15f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD }
  panel.addView(label)
  val scroll = HorizontalScrollView(this).apply { isHorizontalScrollBarEnabled = false }
  val appsRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0,dp(12),0,dp(6)) }
  scroll.addView(appsRow)
  panel.addView(scroll)
  val pm = packageManager
  val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
  val apps = pm.queryIntentActivities(intent, 0).filter { it.activityInfo.packageName != packageName }.distinctBy { it.activityInfo.packageName }.sortedBy { it.loadLabel(pm).toString().lowercase() }
  if (apps.isEmpty()) {
   val empty = TextView(this).apply { text = "Belum ada aplikasi TV terdeteksi"; setTextColor(Color.WHITE); textSize = 19f }
   appsRow.addView(empty)
  }
  apps.forEach { item ->
   val pkg = item.activityInfo.packageName
   val tile = LinearLayout(this).apply {
    orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; isFocusable = true; isFocusableInTouchMode = true
    background = shape(0xD9243653.toInt(), 16)
    setPadding(dp(12),dp(10),dp(12),dp(8))
    elevation = dp(3).toFloat()
   }
   val icon = ImageView(this).apply { setImageDrawable(item.loadIcon(pm)); scaleType = ImageView.ScaleType.FIT_CENTER }
   tile.addView(icon, LinearLayout.LayoutParams(dp(54),dp(54)))
   val name = TextView(this).apply { text = item.loadLabel(pm).toString(); setTextColor(Color.WHITE); textSize = 13f; maxLines = 1; ellipsize = android.text.TextUtils.TruncateAt.END; gravity = Gravity.CENTER }
   tile.addView(name, LinearLayout.LayoutParams(dp(116),dp(32)))
   tile.setOnFocusChangeListener { v, focused -> v.background = shape(if (focused) 0xFF167DAD.toInt() else 0xD9243653.toInt(),16); v.scaleX = if (focused) 1.07f else 1f; v.scaleY = if (focused) 1.07f else 1f }
   tile.setOnClickListener { try { val launch = pm.getLeanbackLaunchIntentForPackage(pkg) ?: pm.getLaunchIntentForPackage(pkg); if (launch != null) startActivity(launch) else Toast.makeText(this,"Tidak dapat membuka aplikasi",Toast.LENGTH_SHORT).show() } catch (e: Exception) { Toast.makeText(this,"Gagal membuka aplikasi",Toast.LENGTH_SHORT).show() } }
   appsRow.addView(tile, LinearLayout.LayoutParams(dp(146),dp(118)).apply { rightMargin = dp(12) })
  }
  val footer = TextView(this).apply { text = "Gunakan tombol arah dan OK pada remote  •  Tidak ada iklan atau layanan latar tambahan"; textSize = 12f; setTextColor(0xFFE0E7EE.toInt()) }
  panel.addView(footer)
  setContentView(root)
  appsRow.post { if (appsRow.childCount > 0) appsRow.getChildAt(0).requestFocus() }
 }
 override fun onResume() { super.onResume(); handler.removeCallbacks(tick); tick.run() }
 override fun onPause() { handler.removeCallbacks(tick); super.onPause() }
}
