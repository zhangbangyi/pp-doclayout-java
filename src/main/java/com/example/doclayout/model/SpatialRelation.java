package com.example.doclayout.model;

/**
 * 两个版面区域之间的空间关系。关系是有向的，例如 A LEFT_OF B 时，B
 * 同时会有一条 RIGHT_OF A 的反向关系。
 */
public enum SpatialRelation {
    LEFT_OF,
    RIGHT_OF,
    ABOVE,
    BELOW,
    CONTAINS,
    INSIDE,
    OVERLAPS,
    ADJACENT_TO
}
