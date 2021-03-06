package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.ENode;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.scheduling.IScheduleService;
import com.qianzhui.enode.infrastructure.*;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

/**
 * Created by junbo_xu on 2016/3/19.
 */
public class DefaultMessageProcessor<X extends IProcessingMessage<X, Y>, Y extends IMessage> implements IMessageProcessor<X, Y> {

    private final ILogger _logger;
    private ConcurrentMap<String, ProcessingMessageMailbox<X, Y>> _mailboxDict;
    private IProcessingMessageScheduler<X, Y> _processingMessageScheduler;
    private IProcessingMessageHandler<X, Y> _processingMessageHandler;
    private final IScheduleService _scheduleService;
    private final int _timeoutSeconds;
    private final String _taskName;

    @Inject
    public DefaultMessageProcessor(IProcessingMessageScheduler<X, Y> processingMessageScheduler, IProcessingMessageHandler<X, Y> processingMessageHandler, ILoggerFactory loggerFactory) {
        _mailboxDict = new ConcurrentHashMap<>();
        _processingMessageScheduler = processingMessageScheduler;
        _processingMessageHandler = processingMessageHandler;
        _logger = loggerFactory.create(getClass());
        _scheduleService = ObjectContainer.resolve(IScheduleService.class);
        _timeoutSeconds = ENode.getInstance().getSetting().getAggregateRootMaxInactiveSeconds();
        _taskName = "CleanInactiveAggregates_" + System.nanoTime() + new Random().nextInt(10000);
    }

    public String getMessageName() {
        return "message";
    }

    @Override
    public void process(X processingMessage) {
        String routingKey = processingMessage.getMessage().getRoutingKey();
        if (routingKey != null && !routingKey.trim().equals("")) {
//            ProcessingMessageMailbox<X, Y, Z> mailbox = _mailboxDict.putIfAbsent(routingKey, new ProcessingMessageMailbox<>(_processingMessageScheduler, _processingMessageHandler));
            ProcessingMessageMailbox<X, Y> mailbox = _mailboxDict.computeIfAbsent(routingKey, key -> new ProcessingMessageMailbox<>(routingKey, _processingMessageScheduler, _processingMessageHandler, _logger));
            mailbox.enqueueMessage(processingMessage);
        } else {
            _processingMessageScheduler.scheduleMessage(processingMessage);
        }
    }

    @Override
    public void start() {
        _scheduleService.startTask(_taskName, this::cleanInactiveMailbox, ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds(), ENode.getInstance().getSetting().getScanExpiredAggregateIntervalMilliseconds());
    }

    @Override
    public void stop() {
        _scheduleService.stopTask(_taskName);
    }

    private void cleanInactiveMailbox() {
        List<Map.Entry<String, ProcessingMessageMailbox<X, Y>>> inactiveList = _mailboxDict.entrySet().stream().filter(entry ->
                entry.getValue().isInactive(_timeoutSeconds) && !entry.getValue().isRunning()
        ).collect(Collectors.toList());

        inactiveList.stream().forEach(entry -> {
            if (_mailboxDict.remove(entry.getKey()) != null) {
                _logger.info("Removed inactive %s mailbox, aggregateRootId: {1}", getMessageName(), entry.getKey());
            }
        });
    }
}
