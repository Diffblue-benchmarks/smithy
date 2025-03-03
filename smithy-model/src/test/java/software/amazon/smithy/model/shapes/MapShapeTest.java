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

package software.amazon.smithy.model.shapes;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MapShapeTest {
    @Test
    public void returnsAppropriateType() {
        MapShape shape = MapShape.builder()
                .id("ns.foo#bar")
                .key(MemberShape.builder().id("ns.foo#bar$key").target("ns.foo#bam").build())
                .value(MemberShape.builder().id("ns.foo#bar$value").target("ns.foo#bam").build())
                .build();

        assertEquals(shape.getType(), ShapeType.MAP);
    }

    @Test
    public void mustNotContainMembersInShapeId() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            MapShape.builder()
                    .id("ns.foo#bar$baz")
                    .key(MemberShape.builder().id("ns.foo#bar$key").target("ns.foo#bam").build())
                    .value(MemberShape.builder().id("ns.foo#bar$value").target("ns.foo#bam").build())
                    .build();
        });
    }
}
