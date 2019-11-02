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

import com.google.common.collect.Lists;
import org.apache.curator.RetryLoop;
import org.apache.curator.drivers.OperationTrace;
import org.apache.curator.framework.api.BackgroundCallback;
import org.apache.curator.framework.api.CuratorEventType;
import org.apache.curator.framework.api.ErrorListenerPathable;
import org.apache.curator.framework.api.GetEphemeralsBuilder;
import org.apache.curator.framework.api.Pathable;
import org.apache.curator.framework.api.UnhandledErrorListener;
import org.apache.zookeeper.AsyncCallback;
import java.util.List;
import java.util.concurrent.Executor;

public class GetEphemeralsBuilderImpl implements GetEphemeralsBuilder, BackgroundOperation<String>, ErrorListenerPathable<List<String>>
{
    private final CuratorFrameworkImpl client;
    private Backgrounding backgrounding;

    GetEphemeralsBuilderImpl(CuratorFrameworkImpl client)
    {
        this.client = client;
        backgrounding = new Backgrounding();
    }

    public GetEphemeralsBuilderImpl(CuratorFrameworkImpl client, Backgrounding backgrounding)
    {
        this.client = client;
        this.backgrounding = backgrounding;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground(BackgroundCallback callback, Object context)
    {
        backgrounding = new Backgrounding(callback, context);
        return this;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground(BackgroundCallback callback, Object context, Executor executor)
    {
        backgrounding = new Backgrounding(client, callback, context, executor);
        return this;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground(BackgroundCallback callback)
    {
        backgrounding = new Backgrounding(callback);
        return this;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground(BackgroundCallback callback, Executor executor)
    {
        backgrounding = new Backgrounding(client, callback, executor);
        return this;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground()
    {
        backgrounding = new Backgrounding(true);
        return this;
    }

    @Override
    public ErrorListenerPathable<List<String>> inBackground(Object context)
    {
        backgrounding = new Backgrounding(context);
        return this;
    }

    @Override
    public Pathable<List<String>> withUnhandledErrorListener(UnhandledErrorListener listener)
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
            AsyncCallback.EphemeralsCallback callback = (rc, ctx, paths) -> {
                trace.setReturnCode(rc).commit();
                if ( paths == null )
                {
                    paths = Lists.newArrayList();
                }
                CuratorEventImpl event = new CuratorEventImpl(client, CuratorEventType.EPHEMERALS, rc, operationAndData.getData(), null, ctx, null, null, paths, null, null, null);
                client.processBackgroundOperation(operationAndData, event);
            };
            client.getZooKeeper().getEphemerals(operationAndData.getData(), callback, backgrounding.getContext());
        }
        catch ( Throwable e )
        {
            backgrounding.checkError(e, null);
        }
    }

    @Override
    public List<String> forPath(String path) throws Exception
    {
        path = client.fixForNamespace(path);

        List<String> paths = null;
        if ( backgrounding.inBackground() )
        {
            client.processBackgroundOperation(new OperationAndData<>(this, path, backgrounding.getCallback(), null, backgrounding.getContext(), null), null);
        }
        else
        {
            paths = pathInForeground(path);
        }
        return paths;
    }

    private List<String> pathInForeground(final String path) throws Exception
    {
        OperationTrace trace = client.getZookeeperClient().startAdvancedTracer("GetEphemeralsBuilderImpl-Foreground");
        List<String> children = RetryLoop.callWithRetry(client.getZookeeperClient(), () -> client.getZooKeeper().getEphemerals(path));
        trace.setPath(path).commit();
        return children;
    }
}
