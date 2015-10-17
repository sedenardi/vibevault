package com.code.android.vibevault;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.view.KeyEvent;

public class RemoteControlReceiver extends BroadcastReceiver{

	@Override
	public void onReceive(Context context, Intent intent) {
		String action = intent.getAction();
		if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)) {
			context.startService(new Intent(PlaybackService.ACTION_PAUSE));
		}
		else if (action.equals(Intent.ACTION_MEDIA_BUTTON)) {
			KeyEvent key = (KeyEvent) intent.getExtras().get(Intent.EXTRA_KEY_EVENT);
			
			if (key.getAction() != KeyEvent.ACTION_DOWN) {
				return;
			}
			
			switch (key.getKeyCode()) {
			case KeyEvent.KEYCODE_HEADSETHOOK:
            case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE:
                context.startService(new Intent(PlaybackService.ACTION_TOGGLE));
                break;
            case KeyEvent.KEYCODE_MEDIA_PLAY:
                context.startService(new Intent(PlaybackService.ACTION_PLAY));
                break;
            case KeyEvent.KEYCODE_MEDIA_PAUSE:
                context.startService(new Intent(PlaybackService.ACTION_PAUSE));
                break;
            case KeyEvent.KEYCODE_MEDIA_STOP:
                context.startService(new Intent(PlaybackService.ACTION_STOP));
                break;
            case KeyEvent.KEYCODE_MEDIA_NEXT:
                context.startService(new Intent(PlaybackService.ACTION_NEXT));
                break;
            case KeyEvent.KEYCODE_MEDIA_PREVIOUS:
                context.startService(new Intent(PlaybackService.ACTION_PREV));
                break;
			}
		}		
	}

}
