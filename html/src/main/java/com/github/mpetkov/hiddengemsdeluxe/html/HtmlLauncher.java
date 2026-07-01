package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.badlogic.gdx.ApplicationListener;

/** Launches the browser (TeaVM) application. */
public class HtmlLauncher {

    public static void main(String[] args) {
        int startW = Math.max(1, WebCanvasSync.viewportWidth());
        int startH = Math.max(1, WebCanvasSync.viewportHeight());
        float dpr = WebCanvasSync.devicePixelRatio();

        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = Math.max(1, Math.round(startW * dpr));
        config.height = Math.max(1, Math.round(startH * dpr));
        WebCanvasSync.resizeCanvasBuffer(startW, startH, config.width, config.height);
        config.showDownloadLogs = true;
        config.antialiasing = true;

        config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");

        ApplicationListener game = new WebCanvasSyncListener(new GameApp());
        new WebApplication(game, config);
    }
}
