package com.example.aipc

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.io.File

/**
 * 语音合成引擎，封装 Rust 推理库的 JNI 调用
 */
class TtsEngine {
    private var isInitialized = false

    init {
        try {
            System.loadLibrary("gpt_sovits_android")
        } catch (e: UnsatisfiedLinkError) {
            Log.e("TtsEngine", "Failed to load native library", e)
        }
    }

    /**
     * 初始化模型（需传入模型文件路径）
     */
    external fun init(modelPath: String): Boolean

    /**
     * 合成语音
     * @param text 要合成的文本
     * @param refAudioPath 参考音频路径（用于克隆音色）
     * @return PCM 数据（16kHz, 16bit, mono）
     */
    external fun synthesize(text: String, refAudioPath: String): ByteArray?

    /**
     * 释放资源（预留）
     */
    external fun release()

    /**
     * 便捷方法：初始化并播放合成的音频
     */
    fun speak(text: String, refAudioPath: String): Boolean {
        if (!isInitialized) {
            // 假设模型文件放在 assets 目录，此处应实际初始化
            Log.w("TtsEngine", "Engine not initialized, attempting init...")
            // 实际项目中应传入正确的模型路径
            return false
        }
        val pcmData = synthesize(text, refAudioPath)
        if (pcmData == null || pcmData.isEmpty()) {
            Log.e("TtsEngine", "Synthesis failed")
            return false
        }
        playPcm(pcmData)
        return true
    }

    private fun playPcm(pcmData: ByteArray) {
        try {
            val audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(16000)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcmData.size)
                .build()
            audioTrack.play()
            audioTrack.write(pcmData, 0, pcmData.size)
            audioTrack.stop()
            audioTrack.release()
        } catch (e: Exception) {
            Log.e("TtsEngine", "Audio playback failed", e)
        }
    }
}