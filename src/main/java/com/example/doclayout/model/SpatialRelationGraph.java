package com.example.doclayout.model;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 区域感知框架生成的空间关系图。
 *
 * <p>图中的节点顺序与 {@link LayoutResult#boxes()} 完全一致，边是有向的；
 * 因此调用方可以直接用区域下标回查类别、坐标和 OCR 内容。</p>
 */
public final class SpatialRelationGraph {
    private static final float EPSILON = 1.0e-4f;
    private final int nodeCount;
    private final List<RegionRelation> relations;

    private SpatialRelationGraph(int nodeCount, List<RegionRelation> relations) {
        this.nodeCount = nodeCount;
        this.relations = List.copyOf(relations);
    }

    public static SpatialRelationGraph empty(int nodeCount) {
        if (nodeCount < 0) throw new IllegalArgumentException("节点数量不能为负数");
        return new SpatialRelationGraph(nodeCount, List.of());
    }

    public static SpatialRelationGraph fromBoxes(List<LayoutBox> boxes) {
        return fromBoxes(boxes, SpatialRelationConfig.defaults());
    }

    /** 根据区域矩形构建关系图，输入顺序会被保留为节点下标。 */
    public static SpatialRelationGraph fromBoxes(List<LayoutBox> boxes, SpatialRelationConfig config) {
        List<LayoutBox> safeBoxes = boxes == null ? List.of() : boxes;
        SpatialRelationConfig safeConfig = config == null ? SpatialRelationConfig.defaults() : config;
        List<RegionRelation> edges = new ArrayList<>();
        for (int i = 0; i < safeBoxes.size(); i++) {
            LayoutBox a = safeBoxes.get(i);
            if (a == null || !valid(a)) continue;
            for (int j = i + 1; j < safeBoxes.size(); j++) {
                LayoutBox b = safeBoxes.get(j);
                if (b == null || !valid(b)) continue;
                addRelations(edges, i, j, a, b, safeConfig);
            }
        }
        edges.sort(Comparator.comparingInt(RegionRelation::fromIndex)
                .thenComparingInt(RegionRelation::toIndex)
                .thenComparing(RegionRelation::type));
        return new SpatialRelationGraph(safeBoxes.size(), edges);
    }

    public int nodeCount() {
        return nodeCount;
    }

    /** Jackson/JavaBean 兼容访问器，保持领域侧 nodeCount() API 不变。 */
    public int getNodeCount() {
        return nodeCount;
    }

    public List<RegionRelation> relations() {
        return relations;
    }

    /** Jackson/JavaBean 兼容访问器，保持领域侧 relations() API 不变。 */
    public List<RegionRelation> getRelations() {
        return relations;
    }

    public List<RegionRelation> outgoing(int fromIndex) {
        checkIndex(fromIndex);
        return relations.stream().filter(r -> r.fromIndex() == fromIndex).toList();
    }

    public List<RegionRelation> incoming(int toIndex) {
        checkIndex(toIndex);
        return relations.stream().filter(r -> r.toIndex() == toIndex).toList();
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= nodeCount) throw new IndexOutOfBoundsException("区域下标: " + index);
    }

    private static void addRelations(List<RegionRelation> edges, int i, int j,
                                     LayoutBox a, LayoutBox b, SpatialRelationConfig config) {
        Rect left = Rect.of(a);
        Rect right = Rect.of(b);
        float intersection = left.intersectionArea(right);
        float minArea = Math.min(left.area(), right.area());
        float iou = intersection / Math.max(EPSILON, left.area() + right.area() - intersection);

        if (left.contains(right)) {
            edges.add(edge(i, j, SpatialRelation.CONTAINS, 1f, 0f));
            edges.add(edge(j, i, SpatialRelation.INSIDE, 1f, 0f));
            return;
        }
        if (right.contains(left)) {
            edges.add(edge(j, i, SpatialRelation.CONTAINS, 1f, 0f));
            edges.add(edge(i, j, SpatialRelation.INSIDE, 1f, 0f));
            return;
        }

        float horizontalOverlap = left.overlapWidth(right) / Math.max(EPSILON, Math.min(left.width(), right.width()));
        float verticalOverlap = left.overlapHeight(right) / Math.max(EPSILON, Math.min(left.height(), right.height()));
        float horizontalGap = left.horizontalGap(right);
        float verticalGap = left.verticalGap(right);
        float distance = (float) Math.hypot(horizontalGap, verticalGap);
        boolean overlaps = intersection > EPSILON &&
                (iou >= config.overlapIouThreshold() || intersection / Math.max(EPSILON, minArea) >= 0.20f);
        if (overlaps) {
            float score = clamp(iou);
            edges.add(edge(i, j, SpatialRelation.OVERLAPS, score, 0f));
            edges.add(edge(j, i, SpatialRelation.OVERLAPS, score, 0f));
            return;
        }

        SpatialRelation direction;
        if (horizontalGap > EPSILON && verticalOverlap >= config.minAxisOverlapRatio()) {
            direction = left.centerX() < right.centerX() ? SpatialRelation.LEFT_OF : SpatialRelation.RIGHT_OF;
        } else if (verticalGap > EPSILON && horizontalOverlap >= config.minAxisOverlapRatio()) {
            direction = left.centerY() < right.centerY() ? SpatialRelation.ABOVE : SpatialRelation.BELOW;
        } else {
            direction = dominantDirection(left, right);
        }
        float directionScore = alignmentScore(left, right, direction, distance);
        edges.add(edge(i, j, direction, directionScore, distance));
        edges.add(edge(j, i, opposite(direction), directionScore, distance));

        float maxDimension = Math.max(Math.max(left.width(), left.height()), Math.max(right.width(), right.height()));
        if (distance <= Math.max(1f, maxDimension * config.adjacencyGapRatio())) {
            float adjacencyScore = clamp(1f - distance / Math.max(1f, maxDimension * Math.max(config.adjacencyGapRatio(), EPSILON)));
            edges.add(edge(i, j, SpatialRelation.ADJACENT_TO, adjacencyScore, distance));
            edges.add(edge(j, i, SpatialRelation.ADJACENT_TO, adjacencyScore, distance));
        }
    }

    private static RegionRelation edge(int from, int to, SpatialRelation type, float score, float distance) {
        return new RegionRelation(from, to, type, clamp(score), Math.max(0f, distance));
    }

    private static SpatialRelation dominantDirection(Rect a, Rect b) {
        float dx = b.centerX() - a.centerX();
        float dy = b.centerY() - a.centerY();
        if (Math.abs(dx) >= Math.abs(dy)) return dx >= 0 ? SpatialRelation.LEFT_OF : SpatialRelation.RIGHT_OF;
        return dy >= 0 ? SpatialRelation.ABOVE : SpatialRelation.BELOW;
    }

    private static float alignmentScore(Rect a, Rect b, SpatialRelation relation, float distance) {
        float axis = switch (relation) {
            case LEFT_OF, RIGHT_OF -> a.overlapHeight(b) / Math.max(EPSILON, Math.min(a.height(), b.height()));
            case ABOVE, BELOW -> a.overlapWidth(b) / Math.max(EPSILON, Math.min(a.width(), b.width()));
            default -> 0.5f;
        };
        return clamp(0.5f * axis + 0.5f / (1f + distance));
    }

    private static SpatialRelation opposite(SpatialRelation relation) {
        return switch (relation) {
            case LEFT_OF -> SpatialRelation.RIGHT_OF;
            case RIGHT_OF -> SpatialRelation.LEFT_OF;
            case ABOVE -> SpatialRelation.BELOW;
            case BELOW -> SpatialRelation.ABOVE;
            default -> relation;
        };
    }

    private static boolean valid(LayoutBox b) {
        return Float.isFinite(b.x1()) && Float.isFinite(b.y1()) && Float.isFinite(b.x2()) && Float.isFinite(b.y2())
                && Math.max(b.x1(), b.x2()) - Math.min(b.x1(), b.x2()) > EPSILON
                && Math.max(b.y1(), b.y2()) - Math.min(b.y1(), b.y2()) > EPSILON;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private record Rect(float minX, float minY, float maxX, float maxY) {
        static Rect of(LayoutBox b) {
            return new Rect(Math.min(b.x1(), b.x2()), Math.min(b.y1(), b.y2()),
                    Math.max(b.x1(), b.x2()), Math.max(b.y1(), b.y2()));
        }
        float width() { return maxX - minX; }
        float height() { return maxY - minY; }
        float area() { return width() * height(); }
        float centerX() { return (minX + maxX) / 2f; }
        float centerY() { return (minY + maxY) / 2f; }
        float overlapWidth(Rect o) { return Math.max(0f, Math.min(maxX, o.maxX) - Math.max(minX, o.minX)); }
        float overlapHeight(Rect o) { return Math.max(0f, Math.min(maxY, o.maxY) - Math.max(minY, o.minY)); }
        float intersectionArea(Rect o) { return overlapWidth(o) * overlapHeight(o); }
        float horizontalGap(Rect o) { return Math.max(0f, Math.max(minX, o.minX) - Math.min(maxX, o.maxX)); }
        float verticalGap(Rect o) { return Math.max(0f, Math.max(minY, o.minY) - Math.min(maxY, o.maxY)); }
        boolean contains(Rect o) { return minX <= o.minX + EPSILON && minY <= o.minY + EPSILON && maxX >= o.maxX - EPSILON && maxY >= o.maxY - EPSILON && area() > o.area() + EPSILON; }
    }
}
