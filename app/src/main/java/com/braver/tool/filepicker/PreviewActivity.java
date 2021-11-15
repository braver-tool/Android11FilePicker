/*
 *
 *  * Created by https://github.com/braver-tool on 12/10/21, 08:30 PM
 *  * Copyright (c) 2021 . All rights reserved.
 *  * Last modified 15/11/21, 05:50 PM
 *
 */

package com.braver.tool.filepicker;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.github.chrisbanes.photoview.PhotoView;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;

import java.util.Objects;

import static com.braver.tool.picker.BraveFileUtils.PREVIEW_IMAGE_FILE_PATH;
import static com.braver.tool.picker.BraveFileUtils.PREVIEW_VIDEO_FILE_PATH;

public class PreviewActivity extends AppCompatActivity implements View.OnClickListener {
    private PlayerView playerView;
    private SimpleExoPlayer videoPlayer;
    private ImageView imagePlayPause;
    private boolean isVideoEnded;
    private String imageFilePath, videoFilePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        initializeViews();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (videoPlayer != null) {
            videoPlayer.setPlayWhenReady(false);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (videoPlayer != null) {
            videoPlayer.release();
        }
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.videoCloseImageView) {
            onBackPressed();
        }
    }

    private void initializeViews() {
        playerView = findViewById(R.id.player_view_lib);
        imagePlayPause = findViewById(R.id.image_play_pause);
        PhotoView scaleImageView = findViewById(R.id.scaleImageView);
        LinearLayout videoViewParentLayout = findViewById(R.id.videoViewParentLayout);
        ImageView videoCloseImageView = findViewById(R.id.videoCloseImageView);
        videoCloseImageView.setOnClickListener(this);
        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            imageFilePath = bundle.getString(PREVIEW_IMAGE_FILE_PATH, "");
            videoFilePath = bundle.getString(PREVIEW_VIDEO_FILE_PATH, "");
        }
        if (!videoFilePath.isEmpty()) {
            scaleImageView.setVisibility(View.GONE);
            videoViewParentLayout.setVisibility(View.VISIBLE);
            initPlayer();
            setDataInView();
        } else if (!imageFilePath.isEmpty()) {
            videoViewParentLayout.setVisibility(View.GONE);
            scaleImageView.setVisibility(View.VISIBLE);
            Bitmap photoBitmap = BitmapFactory.decodeFile(imageFilePath);
            if (photoBitmap != null) {
                scaleImageView.setImageBitmap(photoBitmap);
            }
        }
    }


    /**
     * SettingUp exoplayer
     **/
    private void initPlayer() {
        try {
            videoPlayer = new SimpleExoPlayer.Builder(this).build();
            playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
            playerView.setPlayer(videoPlayer);
            AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.CONTENT_TYPE_MOVIE).build();
            videoPlayer.setAudioAttributes(audioAttributes, true);
        } catch (Exception e) {
            AppUtils.printLogConsole("initPlayer", "Exception-------->" + e.getMessage());
        }
    }

    private void setDataInView() {
        try {
            imagePlayPause.setOnClickListener(v -> onVideoClicked());
            Objects.requireNonNull(playerView.getVideoSurfaceView()).setOnClickListener(v -> onVideoClicked());
            buildMediaSource(Uri.parse(videoFilePath));
        } catch (Exception e) {
            AppUtils.printLogConsole("setDataInView", "Exception-------->" + e.getMessage());
        }
    }

    private void onVideoClicked() {
        try {
            long lastMinValue = 0;
            if (isVideoEnded) {
                seekTo(lastMinValue);
                videoPlayer.setPlayWhenReady(true);
                return;
            }
            seekTo(lastMinValue);
            videoPlayer.setPlayWhenReady(!videoPlayer.getPlayWhenReady());
        } catch (Exception e) {
            AppUtils.printLogConsole("onVideoClicked", "Exception-------->" + e.getMessage());
        }
    }

    private void seekTo(long sec) {
        if (videoPlayer != null)
            videoPlayer.seekTo(sec * 1000);
    }

    private void buildMediaSource(Uri mUri) {
        try {
            DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, getString(R.string.app_name));
            MediaSource mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory).createMediaSource(MediaItem.fromUri(mUri));
            videoPlayer.addMediaSource(mediaSource);
            videoPlayer.prepare();
            videoPlayer.setPlayWhenReady(true);
            videoPlayer.addListener(new Player.Listener() {
                @Override
                public void onPlaybackStateChanged(int playbackState) {
                    switch (playbackState) {
                        case Player.STATE_ENDED:
                            imagePlayPause.setVisibility(View.VISIBLE);
                            isVideoEnded = true;
                            break;
                        case Player.STATE_READY:
                            isVideoEnded = false;
                            break;
                        case Player.STATE_BUFFERING:
                        case Player.STATE_IDLE:
                        default:
                            break;
                    }
                }

                @Override
                public void onPlayWhenReadyChanged(boolean playWhenReady, int reason) {
                    imagePlayPause.setVisibility(playWhenReady ? View.GONE : View.VISIBLE);

                }
            });
        } catch (Exception e) {
            AppUtils.printLogConsole("buildMediaSource", "Exception-------->" + e.getMessage());
        }
    }
}
