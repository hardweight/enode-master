package com.qianzhui.enode.commanding.impl;

import com.qianzhui.enode.commanding.ICommand;
import com.qianzhui.enode.commanding.ICommandAsyncHandler;
import com.qianzhui.enode.commanding.ICommandAsyncHandlerProxy;
import com.qianzhui.enode.common.container.ObjectContainer;
import com.qianzhui.enode.common.io.AsyncTaskResult;
import com.qianzhui.enode.infrastructure.Handled;
import com.qianzhui.enode.infrastructure.IApplicationMessage;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

/**
 * Created by junbo_xu on 2016/3/31.
 */
public class CommandAsyncHandlerProxy implements ICommandAsyncHandlerProxy {
    private Class _commandHandlerType;
    private ICommandAsyncHandler _commandHandler;
    private MethodHandle _methodHandle;
    private Method _method;
    private boolean _isCheckCommandHandledFirst;

    public CommandAsyncHandlerProxy(Class commandHandlerType, ICommandAsyncHandler commandHandler, MethodHandle methodHandle, Method method) {
        _commandHandlerType = commandHandlerType;
        _commandHandler = commandHandler;
        _methodHandle = methodHandle;
        _method = method;
        _isCheckCommandHandledFirst = parseCheckCommandHandledFirst();
    }

    @Override
    public CompletableFuture<AsyncTaskResult<IApplicationMessage>> handleAsync(ICommand command) {
        ICommandAsyncHandler handler = (ICommandAsyncHandler) getInnerObject();
        try {
            return (CompletableFuture<AsyncTaskResult<IApplicationMessage>>) _methodHandle.invoke(handler, command);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean checkCommandHandledFirst() {
        return _isCheckCommandHandledFirst;
    }

    @Override
    public Object getInnerObject() {
        if (_commandHandler != null)
            return _commandHandler;

        return ObjectContainer.resolve(_commandHandlerType);
    }

    private boolean parseCheckCommandHandledFirst() {
        Handled handled = _method.getAnnotation(Handled.class);

        if (handled != null)
            return handled.value();

        handled = _commandHandler.getClass().getAnnotation(Handled.class);

        if (handled != null)
            return handled.value();

        //default handled first
        return true;
    }

    @Override
    public Method getMethod() {
        return _method;
    }
}
