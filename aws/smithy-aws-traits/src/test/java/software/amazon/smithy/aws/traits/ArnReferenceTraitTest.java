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

package software.amazon.smithy.aws.traits;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import software.amazon.smithy.model.Model;
import software.amazon.smithy.model.node.Node;
import software.amazon.smithy.model.shapes.Shape;
import software.amazon.smithy.model.shapes.ShapeId;
import software.amazon.smithy.model.traits.Trait;
import software.amazon.smithy.model.traits.TraitFactory;

public class ArnReferenceTraitTest {

    @Test
    public void loadsEmptyTrait() {
        Node node = Node.parse("{}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnReferenceTrait.ID, ShapeId.from("ns.foo#foo"), node);

        assertTrue(trait.isPresent());
        assertThat(trait.get(), instanceOf(ArnReferenceTrait.class));
        ArnReferenceTrait arn = (ArnReferenceTrait) trait.get();
        assertThat(arn.toBuilder().build(), equalTo(arn));
        assertThat(arn.getService(), equalTo(Optional.empty()));
        assertThat(arn.getResource(), equalTo(Optional.empty()));
        assertThat(arn.getType(), equalTo(Optional.empty()));
    }

    @Test
    public void loadsTraitWithOptionalValues() {
        Node node = Node.parse("{\"resource\": \"com.foo#Baz\", \"service\": \"com.foo#Bar\", \"type\": \"AWS::Foo::Baz\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnReferenceTrait.ID, ShapeId.from("ns.foo#foo"), node);

        ArnReferenceTrait arn = (ArnReferenceTrait) trait.get();
        assertThat(arn.toBuilder().build(), equalTo(arn));
        assertThat(arn.getService(), equalTo(Optional.of(ShapeId.from("com.foo#Bar"))));
        assertThat(arn.getResource(), equalTo(Optional.of(ShapeId.from("com.foo#Baz"))));
        assertThat(arn.getType(), equalTo(Optional.of("AWS::Foo::Baz")));
    }

    @Test
    public void loadsTraitWithOptionalValuesAndRelativeShapeIds() {
        Node node = Node.parse("{\"resource\": \"Baz\", \"service\": \"Bar\", \"type\": \"AWS::Foo::Baz\"}");
        TraitFactory provider = TraitFactory.createServiceFactory();
        Optional<Trait> trait = provider.createTrait(ArnReferenceTrait.ID, ShapeId.from("ns.foo#String"), node);

        ArnReferenceTrait arn = (ArnReferenceTrait) trait.get();
        assertThat(arn.toBuilder().build(), equalTo(arn));
        assertThat(arn.getService(), equalTo(Optional.of(ShapeId.from("ns.foo#Bar"))));
        assertThat(arn.getResource(), equalTo(Optional.of(ShapeId.from("ns.foo#Baz"))));
        assertThat(arn.getType(), equalTo(Optional.of("AWS::Foo::Baz")));
    }

    @Test
    public void loadsFromModel() {
        Model result = Model.assembler()
                .discoverModels(getClass().getClassLoader())
                .addImport(getClass().getResource("test-model.json"))
                .assemble()
                .unwrap();
        Shape service = result.getShapeIndex()
                .getShape(ShapeId.from("ns.foo#AbsoluteResourceArn")).get();
        ArnReferenceTrait trait = service.getTrait(ArnReferenceTrait.class).get();

        assertThat(trait.getType(), equalTo(Optional.of("AWS::SomeService::AbsoluteResource")));
        assertThat(trait.getResource(), equalTo(Optional.of(ShapeId.from("ns.foo#AbsoluteResource"))));
        assertThat(trait.getService(), equalTo(Optional.of(ShapeId.from("ns.foo#SomeService"))));
    }
}
