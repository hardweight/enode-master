package com.qianzhui.enode.infrastructure.impl;

import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.IMessage;
import com.qianzhui.enode.infrastructure.IMessageHandler;
import com.qianzhui.enode.infrastructure.IMessageHandlerProxy3;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class MessageHandlerProxy3 implements IMessageHandlerProxy3 {
    private Class _handlerType;
    private IMessageHandler _handler;
    private MethodHandle _methodHandle;
    private Method _method;
    private Class<?>[] _methodParameterTypes;

    public MessageHandlerProxy3(Class handlerType, IMessageHandler handler, MethodHandle methodHandle, Method method) {
        _handlerType = handlerType;
        _handler = handler;
        _methodHandle = methodHandle;
        _method = method;
        _methodParameterTypes = method.getParameterTypes();
    }

    @Override
    public CompletableFuture<AsyncTaskResult> handleAsync(IMessage message1, IMessage message2, IMessage message3) {
        IMessageHandler handler = (IMessageHandler) getInnerObject();

        List<Class<?>> parameterTypes = Arrays.asList(_methodParameterTypes);

        List<IMessage> params = new ArrayList<>();
        params.add(message1);
        params.add(message2);
        params.add(message3);

        //排序参数
        params.sort((m1, m2) ->
                getMessageParameterIndex(parameterTypes, m1) - getMessageParameterIndex(parameterTypes, m2)
        );

        try {
            //参数按照方法定义参数类型列表传递
            return (CompletableFuture<AsyncTaskResult>) _methodHandle.invoke(handler, params.get(0), params.get(1), params.get(2));
        } catch (Throwable throwable) {
            throw new RuntimeException(throwable);
        }
    }

    private int getMessageParameterIndex(List<Class<?>> methodParameterTypes, IMessage message) {
        int i = 0;
        for (Class paramType : methodParameterTypes) {
            if (paramType.isAssignableFrom(message.getClass())) {
                return i;
            }
            i++;
        }

        return i;
    }

    @Override
    public Object getInnerObject() {
        if (_handler != null)
            return _handler;

        return ObjectContainer.resolve(_handlerType);
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
