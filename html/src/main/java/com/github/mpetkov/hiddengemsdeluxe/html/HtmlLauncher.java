package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;

/** Launches the browser (TeaVM) application. */
public class HtmlLauncher {

    public static void main(String[] args) {
        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        config.width = 0;
        config.height = 0;
        config.usePhysicalPixels = true;
        config.showDownloadLogs = true;
        config.antialiasing = true;

        config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");

        new WebApplication(new GameApp(), config);
    }
}
