package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.mpetkov.hiddengemsdeluxe.GameApp;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.badlogic.gdx.ApplicationListener;
import org.teavm.jso.browser.Window;

/** Launches the browser (TeaVM) application. */
public class HtmlLauncher {

    public static void main(String[] args) {
        Window window = Window.current();
        int startW = Math.max(1, window.getInnerWidth());
        int startH = Math.max(1, window.getInnerHeight());

        WebApplicationConfiguration config = new WebApplicationConfiguration("canvas");
        // Non-zero size disables TeaVM's built-in resize listener (we handle resize ourselves).
        config.width = startW;
        config.height = startH;
        config.showDownloadLogs = true;
        config.antialiasing = true;

        config.preloadListener = assetLoader -> assetLoader.loadScript("freetype.js");

        ApplicationListener game = new WebResizePollingListener(new GameApp());
        new WebApplication(game, config);
    }
}
