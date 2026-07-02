package com.example.wakebyvolume

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.core.app.NotificationCompat
import androidx.media.VolumeProviderCompat

/**
 * Service chạy nền, chịu trách nhiệm:
 *  - Khi màn hình TẮT: tạo và kích hoạt (arm) một MediaSession giả, có VolumeProvider
 *    riêng để "chiếm" quyền xử lý phím Volume. Nhờ vậy hệ thống KHÔNG hiển thị
 *    thanh trượt (slider) âm lượng mặc định nữa, vì âm lượng lúc này do app tự quản lý.
 *  - Khi phát hiện phím Volume được nhấn lúc màn hình tắt: đánh thức màn hình bằng WakeLock.
 *  - Khi màn hình BẬT trở lại: hủy (disarm) MediaSession ngay lập tức, trả quyền
 *    điều khiển âm lượng lại cho hệ thống như bình thường.
 */
class WakeService : Service() {

    private var mediaSession: MediaSessionCompat? = null
    private lateinit var powerManager: PowerManager
    private var wakeLock: PowerManager.WakeLock? = null

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> armSession()
                Intent.ACTION_SCREEN_ON -> disarmSession()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        startForeground(NOTIFICATION_ID, buildNotification())

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }
        registerReceiver(screenReceiver, filter)

        // Nếu service được khởi động trong lúc màn hình đang tắt sẵn thì kích hoạt luôn
        if (!powerManager.isInteractive) {
            armSession()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        disarmSession()
        try {
            unregisterReceiver(screenReceiver)
        } catch (e: IllegalArgumentException) {
            // receiver đã gỡ rồi, bỏ qua
        }
        super.onDestroy()
    }

    /** Kích hoạt MediaSession giả để chiếm quyền xử lý phím volume, không hiện slider hệ thống */
    private fun armSession() {
        if (mediaSession != null) return

        val session = MediaSessionCompat(this, "WakeByVolumeSession")

        val volumeProvider = object : VolumeProviderCompat(
            VOLUME_CONTROL_RELATIVE,
            MAX_VOLUME,
            CURRENT_VOLUME
        ) {
            override fun onAdjustVolume(direction: Int) {
                // Cố tình KHÔNG gọi setCurrentVolume(...) để tránh việc hệ thống
                // vẽ lại/thay đổi thanh trượt âm lượng. Ta chỉ dùng sự kiện này
                // như một "tín hiệu" rằng phím volume vừa được nhấn.
                wakeScreen()
            }
        }

        session.setPlaybackToRemote(volumeProvider)

        val stateBuilder = PlaybackStateCompat.Builder()
            .setActions(PlaybackStateCompat.ACTION_PLAY)
            .setState(PlaybackStateCompat.STATE_PLAYING, 0, 1f)
        session.setPlaybackState(stateBuilder.build())

        session.isActive = true
        mediaSession = session
    }

    /** Kết thúc MediaSession ngay khi màn hình bật, trả quyền volume lại cho hệ thống */
    private fun disarmSession() {
        mediaSession?.let {
            it.isActive = false
            it.release()
        }
        mediaSession = null

        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    private fun wakeScreen() {
        if (powerManager.isInteractive) return

        @Suppress("DEPRECATION")
        val flags = PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE

        @Suppress("DEPRECATION")
        val newWakeLock = powerManager.newWakeLock(flags, "WakeByVolume::WakeLock")
        newWakeLock.acquire(3000)
        wakeLock = newWakeLock
    }

    private fun buildNotification(): Notification {
        val channelId = "wake_by_volume_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_MIN
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_power_off)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 101
        private const val MAX_VOLUME = 100
        private const val CURRENT_VOLUME = 50
    }
}
