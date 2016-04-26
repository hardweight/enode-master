package com.qianzhui.enode.infrastructure.impl.inmemory;

import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.common.io.AsyncTaskStatus;
import com.qianzhui.enode.infrastructure.IPublishedVersionStore;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Created by junbo_xu on 2016/4/2.
 */
public class InMemoryPublishedVersionStore implements IPublishedVersionStore {
    private final CompletableFuture<AsyncTaskResult> _successTask = CompletableFuture.completedFuture(AsyncTaskResult.Success);

    private final ConcurrentMap<String, Integer> _versionDict = new ConcurrentHashMap<>();

    @Override
    public CompletableFuture<AsyncTaskResult> updatePublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId, int publishedVersion) {
        _versionDict.put(buildKey(processorName, aggregateRootId), publishedVersion);
        return _successTask;
    }

    @Override
    public CompletableFuture<AsyncTaskResult<Integer>> getPublishedVersionAsync(String processorName, String aggregateRootTypeName, String aggregateRootId) {
        Integer version = _versionDict.get(buildKey(processorName, aggregateRootId));
        int publishedVersion = version == null ? 0 : version;
        return CompletableFuture.completedFuture(new AsyncTaskResult<>(AsyncTaskStatus.Success, publishedVersion));
    }

    private String buildKey(String eventProcessorName, String aggregateRootId) {
        return String.format("{0}-{1}", eventProcessorName, aggregateRootId);
    }
}
