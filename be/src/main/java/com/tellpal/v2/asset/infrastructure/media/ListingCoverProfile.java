package com.tellpal.v2.asset.infrastructure.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Validated, versioned encoding policy captured in each listing-cover job. */
@Component
public record ListingCoverProfile(int phoneWidth,int phoneHeight,int tabletWidth,int tabletHeight,
        int quality,int maxSourceBytes,long maxPixels) {
    public ListingCoverProfile(@Value("${tellpal.asset.listing-cover.phone-width:360}") int phoneWidth,
            @Value("${tellpal.asset.listing-cover.phone-height:480}") int phoneHeight,
            @Value("${tellpal.asset.listing-cover.tablet-width:600}") int tabletWidth,
            @Value("${tellpal.asset.listing-cover.tablet-height:800}") int tabletHeight,
            @Value("${tellpal.asset.listing-cover.jpeg-quality-percent:85}") int quality,
            @Value("${tellpal.asset.listing-cover.max-source-bytes:20971520}") int maxSourceBytes,
            @Value("${tellpal.asset.listing-cover.max-pixels:40000000}") long maxPixels) {
        if(phoneWidth<=0 || phoneHeight<=0 || tabletWidth<=0 || tabletHeight<=0 || quality<1 || quality>100
                || maxSourceBytes<=0 || maxSourceBytes==Integer.MAX_VALUE || maxPixels<=0)
            throw new IllegalArgumentException("Listing cover profile limits are invalid");
        this.phoneWidth=phoneWidth;this.phoneHeight=phoneHeight;this.tabletWidth=tabletWidth;this.tabletHeight=tabletHeight;
        this.quality=quality;this.maxSourceBytes=maxSourceBytes;this.maxPixels=maxPixels;
    }
    public String version() {return "listing-jpeg-v1-%dx%d-%dx%d-q%d-b%d-p%d-white".formatted(phoneWidth,phoneHeight,tabletWidth,tabletHeight,quality,maxSourceBytes,maxPixels);}
    public static ListingCoverProfile parse(String captured) {
        var m=java.util.regex.Pattern.compile("listing-jpeg-v1-(\\d+)x(\\d+)-(\\d+)x(\\d+)-q(\\d+)-b(\\d+)-p(\\d+)-white").matcher(captured==null?"":captured);
        if(!m.matches()) throw new IllegalArgumentException("Captured listing profile is unsupported");
        return new ListingCoverProfile(Integer.parseInt(m.group(1)),Integer.parseInt(m.group(2)),Integer.parseInt(m.group(3)),
                Integer.parseInt(m.group(4)),Integer.parseInt(m.group(5)),Integer.parseInt(m.group(6)),Long.parseLong(m.group(7)));
    }
}
