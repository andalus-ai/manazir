package tech.kayys.manazir.media.runtime;

import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.util.Objects;

import static java.lang.foreign.ValueLayout.JAVA_INT;

/**
 * Java 22 FFM-backed symbol access for libraries already loaded into the
 * process class loader. Call only after NativeRuntime initialization.
 */
public final class FfmNativeSymbolAccess implements NativeSymbolAccess {
    private final Linker linker;
    private final SymbolLookup symbols;

    public FfmNativeSymbolAccess() {
        this(Linker.nativeLinker(), SymbolLookup.loaderLookup());
    }

    FfmNativeSymbolAccess(Linker linker, SymbolLookup symbols) {
        this.linker = Objects.requireNonNull(linker, "linker");
        this.symbols = Objects.requireNonNull(symbols, "symbols");
    }

    @Override
    public boolean hasSymbol(String symbolName) {
        return symbols.find(Objects.requireNonNull(symbolName, "symbolName")).isPresent();
    }

    @Override
    public int invokeInt(String symbolName) {
        MemorySegment address = symbols.find(Objects.requireNonNull(symbolName, "symbolName"))
                .orElseThrow(() -> new NativeSymbolAccessException("Missing native symbol: " + symbolName));
        MethodHandle handle = linker.downcallHandle(address, FunctionDescriptor.of(JAVA_INT));
        try {
            return (int) handle.invokeExact();
        } catch (Throwable failure) {
            throw new NativeSymbolAccessException("Failed invoking native symbol: " + symbolName, failure);
        }
    }
}
