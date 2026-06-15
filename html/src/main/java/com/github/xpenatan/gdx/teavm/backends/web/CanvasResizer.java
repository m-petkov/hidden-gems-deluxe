package com.github.xpenatan.gdx.teavm.backends.web;

import org.teavm.jso.dom.css.CSSStyleDeclaration;

/**
 * Resizes the WebGL drawing buffer to match the browser viewport.
 * CSS width/height must NOT be set — that stretches the bitmap and squishes the UI.
 */
public final class CanvasResizer {

    private CanvasResizer() {
    }

    public static void apply(WebGraphics graphics, WebApplicationConfiguration config, int clientWidth, int clientHeight) {
        int width = clientWidth - config.padHorizontal;
        int height = clientHeight - config.padVertical;
        if (width <= 0 || height <= 0) {
            return;
        }

        CSSStyleDeclaration style = graphics.canvas.getStyle();
        style.removeProperty("width");
        style.removeProperty("height");

        graphics.setCanvasSize(width, height, false);
    }
}
