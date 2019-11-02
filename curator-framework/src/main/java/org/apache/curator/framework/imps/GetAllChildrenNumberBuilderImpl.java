/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.curator.framework.imps;

import org.apache.curator.RetryLoop;
import org.apache.curator.drivers.OperationTrace;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.ErrorListenerPathableInt;
import org.apache.curator.framework.api.GetAllChildrenNumberBuilder;
import org.apache.curator.framework.api.PathableInt;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.zookeeper.AsyncCallback;
import java.util.concurrent.Executor;

public class GetAllChildrenNumberBuilderImpl implements GetAllChildrenNumberBuilder, BackgroundOperation<String>, ErrorListenerPathableInt
{
    private final CuratorFrameworkImpl client;
    private Backgrounding backgrounding;

    GetAllChildrenNumberBuilderImpl(CuratorFrameworkImpl client)
    {
        this.client = client;
        backgrounding = new Backgrounding();
    }

    public GetAllChildrenNumberBuilderImpl(CuratorFrameworkImpl client, Backgrounding backgrounding)
    {
        this.client = client;
        this.backgrounding = backgrounding;
    }

    @Override
    public ErrorListenerPathableInt inBackground(BackgroundCallback callback, Object context)
    {
        backgrounding = new Backgrounding(callback, context);
        return this;
    }

    @Override
    public ErrorListenerPathableInt inBackground(BackgroundCallback callback, Object context, Executor executor)
    {
        backgrounding = new Backgrounding(client, callback, context, executor);
        return this;
    }

    @Override
    public ErrorListenerPathableInt inBackground(BackgroundCallback callback)
    {
        backgrounding = new Backgrounding(callback);
        return this;
    }

    @Override
    public ErrorListenerPathableInt inBackground(BackgroundCallback callback, Executor executor)
    {
        backgrounding = new Backgrounding(client, callback, executor);
        return this;
    }

    @Override
    public ErrorListenerPathableInt inBackground()
    {
        backgrounding = new Backgrounding(true);
        return this;
    }

    @Override
    public ErrorListenerPathableInt inBackground(Object context)
    {
        backgrounding = new Backgrounding(context);
        return this;
    }

    @Override
    public PathableInt withUnhandledErrorListener(UnhandledErrorListener listener)
    {
        backgrounding = new Backgrounding(backgrounding, listener);
        return this;
    }

    @Override
    public void performBackgroundOperation(final OperationAndData<String> operationAndData) throws Exception
    {
        try
        {
            final OperationTrace trace = client.getZookeeperClient().startAdvancedTracer("GetEphemeralsBuilderImpl-Background");
            AsyncCallback.AllChildrenNumberCallback callback = (rc, path, ctx, number) -> {
                trace.setReturnCode(rc).setPath(path).commit();
                CuratorEventImpl event = new CuratorEventImpl(client, CuratorEventType.ALL_CHILDREN_NUMBER, rc, path, null, ctx, null, null, null, null, null, null, number);
                client.processBackgroundOperation(operationAndData, event);
            };
            client.getZooKeeper().getAllChildrenNumber(operationAndData.getData(), callback, backgrounding.getContext());
        }
        catch ( Throwable e )
        {
            backgrounding.checkError(e, null);
        }
    }

    @Override
    public int forPath(String path) throws Exception
    {
        path = client.fixForNamespace(path);

        int number;
        if ( backgrounding.inBackground() )
        {
            client.processBackgroundOperation(new OperationAndData<>(this, path, backgrounding.getCallback(), null, backgrounding.getContext(), null), null);
            number = 0;
        }
        else
        {
            number = pathInForeground(path);
        }
        return number;
    }

    private int pathInForeground(final String path) throws Exception
    {
        OperationTrace trace = client.getZookeeperClient().startAdvancedTracer("GetEphemeralsBuilderImpl-Foreground");
        Integer number = RetryLoop.callWithRetry(client.getZookeeperClient(), () -> client.getZooKeeper().getAllChildrenNumber(path));
        trace.setPath(path).commit();
        return number;
    }
}
