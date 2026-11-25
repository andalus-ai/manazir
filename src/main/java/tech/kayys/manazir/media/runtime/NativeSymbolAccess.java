package tech.kayys.manazir.media.runtime;

/** Minimal symbol access boundary used by the FFmpeg inspector. */
public interface NativeSymbolAccess {
    boolean hasSymbol(String symbolName);

    int invokeInt(String symbolName);
}
