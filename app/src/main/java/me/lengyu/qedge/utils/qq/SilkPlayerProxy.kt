package me.lengyu.qedge.utils.qq

import com.tencent.mobileqq.qqaudio.audioplayer.SilkPlayer

/**
 * 封装 QQ 内置 SilkPlayer 播放 silk 格式音频。
 *
 * SilkPlayer 的包名和方法名稳定（标准 Android MediaPlayer 命名规范），
 * 通过 qqinterface stub 在编译期直接引用，运行时由 HybridClassLoader 加载真实类。
 *
 * 调用流程：createPlayer → setDataSource → prepare → start
 * 控制：pause / stop / seekTo
 * 查询：isPlaying / getCurrentPosition / getDuration
 */
object SilkPlayerProxy {
    /** 创建 SilkPlayer 实例，失败返回 null。 */
    fun createPlayer(): SilkPlayer? {
        return try {
            SilkPlayer()
        } catch (e: Throwable) {
            null
        }
    }

    fun setDataSource(player: SilkPlayer, path: String): Boolean {
        return try {
            player.setDataSource(path)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun prepare(player: SilkPlayer): Boolean {
        return try {
            player.prepare()
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun start(player: SilkPlayer): Boolean {
        return try {
            player.start()
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun pause(player: SilkPlayer): Boolean {
        return try {
            player.pause()
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun stop(player: SilkPlayer): Boolean {
        return try {
            player.stop()
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun seekTo(player: SilkPlayer, msec: Int): Boolean {
        return try {
            player.seekTo(msec)
            true
        } catch (e: Throwable) {
            false
        }
    }

    fun isPlaying(player: SilkPlayer): Boolean {
        return try {
            player.isPlaying
        } catch (e: Throwable) {
            false
        }
    }

    fun currentPosition(player: SilkPlayer): Int {
        return try {
            player.currentPosition
        } catch (e: Throwable) {
            0
        }
    }

    fun duration(player: SilkPlayer): Int {
        return try {
            player.duration
        } catch (e: Throwable) {
            0
        }
    }
}
