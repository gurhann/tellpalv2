package com.tellpal.v2.asset.infrastructure.media.reading;

import com.tellpal.v2.asset.api.ReadingImagePreparationApi.TestProfile;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Ham metadata korunur; yalnız EXIF taşıma öneki TIFF karşılaştırmasından ayrılır. */
record ReadingPreservationMetadata(byte[] icc, byte[] xmp, byte[] tiff) {
    static byte[] tiff(byte[] bytes) throws IOException {
        if (bytes == null) return null;
        byte[] prefix = {'E', 'x', 'i', 'f', 0, 0};
        byte[] data = starts(bytes, prefix) ? Arrays.copyOfRange(bytes, 6, bytes.length) : bytes;
        ReadingPreservation.require(data.length >= 8 && data.length <= ReadingPreservation.METADATA, "TIFF_SIZE");
        boolean little = data[0] == 'I' && data[1] == 'I';
        ReadingPreservation.require(little || data[0] == 'M' && data[1] == 'M', "TIFF_ENDIAN");
        var view = ByteBuffer.wrap(data).order(little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        ReadingPreservation.require(Short.toUnsignedInt(view.getShort(2)) == 42, "TIFF_MAGIC");
        inspectIfd(view, Integer.toUnsignedLong(view.getInt(4)), new HashSet<>());
        return data;
    }

    private static void inspectIfd(ByteBuffer data, long address, HashSet<Long> seen) throws IOException {
        if (address == 0) return;
        ReadingPreservation.require(seen.size() < 32 && seen.add(address) && address >= 8
                && address + 2 <= data.limit(), "TIFF_IFD_BOUNDS_OR_CYCLE");
        int offset = (int) address, count = Short.toUnsignedInt(data.getShort(offset));
        ReadingPreservation.require(address + 2L + 12L * count + 4 <= data.limit(), "TIFF_IFD_LENGTH");
        int[] lengths = {0, 1, 1, 2, 4, 8, 1, 1, 2, 4, 8, 4, 8};
        for (int i = 0; i < count; i++) {
            int at = offset + 2 + i * 12;
            int tag = Short.toUnsignedInt(data.getShort(at));
            ReadingPreservation.require(tag != 274, "ORIENTATION_REFUSED");
            ReadingPreservation.require(tag != 330, "TIFF_SUBIFD_REFUSED");
            int type = Short.toUnsignedInt(data.getShort(at + 2));
            long n = Integer.toUnsignedLong(data.getInt(at + 4));
            ReadingPreservation.require(type > 0 && type < lengths.length, "TIFF_TYPE");
            long size = n * lengths[type], value = Integer.toUnsignedLong(data.getInt(at + 8));
            ReadingPreservation.require(size <= ReadingPreservation.METADATA && (size <= 4 || value >= 8
                    && value + size <= data.limit()), "TIFF_VALUE_BOUNDS");
            if (tag == 34665 || tag == 34853 || tag == 40965) {
                ReadingPreservation.require(type == 4 && n == 1, "TIFF_POINTER");
                inspectIfd(data, value, seen);
            }
        }
        inspectIfd(data, Integer.toUnsignedLong(data.getInt(offset + 2 + count * 12)), seen);
    }

    void validate() throws IOException {
        long size = (icc == null ? 0 : icc.length) + (xmp == null ? 0 : xmp.length)
                + (tiff == null ? 0 : tiff.length);
        ReadingPreservation.require(size <= ReadingPreservation.METADATA, "METADATA_LIMIT");
        if (xmp != null) validateXmp(xmp);
        if (icc != null) ReadingPreservation.require(icc.length == 536 && ReadingPreservation.sha(icc).equals(ReadingPreservation.ICC_SHA), "ICC_REFUSED");
    }

    private static void validateXmp(byte[] bytes) throws IOException {
        try {
            var factory = javax.xml.parsers.SAXParserFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            var parser = factory.newSAXParser();
            parser.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
            parser.setProperty(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            parser.parse(new java.io.ByteArrayInputStream(bytes), new org.xml.sax.helpers.DefaultHandler() {
                private int depth;
                private void orientation(String uri, String local) throws org.xml.sax.SAXException {
                    if (uri.equals("http://ns.adobe.com/tiff/1.0/") && local.equals("Orientation"))
                        throw new org.xml.sax.SAXException("ORIENTATION_REFUSED");
                }
                @Override public void startElement(String uri, String local, String qualified, org.xml.sax.Attributes attributes) throws org.xml.sax.SAXException {
                    if (++depth > 256) throw new org.xml.sax.SAXException("XMP_DEPTH_LIMIT");
                    orientation(uri, local);
                    for (int i = 0; i < attributes.getLength(); i++) orientation(attributes.getURI(i), attributes.getLocalName(i));
                }
                @Override public void endElement(String uri, String local, String qualified) { depth--; }
                @Override public void error(org.xml.sax.SAXParseException failure) throws org.xml.sax.SAXException { throw failure; }
                @Override public void fatalError(org.xml.sax.SAXParseException failure) throws org.xml.sax.SAXException { throw failure; }
            });
        } catch (org.xml.sax.SAXException e) {
            throw new IOException(e.getMessage().equals("ORIENTATION_REFUSED") ? "ORIENTATION_REFUSED" : "XMP_XML_REFUSED", e);
        } catch (javax.xml.parsers.ParserConfigurationException e) { throw new IOException("XMP_SECURE_PARSER_UNAVAILABLE", e); }
    }

    void same(ReadingPreservationMetadata other) throws IOException {
        ReadingPreservation.require(Arrays.equals(icc, other.icc), "ICC_MISMATCH");
        ReadingPreservation.require(Arrays.equals(xmp, other.xmp), "XMP_MISMATCH");
        ReadingPreservation.require(Arrays.equals(tiff, other.tiff), "TIFF_MISMATCH");
    }

    Map<String, Object> summary() {
        var result = new LinkedHashMap<String, Object>();
        result.put("icc", summary(icc)); result.put("xmp", summary(xmp)); result.put("normalizedTiff", summary(tiff));
        return result;
    }

    private static Map<String, Object> summary(byte[] bytes) {
        return bytes == null ? Map.of("present", false, "bytes", 0)
                : Map.of("present", true, "bytes", bytes.length, "sha256", ReadingPreservation.sha(bytes));
    }

    static boolean starts(byte[] bytes, byte[] prefix) {
        return bytes.length >= prefix.length && Arrays.equals(bytes, 0, prefix.length, prefix, 0, prefix.length);
    }
}
