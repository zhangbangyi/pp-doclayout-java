package com.example.doclayout.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import com.example.doclayout.core.RegionAwareDataOptimizer;
import com.fasterxml.jackson.databind.ObjectMapper;

class SpatialRelationGraphTest {
    @Test
    void buildsDirectionalAndAdjacentRelations() {
        LayoutBox left = box(0, 0, 10, 10);
        LayoutBox right = box(12, 0, 22, 10);

        SpatialRelationGraph graph = SpatialRelationGraph.fromBoxes(List.of(left, right),
                new SpatialRelationConfig(0.1f, 0.2f, 0.3f));

        assertThat(graph.relations()).extracting(RegionRelation::type)
                .contains(SpatialRelation.LEFT_OF, SpatialRelation.RIGHT_OF, SpatialRelation.ADJACENT_TO);
    }

    @Test
    void buildsContainmentAndOverlapWithoutInventingDirection() {
        LayoutBox outer = box(0, 0, 100, 100);
        LayoutBox inner = box(20, 20, 30, 30);
        SpatialRelationGraph contained = SpatialRelationGraph.fromBoxes(List.of(outer, inner));
        assertThat(contained.relations()).extracting(RegionRelation::type)
                .containsExactly(SpatialRelation.CONTAINS, SpatialRelation.INSIDE);

        LayoutBox overlap = box(25, 25, 120, 120);
        SpatialRelationGraph overlapped = SpatialRelationGraph.fromBoxes(List.of(outer, overlap));
        assertThat(overlapped.relations()).extracting(RegionRelation::type)
                .containsExactly(SpatialRelation.OVERLAPS, SpatialRelation.OVERLAPS);
    }

    @Test
    void emptyAndInvalidBoxesAreSafe() {
        LayoutBox invalid = box(2, 2, 2, 8);
        SpatialRelationGraph graph = SpatialRelationGraph.fromBoxes(List.of(invalid));
        assertThat(graph.nodeCount()).isEqualTo(1);
        assertThat(graph.relations()).isEmpty();
        assertThat(SpatialRelationGraph.fromBoxes(null).relations()).isEmpty();
    }

    @Test
    void optimizerKeepsRegionIndexesAndOrdersBoxes() {
        LayoutBox later = box(0, 20, 10, 30).withOrder(2);
        LayoutBox first = box(0, 0, 10, 10).withOrder(1);
        RegionAwareDataOptimizer.OptimizationResult result =
                new RegionAwareDataOptimizer().optimize(Arrays.asList(later, null, first));

        assertThat(result.readingOrder()).containsExactly(2, 0);
        assertThat(result.relations().nodeCount()).isEqualTo(3);
    }

    @Test
    void graphCanBeSerializedByJackson() throws Exception {
        SpatialRelationGraph graph = SpatialRelationGraph.fromBoxes(
                List.of(box(0, 0, 10, 10), box(12, 0, 22, 10)));

        String json = new ObjectMapper().writeValueAsString(graph);

        assertThat(json).contains("\"nodeCount\":2").contains("\"relations\"")
                .contains("\"fromIndex\":0").contains("\"type\":\"LEFT_OF\"");
    }

    private static LayoutBox box(float x1, float y1, float x2, float y2) {
        return new LayoutBox(1, "text", 0.9f, x1, y1, x2, y2, 0, 1, "", null);
    }
}
