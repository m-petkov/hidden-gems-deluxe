package com.github.mpetkov.hiddengemsdeluxe.html;

import com.github.mpetkov.hiddengemsdeluxe.util.MobileWebLayout;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

/**
 * Syncs the TeaVM canvas buffer with the browser viewport.
 * Game layout refresh is handled centrally by {@link com.github.mpetkov.hiddengemsdeluxe.GameApp}.
 */
public class WebCanvasSyncListener implements ApplicationListener {

    private final ApplicationListener delegate;

    public WebCanvasSyncListener(ApplicationListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void create() {
        Window.current().addEventListener("resize", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
                if (Gdx.app != null) {
                    Gdx.app.postRunnable(WebCanvasSyncListener.this::syncCanvas);
                }
            }
        });
        syncCanvas();
        delegate.create();
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void render() {
        syncCanvas();
        delegate.render();
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
        syncCanvas();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    private void syncCanvas() {
        int cssW = WebCanvasSync.viewportWidth();
        int cssH = WebCanvasSync.viewportHeight();
        float dpr = WebCanvasSync.devicePixelRatio();
        MobileWebLayout.setCssSize(cssW, cssH);
        MobileWebLayout.setPixelRatio(dpr);

        int bufferW = Math.max(1, Math.round(cssW * dpr));
        int bufferH = Math.max(1, Math.round(cssH * dpr));

        WebCanvasSync.resizeCanvasBuffer(cssW, cssH, bufferW, bufferH);

        if (bufferW <= 0 || bufferH <= 0) {
            return;
        }
        if (Gdx.graphics.getWidth() == bufferW && Gdx.graphics.getHeight() == bufferH) {
            return;
        }
        Gdx.graphics.setWindowedMode(bufferW, bufferH);
    }
}
