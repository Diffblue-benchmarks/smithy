/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.smithy.model.knowledge;

import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.neighbor.NeighborProvider;

/**
 * Provides a cache of precomputed neighbors for models.
 */
public final class NeighborProviderIndex implements KnowledgeIndex {
    private final NeighborProvider provider;
    private volatile NeighborProvider reversed;

    public NeighborProviderIndex(Model model) {
        provider = NeighborProvider.precomputed(model.getShapeIndex());
        reversed = NeighborProvider.reverse(model.getShapeIndex(), provider);
    }

    /**
     * Gets the precomputed neighbor provider.
     *
     * @return Returns the provider.
     */
    public NeighborProvider getProvider() {
        return provider;
    }

    /**
     * Gets a reversed, bottom up neighbor provider.
     *
     * @return Returns the reversed neighbor provider.
     */
    public NeighborProvider getReverseProvider() {
        return reversed;
    }
}
