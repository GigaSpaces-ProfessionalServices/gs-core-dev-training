package com.gs;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.*;
import java.util.stream.Collectors;

import com.gigaspaces.CommonSystemProperties;
import com.gigaspaces.annotation.SupportCodeChange;
import com.gigaspaces.internal.io.IOUtils;
import com.gigaspaces.internal.metadata.ITypeDesc;
import com.gigaspaces.internal.metadata.PropertyInfo;
import com.gigaspaces.internal.query.IQueryIndexScanner;
import com.gigaspaces.internal.query.QueryIndexes;
import com.gigaspaces.internal.query.RawEntry;
import com.gigaspaces.internal.query.RawEntryConverter;
import com.gigaspaces.internal.server.storage.EntryHolder;
import com.gigaspaces.internal.server.storage.IEntryData;
import com.gigaspaces.internal.server.storage.IEntryHolder;
import com.gigaspaces.internal.utils.GsEnv;
import com.gigaspaces.metadata.index.SpaceIndex;
import com.gigaspaces.metadata.index.SpaceIndexType;
import com.gigaspaces.query.aggregators.*;
import com.j_spaces.core.ExternalEntryPacket;
import com.j_spaces.core.cache.ExtendedIndexHandler;
import com.j_spaces.core.cache.IEntryCacheInfo;
import com.j_spaces.core.cache.TypeData;
import com.j_spaces.core.cache.TypeDataIndex;
import com.j_spaces.kernel.IStoredList;
import com.j_spaces.kernel.IStoredListIterator;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.aggregators.GigaSpaceAggregation;

@SupportCodeChange(id="1")
public class CustomINAggregator extends AbstractPathAggregator<ArrayList<Object>> implements Externalizable {

   /*injection of local space instance if needed
    @GigaSpaceAggregation
    protected transient GigaSpace gs;*/



    protected Collection<Object> collection;
    protected ArrayList<Object> result = new ArrayList<>();


    @Override
    public String getDefaultAlias() {
        return "IN";
    }

    @Override
    public String getName() {return "IN"; }

    public CustomINAggregator() {
        super();
    }


    public CustomINAggregator(String propertyName, Collection<Object> collection) {
        super();
        setPath(propertyName);
        this.collection = collection;
    }


    /*
    The aggregate method is executed for each space entity matching the SQLQuery in a space partition. The function receives a SpaceEntriesAggregatorContext, which is a wrapper that allows the user to access members of the user entity.
    The members of each space entity can be accessed by the getPathValue method of SpaceEntriesAggregatorContext.
 */
    @Override
    public void aggregate(SpaceEntriesAggregatorContext context) {
        if (collection.contains(context.getPathValue(getPath()))) {
           result.add(context.getRawEntry());
        }
    }

    /*
    Executed after all aggregate method calls have completed, the getIntermediateResult method represents the aggregation result of one partition.
    The returned value will be passed back to the client where it will trigger the aggregateIntermediateResult method.
     */
    @Override
    public ArrayList<Object> getIntermediateResult() {
        return result;
    }


    /*
    The aggregateIntermediateResult assembles the responses from each partition on the client side to represent a response from the entire cluster.
    The input to this method is the returned value of the getIntermediateResult method.
     */
    @Override
    public void aggregateIntermediateResult(ArrayList<Object> partitionResult) {
        if (partitionResult != null)
            result.addAll(partitionResult);
    }


    /*
    The skipProcessedUidStore method decide if to keep all uids while iterating over the objects in order to check if same object was delated and created again while the scan
    true = skip storage of uids.
     */
    @Override
    public boolean skipProcessedUidStore() {
        return Boolean.parseBoolean(GsEnv.property(CommonSystemProperties.AGGREGATION_IGNORE_PROCESSED_UIDS).get("true"));
    }

    /*
    The method skipFullScanSupported checks if indexes can be used to optimize the aggrgation and skip aggrgate per each object
     */
    @Override
    public boolean skipFullScanSupported(Map<String, SpaceIndex> indexes, boolean isMemoryScan) {
        SpaceIndex index = indexes.get(getPath());
        return (getFunctionCallColumn() == null && index != null  && (index.getIndexType() == SpaceIndexType.EQUAL || index.getIndexType() == SpaceIndexType.EQUAL_AND_ORDERED));
    }

    @Override
    public void handleIntermediateResultOfMemoryStorage(TypeDataIndex index, TypeData entryType) {

        ITypeDesc typeDesc = entryType.getCacheManager().getTypeManager().getTypeDesc(entryType.getClassName());
        System.out.println("index is used");
        collection.stream().forEach(value -> addIndexEntries(index, typeDesc, value));

    }



    protected void addIndexEntries(TypeDataIndex index, ITypeDesc typeDesc, Object value){

        IStoredListIterator<IEntryCacheInfo> slh = null;
        try {
            IStoredList<IEntryCacheInfo> sl = index.getIndexEntries(value);
            if (sl == null) return;
            for (slh = sl.establishListScan(false); slh != null; slh = sl.next(slh)) {
                IEntryCacheInfo subject = slh.getSubject();
                if (subject != null)
                    result.add(createEntryPacket(subject,subject.getEntryHolder(index.getCacheManager()).getEntryData()));
            }
        } finally {
            if (slh != null)
                slh.release();
        }

    }

    private ExternalEntryPacket createEntryPacket(IEntryCacheInfo info, IEntryData entryData) {
        ITypeDesc typeDesc = info.getServerTypeDesc().getTypeDesc();
        return new ExternalEntryPacket(typeDesc, typeDesc.getObjectType(),
                entryData.getFixedPropertiesValues(), info.getUID(), info.getVersion(), info.getExpirationTime(), false);
    }


    @Override
    public ArrayList<Object> getFinalResult() {
        if (result == null) {
            return new ArrayList<Object>();
        }

        ArrayList<Object> finalResults = new ArrayList<Object>();

        finalResults.addAll(result.stream().map(rawEntry -> (Object) toObject((RawEntry) rawEntry)).collect(Collectors.toList()));


        return finalResults;
    }


   protected IStoredListIterator<IEntryCacheInfo> getIndexValueIterator(TypeDataIndex index, Object value) {
        if (index.getIndexEntries(value)!= null)
            return index.getIndexEntries(value).establishListScan(true);
        return null;


    }







    public void readExternal(ObjectInput in) {
        try {
            super.readExternal(in);
            collection = (Collection<Object>) in.readObject();

        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
        }
    }



    public void writeExternal(ObjectOutput out) {
        try {
            super.writeExternal(out);
            out.writeObject(collection);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}