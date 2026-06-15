package com.github.mpetkov.hiddengemsdeluxe.html;

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.github.xpenatan.gdx.teavm.backends.web.CanvasResizer;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplication;
import com.github.xpenatan.gdx.teavm.backends.web.WebApplicationConfiguration;
import com.github.xpenatan.gdx.teavm.backends.web.WebGraphics;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.events.Event;
import org.teavm.jso.dom.events.EventListener;

/**
 * Keeps the canvas drawing buffer in sync with the browser viewport and forwards resize()
 * to the game so layouts are recalculated instead of stretched.
 */
public class WebResizePollingListener implements ApplicationListener {

    private final ApplicationListener delegate;
    private int lastClientW = -1;
    private int lastClientH = -1;

    public WebResizePollingListener(ApplicationListener delegate) {
        this.delegate = delegate;
    }

    @Override
    public void create() {
        Window.current().addEventListener("resize", new EventListener<Event>() {
            @Override
            public void handleEvent(Event event) {
                lastClientW = -1;
                lastClientH = -1;
            }
        });
        delegate.create();
        syncCanvasIfNeeded();
    }

    @Override
    public void resize(int width, int height) {
        delegate.resize(width, height);
    }

    @Override
    public void render() {
        syncCanvasIfNeeded();
        delegate.render();
    }

    @Override
    public void pause() {
        delegate.pause();
    }

    @Override
    public void resume() {
        delegate.resume();
        syncCanvasIfNeeded();
    }

    @Override
    public void dispose() {
        delegate.dispose();
    }

    private void syncCanvasIfNeeded() {
        if (!(Gdx.app instanceof WebApplication)) {
            return;
        }

        int clientW = Window.current().getInnerWidth();
        int clientH = Window.current().getInnerHeight();
        if (clientW <= 0 || clientH <= 0) {
            return;
        }
        if (clientW == lastClientW && clientH == lastClientH) {
            return;
        }

        lastClientW = clientW;
        lastClientH = clientH;

        WebApplication app = (WebApplication) Gdx.app;
        WebApplicationConfiguration config = app.getConfig();
        WebGraphics graphics = (WebGraphics) app.getGraphics();

        CanvasResizer.apply(graphics, config, clientW, clientH);

        int w = Gdx.graphics.getWidth();
        int h = Gdx.graphics.getHeight();
        Gdx.gl.glViewport(0, 0, w, h);
        delegate.resize(w, h);
    }
}
