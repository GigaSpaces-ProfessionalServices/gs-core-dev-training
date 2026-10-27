package com.gs.offheap.filter;

import com.j_spaces.core.IJSpace;
import com.j_spaces.core.SpaceContext;
import com.j_spaces.core.filters.ISpaceFilter;
import com.j_spaces.core.filters.entry.ISpaceFilterEntry;

import java.util.logging.Logger;

/**
 * An after-read space filter that looks at every payment a batch read returns, and reads its
 * currency by name. Filters like this are written for auditing, or to mask or replace values on
 * their way out.
 * <p>
 * By contract a filter is handed the entry as a whole: the space calls it before it applies the
 * query projection. That is why the currency is there even when the query did not project it - and
 * why a batch read with a projection is not served straight from the off-heap block on this space:
 * a packet built from the block holds the projected properties only, and the filter would see null.
 * <p>
 * Every call is written to the container log, so you can see what the filter was given.
 */
public class CurrencyAuditFilter implements ISpaceFilter {

    private static final Logger LOGGER = Logger.getLogger(CurrencyAuditFilter.class.getName());

    @Override
    public void init(IJSpace space, String filterId, String url, int priority) {
    }

    @Override
    public void process(SpaceContext context, ISpaceFilterEntry entry, int operationCode) {
        if (entry == null)
            return;
        LOGGER.info("CurrencyAuditFilter saw payment id=" + entry.getFieldValue("id")
                + " currency=" + entry.getFieldValue("currency"));
    }

    @Override
    public void process(SpaceContext context, ISpaceFilterEntry[] entries, int operationCode) {
        if (entries == null)
            return;
        for (ISpaceFilterEntry entry : entries)
            process(context, entry, operationCode);
    }

    @Override
    public void close() {
    }
}
