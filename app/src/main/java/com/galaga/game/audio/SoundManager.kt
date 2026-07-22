package com.galaga.game.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.galaga.game.R
import com.galaga.game.model.SoundEvent

class SoundManager(context: Context) {

    private val soundPool: SoundPool
    
    private var shootSoundId: Int = 0
    private var explosionSoundId: Int = 0
    private var powerupSoundId: Int = 0
    
    private var bgmPlayer: MediaPlayer? = null
    
    var isEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                bgmPlayer?.pause()
            } else {
                if (isPlayingGame) {
                    bgmPlayer?.start()
                }
            }
        }
        
    private var isPlayingGame: Boolean = false

    init {
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
            
        soundPool = SoundPool.Builder()
            .setMaxStreams(10)
            .setAudioAttributes(attributes)
            .build()
            
        shootSoundId = soundPool.load(context, R.raw.shoot, 1)
        explosionSoundId = soundPool.load(context, R.raw.explosion, 1)
        powerupSoundId = soundPool.load(context, R.raw.powerup, 1)
        
        bgmPlayer = MediaPlayer.create(context, R.raw.bgm)
        bgmPlayer?.isLooping = true
    }

    fun playEvent(event: SoundEvent) {
        if (!isEnabled) return
        
        val soundId = when (event) {
            SoundEvent.SHOOT -> shootSoundId
            SoundEvent.EXPLOSION -> explosionSoundId
            SoundEvent.POWERUP -> powerupSoundId
        }
        
        if (soundId != 0) {
            soundPool.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }
    
    fun setGameState(playing: Boolean) {
        isPlayingGame = playing
        if (isEnabled) {
            if (playing) {
                if (bgmPlayer?.isPlaying == false) {
                    bgmPlayer?.start()
                }
            } else {
                if (bgmPlayer?.isPlaying == true) {
                    bgmPlayer?.pause()
                }
            }
        }
    }
    
    fun release() {
        soundPool.release()
        bgmPlayer?.release()
        bgmPlayer = null
    }
}
