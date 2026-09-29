package com.example.horsegenetics.common.parts;

/**
 * Where on the horse a part is rooted. Deliberately an enum and not a bone name:
 * the client owns the mapping to a ModelPart chain and an offset, so retargeting an
 * anchor is a client edit and common/ never learns Minecraft's part names (hard rule 1).
 */
public enum PartAnchor { FOREHEAD, CROWN, SPINE }
