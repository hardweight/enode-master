package com.qianzhui.enode.rocketmq.applicationmessage;

import com.alibaba.rocketmq.common.message.MessageExt;
import com.qianzhui.enode.common.container.GenericTypeLiteral;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.logging.ILogger;
import com.qianzhui.enode.common.logging.ILoggerFactory;
import com.qianzhui.enode.common.rocketmq.consumer.listener.CompletableConsumeConcurrentlyContext;
import com.qianzhui.enode.common.serializing.IJsonSerializer;
import com.qianzhui.enode.common.utilities.BitConverter;
import com.qianzhui.enode.infrastructure.IApplicationMessage;
import com.qianzhui.enode.infrastructure.IMessageProcessor;
import com.qianzhui.enode.infrastructure.ITypeNameProvider;
import com.qianzhui.enode.infrastructure.ProcessingApplicationMessage;
import com.qianzhui.enode.rocketmq.*;

import javax.inject.Inject;

/**
 * Created by junbo_xu on 2016/4/6.
 */
public class ApplicationMessageConsumer {

    private final RocketMQConsumer _consumer;
    private final IJsonSerializer _jsonSerializer;
    private final ITopicProvider<IApplicationMessage> _messageTopicProvider;
    private final ITypeNameProvider _typeNameProvider;
    private final IMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> _processor;
    private final ILogger _logger;

    @Inject
    public ApplicationMessageConsumer(RocketMQConsumer consumer, IJsonSerializer jsonSerializer,
                                      ITopicProvider<IApplicationMessage> messageITopicProvider, ITypeNameProvider typeNameProvider,
                                      IMessageProcessor<ProcessingApplicationMessage, IApplicationMessage> processor,
                                      ILoggerFactory loggerFactory) {
        _consumer = consumer;
        _jsonSerializer = jsonSerializer;
        _messageTopicProvider = messageITopicProvider;
        _typeNameProvider = typeNameProvider;
        _processor = processor;
        _logger = loggerFactory.create(getClass());
    }

    public ApplicationMessageConsumer(RocketMQConsumer consumer) {
        _consumer = consumer;
        _jsonSerializer = ObjectContainer.resolve(IJsonSerializer.class);
        _messageTopicProvider = ObjectContainer.resolve(new GenericTypeLiteral<ITopicProvider<IApplicationMessage>>() {
        });
        _processor = ObjectContainer.resolve(new GenericTypeLiteral<IMessageProcessor<ProcessingApplicationMessage, IApplicationMessage>>() {
        });
        _typeNameProvider = ObjectContainer.resolve(ITypeNameProvider.class);
        _logger = ObjectContainer.resolve(ILoggerFactory.class).create(getClass());
    }

    public ApplicationMessageConsumer start() {
        _consumer.registerMessageHandler(new RocketMQMessageHandler() {
            @Override
            public boolean isMatched(TopicTagData topicTagData) {
                return _messageTopicProvider.getAllSubscribeTopics().contains(topicTagData);
            }

            @Override
            public void handle(MessageExt message, CompletableConsumeConcurrentlyContext context) {
                ApplicationMessageConsumer.this.handle(message, context);
            }
        });
        return this;
    }

    public ApplicationMessageConsumer shutdown() {
        return this;
    }

    void handle(final MessageExt msg, final CompletableConsumeConcurrentlyContext context) {
        ApplicationDataMessage appDataMessage = _jsonSerializer.deserialize(BitConverter.toString(msg.getBody()), ApplicationDataMessage.class);
        Class applicationMessageType;

        try {
            applicationMessageType = _typeNameProvider.getType(appDataMessage.getApplicationMessageType());
        } catch (Exception e) {
            _logger.warn("Consume applicatio message exception:", e);
            context.onMessageHandled();
            return;
        }

        IApplicationMessage message = (IApplicationMessage) _jsonSerializer.deserialize(appDataMessage.getApplicationMessageData(), applicationMessageType);
        RocketMQProcessContext processContext = new RocketMQProcessContext(msg, context);
        ProcessingApplicationMessage processingMessage = new ProcessingApplicationMessage(message, processContext);
        _logger.info("ENode application message received, messageId: %s, routingKey: %s", message.id(), message.getRoutingKey());
        _processor.process(processingMessage);
//        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    }

    public RocketMQConsumer getConsumer() {
        return _consumer;
    }
}
