package me.sathish.diagram_maker.diagram;

public enum ImageFormat {
    PNG("image/png"),
    SVG("image/svg+xml"),
    JPG("image/jpeg");

    private final String contentType;

    ImageFormat(final String contentType) {
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

}
