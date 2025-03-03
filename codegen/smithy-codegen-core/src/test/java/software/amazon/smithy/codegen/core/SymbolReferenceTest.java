package software.amazon.smithy.codegen.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;
import software.amazon.smithy.utils.MapUtils;

public class SymbolReferenceTest {
    @Test
    public void addsDefaultOptions() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref = new SymbolReference(symbol);

        assertThat(ref.hasOption(SymbolReference.ContextOption.USE), is(true));
        assertThat(ref.hasOption(SymbolReference.ContextOption.DECLARE), is(true));
    }

    @Test
    public void propertiesAndOptionsAreUsedInEquals() {
        Symbol symbol = Symbol.builder().name("foo").build();
        SymbolReference ref1 = new SymbolReference(symbol);
        SymbolReference ref2 = new SymbolReference(
                symbol, MapUtils.of("foo", true), SymbolReference.ContextOption.USE);
        SymbolReference ref3 = new SymbolReference(symbol, SymbolReference.ContextOption.USE);

        assertThat(ref1, equalTo(ref1));
        assertThat(ref1, not(equalTo(ref2)));
        assertThat(ref1, not(equalTo(ref3)));
        assertThat(ref2, not(equalTo(ref3)));
    }
}
