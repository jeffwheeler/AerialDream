package com.codingbuffalo.aerialdream;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.codingbuffalo.aerialdream.service.AerialVideo;
import com.google.android.exoplayer.ExoPlaybackException;
import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.MediaCodecTrackRenderer;
import com.google.android.exoplayer.MediaCodecVideoTrackRenderer;
import com.google.android.exoplayer.extractor.ExtractorSampleSource;
import com.google.android.exoplayer.upstream.Allocator;
import com.google.android.exoplayer.upstream.DataSource;
import com.google.android.exoplayer.upstream.DefaultAllocator;
import com.google.android.exoplayer.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer.upstream.DefaultUriDataSource;

public class SimplePlayer extends RelativeLayout implements MediaCodecVideoTrackRenderer.EventListener, TextureView.SurfaceTextureListener, ExoPlayer.Listener {
	private static final int BUFFER_SEGMENT_SIZE  = 64 * 1024;
	private static final int BUFFER_SEGMENT_COUNT = 256;
	
	private ExoPlayer      mPlayer;
	private Surface        mSurface;
	private PlayerListener mPlayerListener;
	
	private TextView mLocationView;
	
	public SimplePlayer(Context context) {
		super(context);
	}
	
	public SimplePlayer(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public SimplePlayer(Context context, AttributeSet attrs, int defStyleAttr) {
		super(context, attrs, defStyleAttr);
	}
	
	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public SimplePlayer(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
		super(context, attrs, defStyleAttr, defStyleRes);
	}
	
	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		
		LayoutInflater.from(getContext()).inflate(R.layout.simple_player, this, true);
		
		// Load preferences
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
		boolean showLocation = preferences.getBoolean("show_location", true);
		
		// Configure views
		TextureView textureView = (TextureView) findViewById(R.id.texture);
		mLocationView = (TextView) findViewById(R.id.location);
		
		mLocationView.setVisibility(showLocation ? VISIBLE : GONE);
		
		mPlayer = ExoPlayer.Factory.newInstance(1);
		mPlayer.addListener(this);
		
		textureView.setSurfaceTextureListener(this);
	}
	
	public void setPlayerListener(PlayerListener playerListener) {
		mPlayerListener = playerListener;
	}
	
	public void load(AerialVideo video) {
		if (mSurface == null) {
			throw new IllegalStateException("Player not ready");
		}
		
		Handler handler = new Handler();
		
		DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter(handler, null);
		DataSource dataSource = new DefaultUriDataSource(getContext(), bandwidthMeter, "AerialDream");
		Allocator allocator = new DefaultAllocator(BUFFER_SEGMENT_SIZE);
		ExtractorSampleSource sampleSource = new ExtractorSampleSource(Uri.parse(video.getUrl()), dataSource, allocator, BUFFER_SEGMENT_COUNT * BUFFER_SEGMENT_SIZE);
		MediaCodecVideoTrackRenderer videoRenderer = new MediaCodecVideoTrackRenderer(getContext(), sampleSource, MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT, 5000, handler, this, 50);
		
		mPlayer.seekTo(0);
		mPlayer.prepare(videoRenderer);
		mPlayer.setPlayWhenReady(false);
		mPlayer.sendMessage(videoRenderer, MediaCodecVideoTrackRenderer.MSG_SET_SURFACE, mSurface);
		
		// Set video description
		mLocationView.setText(video.getLocation());
	}
	
	public void play() {
		mPlayer.setPlayWhenReady(true);
	}
	
	public long getDuration() {
		return mPlayer.getDuration();
	}
	
	public void release() {
		mPlayer.release();
	}
	
	@Override
	public void onDroppedFrames(int count, long elapsed) {
	}
	
	@Override
	public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees, float pixelWidthHeightRatio) {
	}
	
	@Override
	public void onDrawnToSurface(Surface surface) {
	}
	
	@Override
	public void onDecoderInitializationError(MediaCodecTrackRenderer.DecoderInitializationException e) {
	}
	
	@Override
	public void onCryptoError(MediaCodec.CryptoException e) {
	}
	
	@Override
	public void onDecoderInitialized(String decoderName, long elapsedRealtimeMs, long initializationDurationMs) {
	}
	
	/* OnSurfaceTextureListener */
	@Override
	public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
		mSurface = new Surface(surface);
		
		if (mPlayerListener != null) {
			mPlayerListener.onPlayerInitialized(this);
		}
	}
	
	@Override
	public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
	}
	
	@Override
	public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
		return false;
	}
	
	@Override
	public void onSurfaceTextureUpdated(SurfaceTexture surface) {
	}
	
	/* ExoPlayerListener */
	@Override
	public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
		Log.d("aaa", "State: " + playbackState + " duration: " + mPlayer.getDuration());
		
		if (mPlayerListener == null) {
			return;
		}
		
		switch (playbackState) {
			case ExoPlayer.STATE_READY:
				mPlayerListener.onVideoLoaded();
				break;
		}
	}
	
	@Override
	public void onPlayWhenReadyCommitted() {
	}
	
	@Override
	public void onPlayerError(ExoPlaybackException error) {
	}
	
	public interface PlayerListener {
		void onPlayerInitialized(SimplePlayer simplePlayer);
		
		void onVideoLoaded();
	}
}
