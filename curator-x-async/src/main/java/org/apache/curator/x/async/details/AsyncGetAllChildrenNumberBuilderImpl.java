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
package org.apache.curator.x.async.details;

import org.apache.curator.framework.imps.CuratorFrameworkImpl;
import org.apache.curator.framework.imps.GetAllChildrenNumberBuilderImpl;
import org.apache.curator.x.async.AsyncStage;
import org.apache.curator.x.async.api.AsyncPathable;

import static org.apache.curator.x.async.details.BackgroundProcs.numberProc;
import static org.apache.curator.x.async.details.BackgroundProcs.safeCall;

class AsyncGetAllChildrenNumberBuilderImpl implements AsyncPathable<AsyncStage<Integer>>
{
    private final CuratorFrameworkImpl client;
    private final Filters filters;

    AsyncGetAllChildrenNumberBuilderImpl(CuratorFrameworkImpl client, Filters filters)
    {
        this.client = client;
        this.filters = filters;
    }

    @Override
    public AsyncStage<Integer> forPath(String path)
    {
        BuilderCommon<Integer> common = new BuilderCommon<>(filters, null, numberProc);
        GetAllChildrenNumberBuilderImpl builder = new GetAllChildrenNumberBuilderImpl(client, common.backgrounding);
        return safeCall(common.internalCallback, () -> builder.forPath(path));
    }
}
